package com.roamingroths.cmcc.providers;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.utils.UpdateHandle;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import durdinapps.rxfirebase2.DataSnapshotMapper;
import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 1/13/18.
 */

public class KeyProvider {

  enum KeyAlias {
    OBSERVATION, WELLNESS, SYMPTOM
  }

  private static boolean DEBUG = true;
  private static String TAG = KeyProvider.class.getSimpleName();

  private final CryptoUtil mCryptoUtil;
  private final FirebaseDatabase mDb;
  private final FirebaseUser mCurrentUser;

  public KeyProvider(CryptoUtil mCryptoUtil, FirebaseDatabase mDb, FirebaseUser mCurrentUser) {
    this.mCryptoUtil = mCryptoUtil;
    this.mDb = mDb;
    this.mCurrentUser = mCurrentUser;
  }

  public Maybe<Cycle.Keys> getCycleKeys(String cycleId) {
    if (DEBUG) Log.v(TAG, "Getting keys for cycle: " + cycleId);
    Maybe<SecretKey> observationKey = getCycleKey(cycleId, KeyAlias.OBSERVATION);
    Maybe<SecretKey> wellnessKey = getCycleKey(cycleId, KeyAlias.WELLNESS);
    Maybe<SecretKey> symptomKey = getCycleKey(cycleId, KeyAlias.SYMPTOM);
    return Maybe.zip(observationKey, wellnessKey, symptomKey, Cycle.Keys::new);
  }

  private Maybe<SecretKey> getCycleKey(String cycleId, final KeyAlias alias) {
    if (DEBUG) Log.v(TAG, String.format("Loading key alias: %s for cycle: %s", alias.name(), cycleId));
    return RxFirebaseDatabase.observeSingleValueEvent(
        mDb.getReference("keys").child(mCurrentUser.getUid()).child(cycleId).child(alias.name().toLowerCase()),
        DataSnapshotMapper.of(String.class))
        .flatMap(new Function<String, MaybeSource<SecretKey>>() {
          @Override
          public MaybeSource<SecretKey> apply(@NonNull String encryptedKey) throws Exception {
            return mCryptoUtil.decryptKey(encryptedKey);
          }
        });
  }

  // TODO(sharing): add support for multiple users
  public Single<UpdateHandle> putCycleKeys(Cycle cycle) {
    List<Single<UpdateHandle>> handles = new ArrayList<>();
    handles.add(putCycleKey(cycle.keys.chartKey, KeyAlias.OBSERVATION, cycle));
    handles.add(putCycleKey(cycle.keys.wellnessKey, KeyAlias.WELLNESS, cycle));
    handles.add(putCycleKey(cycle.keys.symptomKey, KeyAlias.SYMPTOM, cycle));
    return Single.concat(handles).collectInto(UpdateHandle.forDb(mDb), UpdateHandle.collector());
  }

  private Single<UpdateHandle> putCycleKey(SecretKey key, final KeyAlias alias, final Cycle cycle) {
    return mCryptoUtil.encryptKey(key).map(new Function<String, UpdateHandle>() {
      @Override
      public UpdateHandle apply(String s) throws Exception {
        UpdateHandle handle = UpdateHandle.forDb(mDb);
        handle.updates.put(String.format("/keys/%s/%s/%s", mCurrentUser.getUid(), cycle.id, alias.name().toLowerCase()), s);
        return handle;
      }
    }).toSingle();
  }

  // TODO(sharing): add support for multiple users
  public UpdateHandle dropCycleKeys(Cycle cycle) {
    UpdateHandle handle = UpdateHandle.forDb(mDb);
    handle.updates.put(String.format("/keys/%s/%s", mCurrentUser.getUid(), cycle.id), null);
    return handle;
  }

  public Single<SecretKey> createAndStoreProfileKey() {
    return createAndStoreKeyInternal(profileKeyReference());
  }

  public Single<SecretKey> createAndStoreGoalKey() {
    return createAndStoreKeyInternal(goalKeyReference());
  }

  public Single<SecretKey> createAndStoreKeyInternal(final DatabaseReference ref) {
    try {
      SecretKey key = AesCryptoUtil.createKey();
      return mCryptoUtil.encryptKey(key).flatMapCompletable(new Function<String, CompletableSource>() {
        @Override
        public CompletableSource apply(String encryptedKey) throws Exception {
          return RxFirebaseDatabase.setValue(ref, encryptedKey);
        }
      }).andThen(Single.just(key));
    } catch (NoSuchAlgorithmException e) {
      return Single.error(e);
    }
  }

  private DatabaseReference goalKeyReference() {
    return mDb.getReference().child(
        String.format("keys/%s/%s/goals", mCurrentUser.getUid(), mCurrentUser.getUid()));
  }

  public Single<SecretKey> getGoalKey() {
    return getKeyInternal(goalKeyReference()).switchIfEmpty(createAndStoreGoalKey());
  }

  private DatabaseReference profileKeyReference() {
    return mDb.getReference().child(
        String.format("keys/%s/%s/profile", mCurrentUser.getUid(), mCurrentUser.getUid()));
  }

  public Maybe<SecretKey> getProfileKey() {
    return getKeyInternal(profileKeyReference());
  }

  public Maybe<SecretKey> getKeyInternal(DatabaseReference ref) {
    return RxFirebaseDatabase
        .observeSingleValueEvent(ref, String.class)
        .flatMap(new Function<String, MaybeSource<? extends SecretKey>>() {
          @Override
          public MaybeSource<? extends SecretKey> apply(String encryptedKey) throws Exception {
            return mCryptoUtil.decryptKey(encryptedKey);
          }
        });
  }
}
