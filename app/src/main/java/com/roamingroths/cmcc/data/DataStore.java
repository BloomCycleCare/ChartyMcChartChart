package com.roamingroths.cmcc.data;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.ChartEntryAdapter;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;
import com.roamingroths.cmcc.utils.Listeners.SimpleValueEventListener;

import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

  public static void createCycle(
      final Context context,
      String userId,
      @Nullable Cycle previousCycle,
      @Nullable Cycle nextCycle,
      final LocalDate startDate,
      final @Nullable LocalDate endDate,
      final Callback<Cycle> callback) {
    DatabaseReference cycleRef = DB.getReference("cycles").child(userId).push();
    final Cycle cycle = new Cycle(
        cycleRef.getKey(),
        (previousCycle == null) ? null : previousCycle.id,
        (nextCycle == null) ? null : nextCycle.id,
        startDate,
        endDate);
    Map<String, Object> updates = new HashMap<>();
    updates.put("previous-cycle-id", cycle.previousCycleId);
    updates.put("next-cycle-id", cycle.nextCycleId);
    updates.put("start-date", cycle.startDateStr);
    updates.put("end-date", DateUtil.toWireStr(cycle.endDate));
    cycleRef.updateChildren(updates, new DatabaseReference.CompletionListener() {
      @Override
      public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        if (databaseError == null) {
          callback.acceptData(cycle);
        } else {
          callback.handleError(databaseError);
        }
      }
    });
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

  private static void moveEntries(
      final Cycle fromCycle,
      final Cycle toCycle,
      final String userId,
      final Predicate<LocalDate> datePredicate,
      final Callback<?> callback) {
    final DatabaseReference sourceEntrisRef =
        DB.getReference("entries").child(fromCycle.id);
    final DatabaseReference destinationEntriesRef =
        DB.getReference("entries").child(toCycle.id);
    sourceEntrisRef.addListenerForSingleValueEvent(new SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot entrySnapshots) {
        Map<String, String> entriesToMove = new HashMap<>();
        for (DataSnapshot entrySnapshot : entrySnapshots.getChildren()) {
          final String entryDateStr = entrySnapshot.getKey();
          LocalDate entryDate = DateUtil.fromWireStr(entryDateStr);
          if (datePredicate.apply(entryDate)) {
            entriesToMove.put(entryDateStr, entrySnapshot.getValue(String.class));
          }
        }
        final AtomicLong outstandingRemovals = new AtomicLong(entriesToMove.size());
        final boolean shouldDropCycle = entriesToMove.size() == entrySnapshots.getChildrenCount();
        for (Map.Entry<String, String> entry : entriesToMove.entrySet()) {
          final String dateStr = entry.getKey();
          final String encryptedEntry = entry.getValue();
          DatabaseReference.CompletionListener moveCompleteListener =
              Listeners.completionListener(callback, new Runnable() {
                @Override
                public void run() {
                  DatabaseReference.CompletionListener rmListener =
                      Listeners.completionListener(callback, new Runnable() {
                        @Override
                        public void run() {
                          if (outstandingRemovals.decrementAndGet() == 0) {
                            if (shouldDropCycle) {
                              DB.getReference("cycles").child(userId).child(fromCycle.id).removeValue(
                                  Listeners.completionListener(callback));
                            }
                            callback.acceptData(null);
                          }
                        }
                      });
                  sourceEntrisRef.child(dateStr).removeValue(rmListener);
                }
              });
          destinationEntriesRef.child(dateStr).setValue(encryptedEntry, moveCompleteListener);
        }
      }
    });
  }

  public static void combineCycles(
      final Cycle currentCycle,
      final String userId,
      final Callback<Cycle> mergedCycleCallback) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(currentCycle.previousCycleId));
    getCycle(
        userId,
        currentCycle.previousCycleId,
        Callbacks.singleUse(new Callbacks.ErrorForwardingCallback<Cycle>(mergedCycleCallback) {
      @Override
      public void acceptData(final Cycle previousCycle) {
        DatabaseReference previousCycleRef =
            DB.getReference("cycles").child(userId).child(previousCycle.id);
        Map<String, Object> updates = new HashMap<>();
        updates.put("next-cycle-id", currentCycle.nextCycleId);
        updates.put("end-date", DateUtil.toWireStr(currentCycle.endDate));
        previousCycleRef.updateChildren(updates, Listeners.completionListener(this));
        if (currentCycle.nextCycleId != null) {
          DatabaseReference nextCycleRef =
              DB.getReference("cycles").child(userId).child(currentCycle.nextCycleId);
          nextCycleRef.child("previous-cycle-id").setValue(
              previousCycle.id, Listeners.completionListener(this));
        }
        moveEntries(
            currentCycle,
            previousCycle,
            userId,
            Predicates.<LocalDate>alwaysTrue(),
            new Callbacks.ErrorForwardingCallback<Void>(mergedCycleCallback) {
              @Override
              public void acceptData(Void unused) {
                mergedCycleCallback.acceptData(previousCycle);
              }
            });
      }
        }));
  }

  public static void splitCycle(
      final Context context,
      final String userId,
      final Cycle currentCycle,
      final ChartEntry firstEntry,
      final Callback<Cycle> resultCallback) {
    getCycle(
        userId,
        currentCycle.nextCycleId,
        Callbacks.singleUse(new Callbacks.ErrorForwardingCallback<Cycle>(resultCallback) {
      @Override
      public void acceptData(@Nullable final Cycle nextCycle) {
        createCycle(
            context,
            userId,
            currentCycle,
            nextCycle,
            firstEntry.date,
            currentCycle.endDate,
            Callbacks.singleUse(new Callbacks.ErrorForwardingCallback<Cycle>(this) {
          @Override
          public void acceptData(final Cycle newCycle) {
            if (nextCycle != null) {
              DB.getReference("cycles").child(userId).child(nextCycle.id).child("previous-cycle-id")
                  .setValue(newCycle.id, Listeners.completionListener(this));
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("next-cycle-id", newCycle.id);
            updates.put("end-date", DateUtil.toWireStr(firstEntry.date.minusDays(1)));
            DB.getReference("cycles").child(userId).child(currentCycle.id).updateChildren(
                updates, Listeners.completionListener(this));
            moveEntries(currentCycle, newCycle, userId, new Predicate<LocalDate>() {
              @Override
              public boolean apply(LocalDate entryDate) {
                return entryDate.equals(firstEntry.date) || entryDate.isAfter(firstEntry.date);
              }
            }, Callbacks.singleUse(new Callbacks.ErrorForwardingCallback<Void>(this) {
              @Override
              public void acceptData(Void data) {
                resultCallback.acceptData(newCycle);
              }
            }));
          }
            }));
      }
        }));
  }
}