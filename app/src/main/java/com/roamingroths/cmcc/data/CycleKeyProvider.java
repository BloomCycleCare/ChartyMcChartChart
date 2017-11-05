package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.crypto.RxCryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.Listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
  private final RxCryptoUtil cryptoUtil;

  public static CycleKeyProvider forDb(FirebaseDatabase db, RxCryptoUtil cryptoUtil) {
    return new CycleKeyProvider(db, cryptoUtil);
  }

  private CycleKeyProvider(FirebaseDatabase db, RxCryptoUtil cryptoUtil) {
    this.db = db;
    this.cryptoUtil = cryptoUtil;
  }

  private Maybe<KeyWithAlias> getKey(String cycleId, String userId, final KeyAlias alias) {
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

  public Instance forCycle(String cycleId) {
    return new Instance(cycleId);
  }

  public class Instance {

    private final DatabaseReference ref;
    private final String mCycleId;

    private Instance(String cycleId) {
      mCycleId = cycleId;
      ref = db.getReference("keys").child(cycleId);
    }

    public void getChartKeys(
        String userId, final Callback<Cycle.Keys> callback) {
      if (DEBUG) Log.v(TAG, "Fetching key for user " + userId + " cycle " + ref.getKey());
      final Map<String, SecretKey> decryptedKeys = new ConcurrentHashMap<>();
      final Callback<Map<String, SecretKey>> keyCallback = new Callbacks.ErrorForwardingCallback<Map<String, SecretKey>>(callback) {
        @Override
        public void acceptData(Map<String, SecretKey> decryptedKeys) {
          if (DEBUG) Log.v(TAG, "Done decrypting keys for cycle");
        }
      };
      ref.child(userId).addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
          long numKeys = dataSnapshot.getChildrenCount();
          if (DEBUG) Log.v(TAG, "Found " + numKeys + " keys for cycle");
          decryptKey(dataSnapshot, "chart", decryptedKeys, numKeys, keyCallback);
          decryptKey(dataSnapshot, "wellness", decryptedKeys, numKeys, keyCallback);
          decryptKey(dataSnapshot, "symptom", decryptedKeys, numKeys, keyCallback);
        }
      });
    }

    private void decryptKey(
        DataSnapshot snapshot,
        final String keyAlias,
        final Map<String, SecretKey> decryptedKeys,
        final long keysToDecrypt,
        final Callback<Map<String, SecretKey>> callback) {
      String encryptedKey = snapshot.child(keyAlias).getValue(String.class);
      Preconditions.checkArgument(!Strings.isNullOrEmpty(encryptedKey));
      if (DEBUG) Log.v(TAG, "Begin decryption of " + keyAlias + " key");
      CryptoUtil.decryptKey(encryptedKey, new Callbacks.ErrorForwardingCallback<SecretKey>(callback) {
        @Override
        public void acceptData(SecretKey decryptedKey) {
          if (DEBUG) Log.v(TAG, "Done decrypting " + keyAlias + " key");
          decryptedKeys.put(keyAlias, decryptedKey);
          if (decryptedKeys.size() == keysToDecrypt) {
            callback.acceptData(decryptedKeys);
          }
        }
      });
    }

    public void putChartKeys(
        final Cycle.Keys keys, final String userId, final Callback<Void> callback) {
      final Runnable onFinish = new Runnable() {
        @Override
        public void run() {
          callback.acceptData(null);
        }
      };
      Callback<Map<String, String>> encryptedKeysCallback = new Callbacks.ErrorForwardingCallback<Map<String, String>>(callback) {
        @Override
        public void acceptData(Map<String, String> encryptedKeys) {
          ref.child(userId).updateChildren(
              Maps.transformValues(encryptedKeys, TO_OBJECT),
              Listeners.completionListener(callback, onFinish));
        }
      };
      final Map<String, String> encryptedKeys = Maps.newConcurrentMap();
      encryptKey(keys.chartKey, "chart", encryptedKeys, 3, encryptedKeysCallback);
      encryptKey(keys.wellnessKey, "wellness", encryptedKeys, 3, encryptedKeysCallback);
      encryptKey(keys.symptomKey, "symptom", encryptedKeys, 3, encryptedKeysCallback);
    }

    private void encryptKey(
        SecretKey key,
        final String alias,
        final Map<String, String> encryptedKeys,
        final int numEncryptions,
        final Callback<Map<String, String>> callback) {
      Log.v("CycleKepProvider", "Encrypting key: " + alias);
      CryptoUtil.encryptKey(key, new Callbacks.ErrorForwardingCallback<String>(callback) {
        @Override
        public void acceptData(String encryptedKey) {
          Log.v("CycleKepProvider", "Encrypted key: " + alias);
          encryptedKeys.put(alias, encryptedKey);
          if (encryptedKeys.size() == numEncryptions) {
            Log.v("CycleKepProvider", "Done encrypting keys");
            callback.acceptData(encryptedKeys);
          }
        }
      });
    }

    public void dropKeys(DatabaseReference.CompletionListener listener) {
      ref.removeValue(listener);
    }
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
