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
        .flatMapCompletable(encryptedGoal -> {
          DatabaseReference rootRef = mDb.getReference(String.format("goals/%s/active", mUser.getUid()));
          boolean existingEntry = goal.id != null;
          DatabaseReference entryRef = existingEntry ? rootRef.child(goal.id) : rootRef.push();
          return RxFirebaseDatabase.setValue(entryRef, encryptedGoal);
        });
  }

  public Observable<Goal> getGoals() {
    return mKeyProvider.getGoalKey()
        .flatMapObservable(secretKey -> RxFirebaseDatabase
            .observeSingleValueEvent(mDb.getReference(String.format("goals/%s/active", mUser.getUid())))
            .flatMapObservable(rootSnapshot -> Observable.fromIterable(rootSnapshot.getChildren()))
            .flatMapSingle(snapshot -> {
              final String id = snapshot.getKey();
              String encryptedGoalModel = snapshot.getValue(String.class);
              return mCryptoUtil.decrypt(encryptedGoalModel, secretKey, Goal.class)
                  .map(goal -> {
                    goal.id = id;
                    goal.status = Goal.Status.ACTIVE;
                    return goal;
                  });
            }));
  }
}
