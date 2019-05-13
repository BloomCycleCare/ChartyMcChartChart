package com.roamingroths.cmcc.providers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.logic.goals.Goal;
import com.roamingroths.cmcc.logic.goals.GoalFilterType;
import com.roamingroths.cmcc.logic.goals.GoalModel;

import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Created by parkeroth on 2/28/18.
 */

public class GoalProvider {

  private static int MAX_CACHE_SIZE = 100;

  private final FirebaseDatabase mDb;
  private final FirebaseUser mUser;
  private final Cache<String, Goal> mCache =
      CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build();

  public GoalProvider(FirebaseDatabase mDb, FirebaseUser mUser) {
    this.mDb = mDb;
    this.mUser = mUser;
  }

  public Completable init() {
    return getGoalsRemote().ignoreElements();
  }

  public Completable putGoalModel(final GoalModel model) {
    return Completable.complete();
    /*return mKeyProvider.getGoalKey()
        .map(key -> {
          Goal goal = new Goal(key, model);
          goal.id = getRef(GoalFilterType.ACTIVE).push().getKey();
          return goal;
        })
        .flatMapCompletable(goal -> mCryptoUtil.encrypt(goal).flatMapCompletable(encryptedGoal -> {
          DatabaseReference entryRef = getRef(GoalFilterType.ACTIVE).child(goal.id);
          return RxFirebaseDatabase
              .setValue(entryRef, encryptedGoal)
              .doOnComplete(() -> mCache.put(goal.id, goal));
        }));*/
  }

  public Observable<Goal> getGoals() {
    return Observable.fromIterable(mCache.asMap().values()).sorted();
  }

  public Observable<Goal> getGoalsRemote() {
    return Observable.empty();
    /*return mKeyProvider.getGoalKey()
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
            })
            .doOnEach(notification -> {
              if (notification.isOnNext()) {
                Goal goal = notification.getValue();
                mCache.put(goal.id, goal);
              }
            }));*/
  }

  private DatabaseReference getRef(GoalFilterType filterType) {
    return mDb.getReference(
        String.format("goals/%s/%s", mUser.getUid(), filterType.name().toLowerCase()));
  }
}
