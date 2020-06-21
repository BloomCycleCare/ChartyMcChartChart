package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

/**
 * Created by parkeroth on 9/20/17.
 */
public abstract class Entry {

  @PrimaryKey
  @NonNull
  @ColumnInfo(name = "entryDate")
  public LocalDate mEntryDate;

  public Entry() {}

  protected Entry(LocalDate entryDate) {
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
