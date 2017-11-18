package com.roamingroths.cmcc.data;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.ObservationEntry;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class ObservationEntryProvider extends EntryProvider<ObservationEntry> {

  public static ObservationEntryProvider forDb(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    return new ObservationEntryProvider(db, cryptoUtil);
  }

  private ObservationEntryProvider(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    super(db, cryptoUtil, ChildId.CHART, ObservationEntry.class);
  }


  @Override
  ObservationEntry createEmptyEntry(LocalDate date, SecretKey key) {
    return ObservationEntry.emptyEntry(date, key);
  }

  @Override
  public SecretKey getKey(Cycle cycle) {
    return cycle.keys.chartKey;
  }
}
