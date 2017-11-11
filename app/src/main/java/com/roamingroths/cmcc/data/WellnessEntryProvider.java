package com.roamingroths.cmcc.data;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.WellnessEntry;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class WellnessEntryProvider extends EntryProvider<WellnessEntry> {

  public static WellnessEntryProvider forDb(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    return new WellnessEntryProvider(db, cryptoUtil);
  }

  private WellnessEntryProvider(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    super(db, cryptoUtil, ChildId.WELLNESS, WellnessEntry.class);
  }


  @Override
  WellnessEntry createEmptyEntry(LocalDate date, SecretKey key) {
    return WellnessEntry.emptyEntry(date, key);
  }

  @Override
  public SecretKey getKey(Cycle cycle) {
    return cycle.keys.wellnessKey;
  }
}
