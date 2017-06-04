package com.roamingroths.cmcc.data;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

  public static void getCycle(String userId, @Nullable String cycleId, final Callback<Cycle> callback) {
    if (Strings.isNullOrEmpty(cycleId)) {
      callback.acceptData(null);
      return;
    }
    DB.getReference("cycles").child(userId).child(cycleId).addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        callback.acceptData(Cycle.fromSnapshot(dataSnapshot));
      }
    });
  }

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
      final Context context,
      String userId,
      @Nullable Cycle previousCycle,
      @Nullable Cycle nextCycle,
      final LocalDate startDate,
      final @Nullable LocalDate endDate) {
    final DatabaseReference cycleRef = DB.getReference("cycles").child(userId).push();
    Cycle cycle = new Cycle(
        cycleRef.getKey(),
        (previousCycle == null) ? null : previousCycle.id,
        (nextCycle == null) ? null : nextCycle.id,
        startDate,
        endDate);
    if (cycle.endDate != null) {
      cycleRef.child("end-date").setValue(DateUtil.toWireStr(endDate));
    }
    cycleRef.child("previous-cycle-id").setValue(cycle.previousCycleId);
    cycleRef.child("next-cycle-id").setValue(cycle.nextCycleId);
    cycleRef.child("start-date").setValue(cycle.startDateStr);
    return cycle;
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

  public static void combineCycles(
      final Cycle currentCycle,
      final String userId,
      final Callback<Cycle> mergedCycleCallback) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(currentCycle.previousCycleId));
    getCycle(userId, currentCycle.previousCycleId, new Callbacks.ErrorForwardingCallback<Cycle>(mergedCycleCallback) {
      @Override
      public void acceptData(final Cycle previousCycle) {
        DatabaseReference previousCycleRef =
            DB.getReference("cycles").child(userId).child(previousCycle.id);
        previousCycleRef.child("next-cycle-id").setValue(currentCycle.nextCycleId);
        previousCycleRef.child("end-date").setValue(DateUtil.toWireStr(currentCycle.endDate));
        if (currentCycle.nextCycleId != null) {
          DatabaseReference nextCycleRef =
              DB.getReference("cycles").child(userId).child(currentCycle.nextCycleId);
          nextCycleRef.child("previous-cycle-id").setValue(previousCycle.id);
        }
        DB.getReference("cycles").child(userId).child(currentCycle.id).removeValue();
        final DatabaseReference currentEntriesRef =
            DB.getReference("entries").child(currentCycle.id);
        currentEntriesRef.addListenerForSingleValueEvent(new SimpleValueEventListener(mergedCycleCallback) {
          @Override
          public void onDataChange(DataSnapshot entrySnapshots) {
            DatabaseReference previousEntriesRef =
                DB.getReference("entries").child(previousCycle.id);
            for (DataSnapshot entrySnapshot : entrySnapshots.getChildren()) {
              final String entryDateStr = entrySnapshot.getKey();
              previousEntriesRef.child(entryDateStr).setValue(
                  entrySnapshot.getValue(String.class), new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(
                        DatabaseError databaseError, DatabaseReference databaseReference) {
                      currentEntriesRef.child(entryDateStr).removeValue();
                    }
                  });
            }
          }
        });
      }
    });
  }

  public static void splitCycle(
      final Context context,
      final String userId,
      final Cycle currentCycle,
      final ChartEntry firstEntry,
      final Callback<Cycle> newCycleCallback) {
    final DatabaseReference currentCycleRef =
        DB.getReference("cycles").child(userId).child(currentCycle.id);
    getCycle(userId, currentCycle.nextCycleId, new Callbacks.ErrorForwardingCallback<Cycle>(newCycleCallback) {
      @Override
      public void acceptData(@Nullable Cycle nextCycle) {
        final Cycle newCycle = createCycle(
            context, userId, currentCycle, nextCycle, firstEntry.date, currentCycle.endDate);
        currentCycleRef.child("next-cycle-id").setValue(newCycle.id);
        if (nextCycle != null) {
          DB.getReference("cycles").child(userId).child(nextCycle.id).child("previous-cycle-id")
              .setValue(newCycle.id);
        }

        LocalDate currentCycleEndDate = firstEntry.date.minusDays(1);
        currentCycleRef.child("end-date").setValue(DateUtil.toWireStr(currentCycleEndDate));

        // Move the entries
        final DatabaseReference currentEntriesRef =
            DB.getReference("entries").child(currentCycle.id);
        final DatabaseReference newEntriesRef =
            DB.getReference("entries").child(newCycle.id);
        currentEntriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot entrySnapshots) {
            for (DataSnapshot entrySnapshot : entrySnapshots.getChildren()) {
              final String entryDateStr = entrySnapshot.getKey();
              LocalDate entryDate = DateUtil.fromWireStr(entryDateStr);
              String encryptedEntry = entrySnapshot.getValue(String.class);
              if (entryDate.equals(firstEntry.date) || entryDate.isAfter(firstEntry.date)) {
                Log.v(
                    "DataStore",
                    "Moving " + entryDateStr + " from " + currentCycle.id + " to " + newCycle.id);
                newEntriesRef.child(entryDateStr).setValue(
                    encryptedEntry, new DatabaseReference.CompletionListener() {
                      @Override
                      public void onComplete(
                          DatabaseError databaseError, DatabaseReference databaseReference) {
                        currentEntriesRef.child(entryDateStr).removeValue();
                      }
                    });
              }
            }
            newCycleCallback.acceptData(newCycle);
          }

          @Override
          public void onCancelled(DatabaseError databaseError) {
            newCycleCallback.handleError(databaseError);
          }
        });
      }
    });
  }
}
