package com.roamingroths.cmcc.logic;

import com.roamingroths.cmcc.crypto.Cipherable;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

/**
 * Created by parkeroth on 9/20/17.
 */

public abstract class Entry implements Cipherable {

  private final LocalDate mEntryDate;

  Entry(LocalDate entryDate) {
    mEntryDate = entryDate;
  }

  public LocalDate getDate() {
    return mEntryDate;
  }

  public String getDateStr() {
    return DateUtil.toWireStr(mEntryDate);
  }
}
