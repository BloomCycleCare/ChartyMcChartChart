package com.roamingroths.cmcc.providers;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.goals.Goal;
import com.roamingroths.cmcc.logic.goals.GoalModel;

import javax.crypto.SecretKey;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 2/28/18.
 */

public class GoalProvider {

  private final FirebaseDatabase mDb;
  private final FirebaseUser mUser;
  private final CryptoUtil mCryptoUtil;
  private final KeyProvider mKeyProvider;

  public GoalProvider(FirebaseDatabase mDb, FirebaseUser mUser, CryptoUtil mCryptoUtil, KeyProvider mKeyProvider) {
    this.mDb = mDb;
    this.mUser = mUser;
    this.mCryptoUtil = mCryptoUtil;
    this.mKeyProvider = mKeyProvider;
  }

  public Completable putGoal(final Goal goal) {
    return mCryptoUtil.encrypt(goal)
        .flatMapCompletable(new Function<String, CompletableSource>() {
          @Override
          public CompletableSource apply(String encryptedGoal) throws Exception {
            DatabaseReference rootRef = mDb.getReference(String.format("goals/%s/active", mUser.getUid()));
            boolean existingEntry = goal.id != null;
            DatabaseReference entryRef = existingEntry ? rootRef.child(goal.id) : rootRef.push();
            return RxFirebaseDatabase.setValue(entryRef, encryptedGoal);
          }
        });
  }

  public Observable<Goal> getGoals() {
    return mKeyProvider.getGoalKey()
        .flatMapObservable(new Function<SecretKey, ObservableSource<? extends Goal>>() {
          @Override
          public ObservableSource<? extends Goal> apply(final SecretKey secretKey) throws Exception {
            return RxFirebaseDatabase
                .observeSingleValueEvent(mDb.getReference(String.format("goals/%s/active", mUser.getUid())))
                .flatMapObservable(new Function<DataSnapshot, ObservableSource<DataSnapshot>>() {
                  @Override
                  public ObservableSource<DataSnapshot> apply(DataSnapshot rootSnapshot) throws Exception {
                    return Observable.fromIterable(rootSnapshot.getChildren());
                  }
                })
                .flatMapSingle(goalFromDbEntry(secretKey, Goal.Status.ACTIVE));
          }
        });
  }

  private Function<DataSnapshot, SingleSource<Goal>> goalFromDbEntry(final SecretKey key, final Goal.Status status) {
    return new Function<DataSnapshot, SingleSource<Goal>>() {
      @Override
      public SingleSource<Goal> apply(DataSnapshot aSnapshot) throws Exception {
        final String id = aSnapshot.getKey();
        String encryptedGoalModel = aSnapshot.getValue(String.class);
        return mCryptoUtil.decrypt(encryptedGoalModel, key, Goal.class)
            .map(new Function<Goal, Goal>() {
              @Override
              public Goal apply(Goal goal) throws Exception {
                goal.id = id;
                goal.status = status;
                return goal;
              }
            });
      }
    };
  }
}
