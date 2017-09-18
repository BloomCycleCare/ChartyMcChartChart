package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.Listeners;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class CycleKeyProvider {

  private final FirebaseDatabase db;

  public static CycleKeyProvider forDb(FirebaseDatabase db) {
    return new CycleKeyProvider(db);
  }

  private CycleKeyProvider(FirebaseDatabase db) {
    this.db = db;
  }

  public Instance forCycle(String cycleId) {
    return new Instance(cycleId);
  }

  public class Instance {

    private final DatabaseReference ref;
    private final String mCycleId;

    private Instance(String cycleId) {
      mCycleId = cycleId;
      ref = reference(cycleId);
    }

    public void getChartKeys(
        String userId, final Callback<Cycle.Keys> callback) {
      Log.v("CycleKeyProvider", "Fetching key for user " + userId + " cycle " + ref.getKey());
      final Map<String, SecretKey> decryptedKeys = new ConcurrentHashMap<>();
      final Callback<Map<String, SecretKey>> keyCallback = new Callbacks.ErrorForwardingCallback<Map<String, SecretKey>>(callback) {
        @Override
        public void acceptData(Map<String, SecretKey> decryptedKeys) {
          callback.acceptData(new Cycle.Keys(
              decryptedKeys.get("chart"), decryptedKeys.get("wellness"), decryptedKeys.get("symptoms")));
        }
      };
      ref.child(userId).addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
          Log.v("CycleKeyProvider", "Found key for cycle");
          decryptKey(dataSnapshot, "chart", decryptedKeys, 3, keyCallback);
          decryptKey(dataSnapshot, "wellness", decryptedKeys, 3, keyCallback);
          decryptKey(dataSnapshot, "symptoms", decryptedKeys, 3, keyCallback);
        }
      });
    }

    private void decryptKey(
        DataSnapshot snapshot,
        final String keyAlias,
        final Map<String, SecretKey> decryptedKeys,
        final int keysToDecrypt,
        final Callback<Map<String, SecretKey>> callback) {
      String encryptedKey = snapshot.child(keyAlias).getValue(String.class);
      CryptoUtil.decryptKey(encryptedKey, new Callbacks.ErrorForwardingCallback<SecretKey>(callback) {
        @Override
        public void acceptData(SecretKey decryptedKey) {
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
          reference(mCycleId).child(userId).updateChildren(
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

    public void putChartKey(String encryptedKey, String userId, Callback<?> callback) {
      ref.child(userId).child("chart").setValue(encryptedKey, Listeners.completionListener(callback));
    }

    public void dropKeys(Callback<?> callback) {
      ref.removeValue(Listeners.completionListener(callback));
    }
  }

  private DatabaseReference reference(String cycleId) {
    return db.getReference("keys").child(cycleId);
  }

  private static final Function<String, Object> TO_OBJECT = new Function<String, Object>() {
    @Override
    public Object apply(String input) {
      return input;
    }
  };
}
