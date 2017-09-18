package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class ChartEntryProvider {

  private final FirebaseDatabase db;

  public static ChartEntryProvider forDb(FirebaseDatabase db) {
    return new ChartEntryProvider(db);
  }

  private ChartEntryProvider(FirebaseDatabase db) {
    this.db = db;
  }

  private DatabaseReference reference(String cycleId) {
    return db.getReference("entries").child(cycleId).child("chart");
  }

  private DatabaseReference reference(String cycleId, String dateStr) {
    return reference(cycleId).child(dateStr);
  }

  public void createEmptyEntries(
      final Cycle cycle, final Set<LocalDate> dates, final Callback<Map<LocalDate, ChartEntry>> callback) {
    final Map<LocalDate, ChartEntry> entries = Maps.newConcurrentMap();
    Log.v("ChartEntryList", "Creating " + dates.size() + " entries");
    for (LocalDate date : dates) {
      Log.v("ChartEntryList", "Creating empty entry for " + cycle.id + " " + date);
      final ChartEntry entry = ChartEntry.emptyEntry(date, cycle.keys.chartKey);
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          entries.put(entry.date, entry);
          int entriesRemaining = dates.size() - entries.size();
          if (entriesRemaining == 0) {
            callback.acceptData(entries);
          }
          Log.v("ChartEntryList", "Still waiting for " + entriesRemaining + " creations");
        }
      };
      try {
        putEntry(cycle.id, entry, Listeners.completionListener(callback, runnable));
      } catch (CryptoUtil.CryptoException ce) {
        callback.handleError(DatabaseError.fromException(ce));
      }
    }
  }

  public void putEntry(
      final String cycleId,
      final ChartEntry entry,
      final DatabaseReference.CompletionListener completionListener) throws CryptoUtil.CryptoException {
    CryptoUtil.encrypt(entry, new Callbacks.HaltingCallback<String>() {
      @Override
      public void acceptData(String encryptedEntry) {
        reference(cycleId, entry.getDateStr()).setValue(encryptedEntry, completionListener);
      }
    });
  }

  public void deleteChartEntry(
      String cycleId, LocalDate entryDate, DatabaseReference.CompletionListener completionListener) {
    reference(cycleId, DateUtil.toWireStr(entryDate)).removeValue(completionListener);
  }

  public void getChartEntry(
      String cycleId, String entryDateStr, final SecretKey key, final Callback<ChartEntry> callback) {
    reference(cycleId, entryDateStr).addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        ChartEntry.fromSnapshot(dataSnapshot, key, callback);
      }
    });
  }

  public void getEncryptedEntries(
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

  public void getDecryptedEntries(final Cycle cycle, final Callback<Map<LocalDate, ChartEntry>> callback) {
    getEncryptedEntries(cycle.id, new Callbacks.ErrorForwardingCallback<Map<LocalDate, String>>(callback) {
      @Override
      public void acceptData(final Map<LocalDate, String> encryptedEntries) {
        final Map<LocalDate, ChartEntry> decryptedEntries = Maps.newConcurrentMap();
        Log.v("ChartEntryProvider", "Found " + encryptedEntries.size() + " entries to decrypt.");
        if (encryptedEntries.isEmpty()) {
          callback.acceptData(ImmutableMap.<LocalDate, ChartEntry>of());
        }
        for (Map.Entry<LocalDate, String> entry : encryptedEntries.entrySet()) {
          String encryptedEntry = entry.getValue();
          CryptoUtil.decrypt(encryptedEntry, cycle.keys.chartKey, ChartEntry.class, new Callbacks.ErrorForwardingCallback<ChartEntry>(this) {
            @Override
            public void acceptData(ChartEntry decryptedEntry) {
              decryptedEntries.put(decryptedEntry.date, decryptedEntry);
              if (decryptedEntries.size() == encryptedEntries.size()) {
                callback.acceptData(decryptedEntries);
              }
            }
          });
        }
      }
    });
  }
}
