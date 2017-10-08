package com.roamingroths.cmcc.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks.Callback;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class ChartEntryProvider extends EntryProvider<ChartEntry> {

  public static ChartEntryProvider forDb(FirebaseDatabase db) {
    return new ChartEntryProvider(db);
  }

  private ChartEntryProvider(FirebaseDatabase db) {
    super(db, ChildId.CHART, ChartEntry.class);
  }


  @Override
  ChartEntry createEmptyEntry(LocalDate date, SecretKey key) {
    return ChartEntry.emptyEntry(date, key);
  }

  @Override
  public SecretKey getKey(Cycle cycle) {
    return cycle.keys.chartKey;
  }

  @Override
  void fromSnapshot(DataSnapshot snapshot, SecretKey key, Callback<ChartEntry> callback) {
    ChartEntry.fromSnapshot(snapshot, key, callback);
  }
}
