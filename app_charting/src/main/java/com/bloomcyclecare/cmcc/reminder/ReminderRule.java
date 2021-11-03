package com.bloomcyclecare.cmcc.reminder;

import androidx.annotation.Nullable;

import org.joda.time.LocalTime;

import java.util.Optional;

public class ReminderRule {
  public final long reminderId;
  @Nullable
  public final LocalTime localTime;

  public ReminderRule(int reminderId, @Nullable LocalTime localTime) {
    this.reminderId = reminderId;
    this.localTime = localTime;
  }

  public Optional<LocalTime> localTime() {
    return Optional.ofNullable(localTime);
  }
}
