package com.roamingroths.cmcc.logic.drive;

import android.content.Context;

import com.google.common.collect.Range;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;


public class SyncWorker extends Worker {

  private enum Params {
    START_DATE,
    END_DATE
  }

  public static Data createInputData(Range<LocalDate> dateRange) {
    if (!dateRange.hasUpperBound() || !dateRange.hasLowerBound()) {
      throw new IllegalArgumentException();
    }
    return new Data.Builder()
        .putString(Params.START_DATE.name(), DateUtil.toWireStr(dateRange.lowerEndpoint()))
        .putString(Params.END_DATE.name(), DateUtil.toWireStr(dateRange.upperEndpoint()))
        .build();
  }

  public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @NonNull
  @Override
  public Result doWork() {
    Timber.i("Doing work from %s to %s",
        getInputData().getString(Params.START_DATE.name()),
        getInputData().getString(Params.END_DATE.name()));
    return Result.success();
  }
}
