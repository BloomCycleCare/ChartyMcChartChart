package com.roamingroths.cmcc.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by parkeroth on 5/13/17.
 */

public class DataStore {

  private static final FirebaseDatabase DB = FirebaseDatabase.getInstance();

  public static void getCurrentCycle(String userId, final Callback<Cycle> callback) {
    DatabaseReference ref = DB.getReference("cycles").child(userId);
    ref.addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        // TODO: Optimize
        Log.v("DataSource", "Received " + dataSnapshot.getChildrenCount() + " cycles");
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
          if (!snapshot.hasChild("end-date")) {
            Log.v("DataSource", "Found current cycle");
            callback.acceptData(Cycle.fromSnapshot(snapshot));
            return;
          }
        }
        Log.v("DataSource", "Could not find current cycle");
        callback.handleNotFound();
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

  public static Cycle createCycle(
      final Context context, String userId, final LocalDate startDate, final @Nullable LocalDate endDate) {
    final DatabaseReference cycleRef = DB.getReference("cycles").child(userId).push();
    if (endDate != null) {
      cycleRef.child("end-date").setValue(DateUtil.toWireStr(endDate));
    }
    cycleRef.child("start-date").setValue(DateUtil.toWireStr(startDate)).addOnCompleteListener(
        new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {
            try {
              createEmptyEntries(context, cycleRef.getKey(), startDate, endDate);
            } catch (CryptoUtil.CryptoException ce) {
              ce.printStackTrace();
            }
          }
        });
    return new Cycle(cycleRef.getKey(), startDate, endDate);
  }

  private static void createEmptyEntries(
      Context context, String cycleId, LocalDate startDate, @Nullable LocalDate endDate) throws CryptoUtil.CryptoException {
    endDate = (endDate == null) ? LocalDate.now().plusDays(1) : endDate.plusDays(1);
    Map<String, Object> updateMap = new HashMap<>();
    PublicKey publicKey = CryptoUtil.getPersonalPublicKey(context);
    for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
      ChartEntry entry = ChartEntry.emptyEntry(date);
      String encryptedEntry = CryptoUtil.encrypt(entry, publicKey);
      updateMap.put(DateUtil.toWireStr(date), encryptedEntry);
    }
    DB.getReference("entries").child(cycleId).updateChildren(updateMap);
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
