package com.bloomcyclecare.cmcc.reminder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

class TimeTrigger extends Observable<DateTime> {

  private final Subject<DateTime> mSubject = PublishSubject.create();

  TimeTrigger(Context context, int intervalMinutes) {
    Timber.v("Scheduling work request with a period of %d minutes", intervalMinutes);
    WorkManager.initialize();
    workManager.enqueueUniquePeriodicWork(
        TimeTrigger.class.getCanonicalName(),
        ExistingPeriodicWorkPolicy.REPLACE,
        new PeriodicWorkRequest.Builder(
            TimeTriggerWorker.class,
            intervalMinutes, TimeUnit.MINUTES)
            .build());
  }

  void trigger(DateTime time) {
    mSubject.onNext(time);
  }

  @Override
  protected void subscribeActual(Observer<? super DateTime> observer) {
    mSubject.subscribe(observer);
  }

  public static class TimeTriggerWorker extends ListenableWorker {
    public TimeTriggerWorker(Context appContext, WorkerParameters workerParams) {
      super(appContext, workerParams);
    }

    @Override
    @NonNull
    public ListenableFuture<Result> startWork() {
      trigger(DateTime.now());
      return Futures.immediateFuture(Result.success());
    }
  }
}
