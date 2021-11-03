package com.bloomcyclecare.cmcc.reminder;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Optional;

import io.reactivex.Single;

public interface ReminderStore {

  @NonNull
  Single<List<ReminderRule>> getActiveRules();

  @NonNull
  Single<Optional<ReminderResult>> getLatestResult(long reminderId);
}
