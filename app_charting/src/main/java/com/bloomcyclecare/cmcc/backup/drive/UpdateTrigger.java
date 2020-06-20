package com.bloomcyclecare.cmcc.backup.drive;

import com.google.common.collect.Range;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class UpdateTrigger {
  public final DateTime triggerTime;
  public final Range<LocalDate> dateRange;

  public UpdateTrigger(DateTime triggerTime, Range<LocalDate> dateRange) {
    this.triggerTime = triggerTime;
    this.dateRange = dateRange;
  }
}


