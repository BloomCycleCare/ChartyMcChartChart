package com.bloomcyclecare.cmcc.reminder;

import androidx.annotation.Nullable;

import org.joda.time.DateTime;

public class ReminderResult {
  public final DateTime triggerTime;
  @Nullable
  public final DateTime resolutionTime;

  public ReminderResult(DateTime triggerTime, @Nullable DateTime resolutionTime) {
    this.triggerTime = triggerTime;
    this.resolutionTime = resolutionTime;
  }
}
