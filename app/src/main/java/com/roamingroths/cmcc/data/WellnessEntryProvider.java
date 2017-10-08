package com.roamingroths.cmcc.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.WellnessEntry;
import com.roamingroths.cmcc.utils.Callbacks.Callback;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class WellnessEntryProvider extends EntryProvider<WellnessEntry> {

  public static WellnessEntryProvider forDb(FirebaseDatabase db) {
    return new WellnessEntryProvider(db);
  }

  private WellnessEntryProvider(FirebaseDatabase db) {
    super(db, ChildId.WELLNESS, WellnessEntry.class);
  }


  @Override
  WellnessEntry createEmptyEntry(LocalDate date, SecretKey key) {
    return WellnessEntry.emptyEntry(date, key);
  }

  @Override
  public SecretKey getKey(Cycle cycle) {
    return cycle.keys.wellnessKey;
  }

  @Override
  void fromSnapshot(DataSnapshot snapshot, SecretKey key, Callback<WellnessEntry> callback) {
    WellnessEntry.fromSnapshot(snapshot, key, callback);
  }
}
