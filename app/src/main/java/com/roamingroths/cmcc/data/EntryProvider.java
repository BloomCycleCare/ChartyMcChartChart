package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public abstract class EntryProvider<E extends Entry> {

  private final Class<E> mClazz;
  private final String mChildId;
  private final FirebaseDatabase db;
  private final String mLogId;

  enum ChildId {
    CHART, WELLNESS, SYMPTOM
  }

  EntryProvider(FirebaseDatabase db, ChildId childId, Class<E> clazz) {
    this.db = db;
    this.mChildId = childId.name().toLowerCase();
    this.mClazz = clazz;
    this.mLogId = "EntryProvider<" + mClazz.getName() + ">";
  }

  abstract E createEmptyEntry(LocalDate date, SecretKey key);

  abstract SecretKey getKey(Cycle cycle);

  abstract void fromSnapshot(DataSnapshot snapshot, SecretKey key, Callback<E> callback);

  public final void maybeAddNewEntries(final Cycle cycle, final Callback<Void> doneCallback) {
    getMostRecentEntryDate(cycle, new Callbacks.ErrorForwardingCallback<LocalDate>(doneCallback) {
      @Override
      public void handleNotFound() {
        logV("No entries found");
        LocalDate today = DateUtil.now();
        final int numDaysWithoutEntries = Days.daysBetween(cycle.startDate, today).getDays();
        logV(numDaysWithoutEntries + " days need entries starting " + today);
        Set<LocalDate> daysWithoutEntries = new HashSet<>(numDaysWithoutEntries);
        for (int i = 0; i < numDaysWithoutEntries; i++) {
          daysWithoutEntries.add(today.minusDays(i));
        }
        makeEntries(daysWithoutEntries);
      }

      @Override
      public void acceptData(LocalDate lastEntryDate) {
        final int numDaysWithoutEntries = Days.daysBetween(lastEntryDate, DateUtil.now()).getDays();
        if (numDaysWithoutEntries == 0) {
          logV("No entries to add");
          doneCallback.acceptData(null);
        }
        logV(numDaysWithoutEntries + " days need entries starting " + lastEntryDate);
        Set<LocalDate> daysWithoutEntries = new HashSet<>(numDaysWithoutEntries);
        for (int i = 0; i < numDaysWithoutEntries; i++) {
          daysWithoutEntries.add(lastEntryDate.plusDays(i + 1));
        }
        Preconditions.checkState(daysWithoutEntries.size() == numDaysWithoutEntries);
        makeEntries(daysWithoutEntries);
      }

      private void makeEntries(final Set<LocalDate> daysWithoutEntries) {
        final Runnable onDone = new Runnable() {
          @Override
          public void run() {
            logV("Done adding new entries");
            doneCallback.acceptData(null);
          }
        };
        final Map<String, Object> updates = Maps.newConcurrentMap();
        for (final LocalDate date : daysWithoutEntries) {
          E emptyEntry = createEmptyEntry(date, getKey(cycle));
          CryptoUtil.encrypt(emptyEntry, new Callbacks.ErrorForwardingCallback<String>(doneCallback) {
            @Override
            public void acceptData(String encryptedEntry) {
              updates.put(DateUtil.toWireStr(date), encryptedEntry);
              if (updates.size() == daysWithoutEntries.size()) {
                logV("New entries encrypted");
                reference(cycle.id).updateChildren(
                    updates, Listeners.completionListener(doneCallback, onDone));
              }
            }
          });
        }
      }
    });
  }

  private final void getMostRecentEntryDate(Cycle cycle, final Callback<LocalDate> callback) {
    final Callbacks.Callback<LocalDate> wrappedCallback =
        Callbacks.singleUse(Preconditions.checkNotNull(callback));
    getDecryptedEntries(cycle, new Callbacks.ErrorForwardingCallback<Map<LocalDate, E>>(wrappedCallback) {
      @Override
      public void acceptData(Map<LocalDate, E> entries) {
        LocalDate lastEntryDate = null;
        for (Map.Entry<LocalDate, E> mapEntry : entries.entrySet()) {
          LocalDate entryDate = mapEntry.getKey();
          if (lastEntryDate == null || lastEntryDate.isBefore(entryDate)) {
            lastEntryDate = entryDate;
          }
        }
        if (lastEntryDate != null) {
          wrappedCallback.acceptData(lastEntryDate);
        } else {
          wrappedCallback.handleNotFound();
        }
      }
    });
  }

  public final void putEntry(
      final String cycleId, final E entry,
      final DatabaseReference.CompletionListener completionListener) throws CryptoUtil.CryptoException {
    CryptoUtil.encrypt(entry, new Callbacks.HaltingCallback<String>() {
      @Override
      public void acceptData(String encryptedEntry) {
        reference(cycleId, entry.getDateStr()).setValue(encryptedEntry, completionListener);
      }
    });
  }

  public final void deleteEntry(
      String cycleId, LocalDate entryDate, DatabaseReference.CompletionListener completionListener) {
    reference(cycleId, DateUtil.toWireStr(entryDate)).removeValue(completionListener);
  }

  public final void getEntry(
      final Cycle cycle, String entryDateStr, final Callback<E> callback) {
    reference(cycle.id, entryDateStr).addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        fromSnapshot(dataSnapshot, getKey(cycle), callback);
      }
    });
  }

  public final void getEncryptedEntries(
      String cycleId, final Callback<Map<LocalDate, String>> callback) {
    reference(cycleId).addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot entrySnapshots) {
        Map<LocalDate, String> entries = new HashMap<>();
        for (DataSnapshot entrySnapshot : entrySnapshots.getChildren()) {
          LocalDate entryDate = DateUtil.fromWireStr(entrySnapshot.getKey());
          entries.put(entryDate, entrySnapshot.getValue(String.class));
        }
        callback.acceptData(entries);
      }
    });
  }

  public final void getDecryptedEntries(final Cycle cycle, final Callback<Map<LocalDate, E>> callback) {
    getEncryptedEntries(cycle.id, new Callbacks.ErrorForwardingCallback<Map<LocalDate, String>>(callback) {
      @Override
      public void acceptData(final Map<LocalDate, String> encryptedEntries) {
        final Map<LocalDate, E> decryptedEntries = Maps.newConcurrentMap();
        final Set<LocalDate> brokenEntries = Sets.newConcurrentHashSet();
        logV("Found " + encryptedEntries.size() + " entries to decrypt.");
        if (encryptedEntries.isEmpty()) {
          callback.acceptData(ImmutableMap.<LocalDate, E>of());
        }
        for (Map.Entry<LocalDate, String> entry : encryptedEntries.entrySet()) {
          final LocalDate entryDate = entry.getKey();
          final String encryptedEntry = entry.getValue();
          CryptoUtil.decrypt(encryptedEntry, getKey(cycle), mClazz, new Callbacks.Callback<E>() {
            @Override
            public void acceptData(E decryptedEntry) {
              decryptedEntries.put(decryptedEntry.getDate(), decryptedEntry);
              maybeRespond();
            }

            @Override
            public void handleNotFound() {
              logV("Decrypted entry not found for " + entryDate);
              brokenEntries.add(entryDate);
              maybeRespond();
            }

            @Override
            public void handleError(DatabaseError error) {
              logV("Error decrypting entry for " + entryDate);
              brokenEntries.add(entryDate);
              maybeRespond();
            }

            private synchronized void maybeRespond() {
              if (decryptedEntries.size() + brokenEntries.size() == encryptedEntries.size()) {
                if (brokenEntries.size() == 0) {
                  callback.acceptData(decryptedEntries);
                } else {
                  callback.handleError(DatabaseError.fromException(new IllegalStateException()));
                }
              }
            }
          });
        }
      }
    });
  }

  private DatabaseReference reference(String cycleId) {
    return db.getReference("entries").child(cycleId).child(mChildId);
  }

  private DatabaseReference reference(String cycleId, String dateStr) {
    return reference(cycleId).child(dateStr);
  }

  private void logV(String message) {
    Log.v(mLogId, message);
  }
}
