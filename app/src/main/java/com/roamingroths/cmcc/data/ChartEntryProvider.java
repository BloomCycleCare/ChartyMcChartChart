package com.roamingroths.cmcc.data;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class ChartEntryProvider extends EntryProvider<ChartEntry> {

  public static ChartEntryProvider forDb(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    return new ChartEntryProvider(db, cryptoUtil);
  }

  private ChartEntryProvider(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    super(db, cryptoUtil, ChildId.CHART, ChartEntry.class);
  }


  @Override
  ChartEntry createEmptyEntry(LocalDate date, SecretKey key) {
    return ChartEntry.emptyEntry(date, key);
  }

  @Override
  public SecretKey getKey(Cycle cycle) {
    return cycle.keys.chartKey;
  }
}
