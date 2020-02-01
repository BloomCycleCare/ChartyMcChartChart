package com.roamingroths.cmcc.logic.drive;

import com.google.common.collect.Range;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class SyncTrigger {
  public final DateTime triggerTime;
  public final Range<LocalDate> dateRange;

  public SyncTrigger(DateTime triggerTime, Range<LocalDate> dateRange) {
    this.triggerTime = triggerTime;
    this.dateRange = dateRange;
  }
}


