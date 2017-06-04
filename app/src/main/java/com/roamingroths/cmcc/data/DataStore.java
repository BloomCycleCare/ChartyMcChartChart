package com.roamingroths.cmcc.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.ChartEntryAdapter;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.EventListeners.SimpleValueEventListener;

import org.joda.time.LocalDate;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

  public static void fillCycleEntryAdapter(
      Cycle cycle, final Context context, final ChartEntryAdapter adapter,
      final Callback<LocalDate> doneCallback) {
    Log.v("DataStore", "Begin filling ChartEntryAdapter");
    DatabaseReference dbRef = DB.getReference("entries").child(cycle.id);
    dbRef.keepSynced(true);
    dbRef.addListenerForSingleValueEvent(new SimpleValueEventListener(doneCallback) {
      @Override
      public void onDataChange(DataSnapshot entriesSnapshot) {
        final AtomicLong entriesToDecrypt = new AtomicLong(entriesSnapshot.getChildrenCount());
        final AtomicReference<LocalDate> lastEntryDate = new AtomicReference<>();
        for (DataSnapshot entrySnapshot : entriesSnapshot.getChildren()) {
          LocalDate entryDate = DateUtil.fromWireStr(entrySnapshot.getKey());
          if (lastEntryDate.get() == null || lastEntryDate.get().isBefore(entryDate)) {
            lastEntryDate.set(entryDate);
          }
          ChartEntry.fromEncryptedString(entrySnapshot.getValue(String.class), context,
              new Callbacks.ErrorForwardingCallback<ChartEntry>(doneCallback) {

                @Override
                public void acceptData(ChartEntry entry) {
                  adapter.addEntry(entry);
                  long numLeftToDecrypt = entriesToDecrypt.decrementAndGet();
                  if (numLeftToDecrypt < 1) {
                    Log.v("DataStore", "Done filling ChartEntryAdapter");
                    doneCallback.acceptData(lastEntryDate.get());
                  } else {
                    Log.v("DataStore", "Still waiting for " + numLeftToDecrypt + " decryptions");
                  }
                }
              });
        }
      }
    });
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
    Cycle cycle = new Cycle(cycleRef.getKey(), startDate, endDate);
    if (endDate != null) {
      cycleRef.child("end-date").setValue(DateUtil.toWireStr(endDate));
    }
    cycleRef.child("start-date").setValue(cycle.startDateStr).addOnCompleteListener(
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

  public static void createEmptyEntries(
      Context context, String cycleId, LocalDate startDate, @Nullable LocalDate endDate) throws CryptoUtil.CryptoException {
    final DatabaseReference ref = DB.getReference("entries").child(cycleId);
    endDate = (endDate == null) ? LocalDate.now().plusDays(1) : endDate.plusDays(1);
    for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
      Log.v("DataStore", "Creating empty entry for " + cycleId + " " + date);
      final ChartEntry entry = ChartEntry.emptyEntry(date);
      CryptoUtil.encrypt(entry, context, new Callbacks.HaltingCallback<String>() {
        @Override
        public void acceptData(String encryptedEntry) {
          ref.child(entry.getDateStr()).setValue(encryptedEntry);
        }
      });
    }
  }

  public static void putChartEntry(Context context, final String cycleId, final ChartEntry entry)
      throws CryptoUtil.CryptoException {
    CryptoUtil.encrypt(entry, context, new Callbacks.HaltingCallback<String>() {
      @Override
      public void acceptData(String encryptedEntry) {
        DB.getReference("entries").child(cycleId).child(entry.getDateStr()).setValue(encryptedEntry);
      }
    });
  }

  public static void deleteChartEntry(String cycleId, LocalDate entryDate) {
    String entryDateStr = DateUtil.toWireStr(entryDate);
    DB.getReference("entries").child(cycleId).child(entryDateStr).removeValue();
  }

  public static void getChartEntry(
      final Context context, String cycleId, String entryDateStr, final Callback<ChartEntry> callback) {
    DB.getReference("entries").child(cycleId).child(entryDateStr)
        .addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            ChartEntry.fromSnapshot(dataSnapshot, context, callback);
          }
        });
  }
}
