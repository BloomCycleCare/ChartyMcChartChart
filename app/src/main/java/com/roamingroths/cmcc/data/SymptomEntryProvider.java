package com.roamingroths.cmcc.data;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.SymptomEntry;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/2/17.
 */

public class SymptomEntryProvider extends EntryProvider<SymptomEntry> {

  public static SymptomEntryProvider forDb(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    return new SymptomEntryProvider(db, cryptoUtil);
  }

  private SymptomEntryProvider(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    super(db, cryptoUtil, ChildId.SYMPTOM, SymptomEntry.class);
  }

  @Override
  SymptomEntry createEmptyEntry(LocalDate date, SecretKey key) {
    return SymptomEntry.emptyEntry(date, key);
  }

  @Override
  public SecretKey getKey(Cycle cycle) {
    return cycle.keys.symptomKey;
  }
}
