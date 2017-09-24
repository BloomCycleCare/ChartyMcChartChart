package com.roamingroths.cmcc.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.SymptomEntry;
import com.roamingroths.cmcc.utils.Callbacks.Callback;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class SymptomEntryProvider extends EntryProvider<SymptomEntry> {

  public static SymptomEntryProvider forDb(FirebaseDatabase db) {
    return new SymptomEntryProvider(db);
  }

  private SymptomEntryProvider(FirebaseDatabase db) {
    super(db, ChildId.SYMPTOM, SymptomEntry.class);
  }


  @Override
  SymptomEntry createEmptyEntry(LocalDate date, SecretKey key) {
    return SymptomEntry.emptyEntry(date, key);
  }

  @Override
  SecretKey getKey(Cycle cycle) {
    return cycle.keys.symptomKey;
  }

  @Override
  void fromSnapshot(DataSnapshot snapshot, SecretKey key, Callback<SymptomEntry> callback) {
    SymptomEntry.fromSnapshot(snapshot, key, callback);
  }
}
