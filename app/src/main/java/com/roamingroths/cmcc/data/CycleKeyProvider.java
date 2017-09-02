package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.Listeners;

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

    private Instance(String cycleId) {
      ref = reference(cycleId);
    }

    public void getKey(
        String userId, final Callback<SecretKey> callback) {
      Log.v("CycleKeyProvider", "Fetching key");
      ref.child(userId).addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
          Log.v("CycleKeyProvider", "Found key for cycle");
          String encryptedKey = dataSnapshot.getValue(String.class);
          CryptoUtil.decryptKey(encryptedKey, callback);
        }
      });
    }

    public void putKey(String encryptedKey, String userId, Callback<?> callback) {
      ref.child(userId).setValue(encryptedKey, Listeners.completionListener(callback));
    }

    public void dropKeys(Callback<?> callback) {
      ref.removeValue(Listeners.completionListener(callback));
    }
  }

  private DatabaseReference reference(String cycleId) {
    return db.getReference("keys").child(cycleId);
  }
}
