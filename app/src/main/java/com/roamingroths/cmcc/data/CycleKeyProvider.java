package com.roamingroths.cmcc.data;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;

import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 9/2/17.
 */

public class CycleKeyProvider {

  enum KeyAlias {
    CHART, WELLNESS, SYMPTOM
  }

  private static boolean DEBUG = true;
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

  private static final com.google.common.base.Function<String, Object> TO_OBJECT = new com.google.common.base.Function<String, Object>() {
    @Override
    public Object apply(String input) {
      return input;
    }
  };
}
