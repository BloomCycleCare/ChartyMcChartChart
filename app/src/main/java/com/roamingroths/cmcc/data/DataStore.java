package com.roamingroths.cmcc.data;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.security.PublicKey;

/**
 * Created by parkeroth on 5/13/17.
 */

public class DataStore {

  private static final FirebaseDatabase DB = FirebaseDatabase.getInstance();

  public static void getCurrentCycle(String userId, final Callback<Cycle> callback) {
    DatabaseReference ref = DB.getReference("cycles").child(userId);
    Query query = ref.orderByChild("start-date").limitToLast(1);
    query.addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null || !dataSnapshot.exists()) {
          callback.handleNotFound();
          return;
        }
        if (dataSnapshot.getChildrenCount() != 1) {
          throw new IllegalStateException("Received wrong number of Cycle snapshots.");
        }
        callback.acceptData(Cycle.fromSnapshot(dataSnapshot.getChildren().iterator().next()));
      }
    });
  }

  public static void attachCycleListener(ChildEventListener listener, String userId) {
    DatabaseReference dbRef = DB.getReference("cycles").child(userId);
    dbRef.addChildEventListener(listener);
    dbRef.keepSynced(true);
  }

  public static void detachCycleListener(ChildEventListener listener, String userId) {
    DB.getReference("cycles").child(userId).removeEventListener(listener);
  }

  public static void attachCycleEntryListener(ChildEventListener listener, Cycle cycle) {
    DatabaseReference dbRef = DB.getReference("entries").child(cycle.id);
    dbRef.addChildEventListener(listener);
    dbRef.keepSynced(true);
  }

  public static void detatchCycleEntryListener(ChildEventListener listener, Cycle cycle) {
    DB.getReference("entries").child(cycle.id).removeEventListener(listener);
  }

  public static Cycle createInitialCycle(final String userId, LocalDate startDate) {
    DatabaseReference cycleRef = DB.getReference("cycles").child(userId).push();
    cycleRef.child("start-date").setValue(DateUtil.toWireStr(startDate));
    return new Cycle(cycleRef.getKey(), startDate, null);
  }

  public static void putChartEntry(Context context, String cycleId, ChartEntry entry) throws CryptoUtil.CryptoException {
    PublicKey publicKey = CryptoUtil.getPersonalPublicKey(context);
    String encryptedEntry = CryptoUtil.encrypt(entry, publicKey);
    DB.getReference("entries").child(cycleId).child(entry.getDateStr()).setValue(encryptedEntry);
  }

  public static void deleteChartEntry(String cycleId, LocalDate entryDate) {
    String entryDateStr = DateUtil.toWireStr(entryDate);
    DB.getReference("entries").child(cycleId).child(entryDateStr).removeValue();
  }

  public static void getChartEntry(final Context context, String cycleId, String entryDateStr, final Callback<ChartEntry> callback) {
    DB.getReference("entries").child(cycleId).child(entryDateStr)
        .addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            String encryptedEntry = dataSnapshot.getValue(String.class);
            try {
              ChartEntry decryptedEntry = CryptoUtil.decrypt(encryptedEntry, CryptoUtil.getPersonalPrivateKey(context), ChartEntry.class);
              callback.acceptData(decryptedEntry);
            } catch (CryptoUtil.CryptoException ce) {
              callback.handleError(DatabaseError.fromException(ce));
            }
          }
        });
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
