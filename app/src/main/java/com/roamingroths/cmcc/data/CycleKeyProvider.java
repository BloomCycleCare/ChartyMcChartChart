package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import durdinapps.rxfirebase2.DataSnapshotMapper;
import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 9/2/17.
 */

public class CycleKeyProvider {

  enum KeyAlias {
    CHART, WELLNESS, SYMPTOM
  }

  private static boolean DEBUG = false;
  private static String TAG = CycleKeyProvider.class.getSimpleName();

  private final FirebaseDatabase db;
  private final CryptoUtil cryptoUtil;

  public static CycleKeyProvider forDb(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    return new CycleKeyProvider(db, cryptoUtil);
  }

  private CycleKeyProvider(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    this.db = db;
    this.cryptoUtil = cryptoUtil;
  }

  private Maybe<KeyWithAlias> getKey(String cycleId, String userId, final KeyAlias alias) {
    if (DEBUG) Log.v(TAG, "Loading key alias: " + alias.name());
    return RxFirebaseDatabase.observeSingleValueEvent(
        db.getReference("keys").child(cycleId).child(userId).child(alias.name().toLowerCase()),
        DataSnapshotMapper.of(String.class))
        .flatMap(new Function<String, MaybeSource<KeyWithAlias>>() {
          @Override
          public MaybeSource<KeyWithAlias> apply(@NonNull String encryptedKey) throws Exception {
            return cryptoUtil.decryptKey(encryptedKey).map(KeyWithAlias.create(alias));
          }
        });
  }

  public Maybe<Cycle.Keys> getChartKeys(String cycleId, String userId) {
    if (DEBUG) Log.v(TAG, "Getting keys for cycle: " + cycleId + ", user: " + userId);
    Set<Maybe<KeyWithAlias>> keys = new HashSet<>();
    for (KeyAlias alias : KeyAlias.values()) {
      keys.add(getKey(cycleId, userId, alias));
    }
    return Maybe.zip(keys, new Function<Object[], Cycle.Keys>() {
      @Override
      public Cycle.Keys apply(@NonNull Object[] objects) throws Exception {
        Map<KeyAlias, SecretKey> keyMap = new HashMap<>();
        for (Object object : objects) {
          KeyWithAlias keyAndAlias = (KeyWithAlias) object;
          keyMap.put(keyAndAlias.alias, keyAndAlias.key);
        }
        return new Cycle.Keys(keyMap.get(KeyAlias.CHART), keyMap.get(KeyAlias.WELLNESS), keyMap.get(KeyAlias.SYMPTOM));
      }
    });
  }

  private Completable putChartKeyRx(SecretKey key, final KeyAlias alias, final String cycleId, final String userId) {
    return cryptoUtil.encryptKey(key)
        .flatMapCompletable(new Function<String, CompletableSource>() {
          @Override
          public CompletableSource apply(@NonNull String encryptedKey) throws Exception {
            return RxFirebaseDatabase.setValue(
                db.getReference("keys").child(cycleId).child(userId).child(alias.name().toLowerCase()),
                encryptedKey);
          }
        });
  }

  public Completable dropKeys(String cycleId) {
    return RxFirebaseDatabase.removeValue(db.getReference("keys").child(cycleId));
  }

  public Completable putChartKeysRx(Cycle.Keys keys, String cycleId, String userId) {
    Set<Completable> ops = new HashSet<>();
    ops.add(putChartKeyRx(keys.chartKey, KeyAlias.CHART, cycleId, userId));
    ops.add(putChartKeyRx(keys.wellnessKey, KeyAlias.WELLNESS, cycleId, userId));
    ops.add(putChartKeyRx(keys.symptomKey, KeyAlias.SYMPTOM, cycleId, userId));
    return Completable.merge(ops);
  }

  private static class KeyWithAlias {
    public final SecretKey key;
    public final KeyAlias alias;

    private KeyWithAlias(SecretKey key, KeyAlias alias) {
      this.key = key;
      this.alias = alias;
    }

    static Function<SecretKey, KeyWithAlias> create(final KeyAlias alias) {
      return new Function<SecretKey, KeyWithAlias>() {
        @Override
        public KeyWithAlias apply(@NonNull SecretKey secretKey) throws Exception {
          return new KeyWithAlias(secretKey, alias);
        }
      };
    }
  }

  private static final com.google.common.base.Function<String, Object> TO_OBJECT = new com.google.common.base.Function<String, Object>() {
    @Override
    public Object apply(String input) {
      return input;
    }
  };
}
