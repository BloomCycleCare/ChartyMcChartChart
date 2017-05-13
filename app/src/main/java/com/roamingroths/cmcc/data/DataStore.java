package com.roamingroths.cmcc.data;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roamingroths.cmcc.utils.CryptoUtil;

import java.security.PublicKey;

/**
 * Created by parkeroth on 5/13/17.
 */

public class DataStore {

  private static final FirebaseDatabase DB = FirebaseDatabase.getInstance();

  public static void getCurrentCycleId(String userId, final Callback<String> callback) {
    DatabaseReference ref = DB.getReference("users").child(userId).child("current-cycle");
    ref.addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        String cycleId = (String) dataSnapshot.getValue();
        if (Strings.isNullOrEmpty(cycleId)) {
          callback.handleNotFound();
        } else {
          callback.acceptData(cycleId);
        }
      }
    });
  }

  public static void getCurrentCycle(
      final Context context, String userId, final Callback<Cycle> callback) {
    getCurrentCycleId(userId, new SimpleCallback<String>(callback) {
      @Override
      public void acceptData(String cycleId) {
        DatabaseReference ref = DB.getReference("cycles").child(cycleId);
        ref.addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            try {
              callback.acceptData(Cycle.fromDataSnapshot(context, dataSnapshot));
            } catch (Exception e) {
              callback.handleError(DatabaseError.fromException(e));
            }
          }
        });
      }
    });
  }

  public static Cycle createCycle(final String userId, final Cycle cycle) {
    // TODO: This should probably all be in a transaction...
    DatabaseReference cycleRef = DB.getReference("cycles").child(cycle.id);
    cycleRef.child("start-date").setValue(cycle.getDateStr());
    cycleRef.child("user").setValue(userId);
    getCurrentCycleId(userId, new Callback<String>() {
      @Override
      public void acceptData(String data) {
      }

      @Override
      public void handleNotFound() {
        DB.getReference("users").child(userId).child("current-cycle").setValue(cycle.id);
      }

      @Override
      public void handleError(DatabaseError error) {
      }
    });
    return cycle;
  }

  public static void putChartEntry(Context context, String cycleId, ChartEntry entry) throws CryptoUtil.CryptoException {
    PublicKey publicKey = CryptoUtil.getPersonalPublicKey(context);
    String encryptedEntry = CryptoUtil.encrypt(entry, publicKey);
    DB.getReference("cycles").child(cycleId).child("entries").child(entry.getDateStr()).setValue(encryptedEntry);
  }

  public interface Callback<T> {
    void acceptData(T data);

    void handleNotFound();

    void handleError(DatabaseError error);
  }

  private static abstract class SimpleCallback<T> implements Callback<T> {

    private final Callback<?> mCallback;

    public SimpleCallback(Callback<?> callback) {
      mCallback = callback;
    }

    @Override
    public void handleNotFound() {
      mCallback.handleNotFound();
    }

    @Override
    public void handleError(DatabaseError databaseError) {
      mCallback.handleError(databaseError);
    }
  }

  private static abstract class SimpleValueEventListener implements ValueEventListener {

    private final Callback<?> mCallback;

    public SimpleValueEventListener(Callback<?> callback) {
      mCallback = Preconditions.checkNotNull(callback);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
      mCallback.handleError(databaseError);
    }
  }
}
