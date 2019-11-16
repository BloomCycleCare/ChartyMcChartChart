package com.roamingroths.cmcc.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Created by parkeroth on 9/20/17.
 */
public abstract class Entry {

  @PrimaryKey
  @NonNull
  @ColumnInfo(name = "entryDate")
  public LocalDate mEntryDate;

  public Entry() {}

  Entry(LocalDate entryDate) {
    mEntryDate = entryDate;
  }

  public LocalDate getDate() {
    return mEntryDate;
  }

  public String getDateStr() {
    return DateUtil.toWireStr(mEntryDate);
  }

  public abstract List<String> getSummaryLines();
}
