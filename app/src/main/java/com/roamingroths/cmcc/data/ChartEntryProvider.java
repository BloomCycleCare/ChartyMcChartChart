package com.roamingroths.cmcc.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.Map;

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
    return db.getReference("entries").child(cycleId);
  }

  private DatabaseReference reference(String cycleId, String dateStr) {
    return reference(cycleId).child(dateStr);
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

  public void getEntries(
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
}
