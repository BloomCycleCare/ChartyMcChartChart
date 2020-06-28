package com.bloomcyclecare.cmcc.backup.drive;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public interface WorkerManager {

  Optional<Observable<ItemStats>> register(Item item, Observable<OneTimeWorkRequest> workStream);

  Optional<Observable<ItemStats>> getUpdateStream(Item item);

  boolean cancel(Item item);

  enum Item {
    PUBLISH, BACKUP
  }

  interface Provider {
    WorkerManager getWorkerManager();
  }

  static WorkerManager fromApp(Application application) {
    if (application instanceof Provider) {
      return ((Provider) application).getWorkerManager();
    }
    RuntimeException e = new IllegalStateException();
    Timber.wtf(e);
    throw e;
  }

  @AutoValue
  abstract class ItemStats {
    public abstract Integer numEncueuedRequests();
    public abstract Integer numCompletedRequests();
    public abstract Long lastCompletedTime();
    public abstract Long lastEncueueTime();

    public static ItemStats create(Integer numEncueuedRequests, Integer numCompletedRequests, Long lastCompletedTime, Long lastEncueueTime) {
      return new AutoValue_WorkerManager_ItemStats(numEncueuedRequests, numCompletedRequests, lastCompletedTime, lastEncueueTime);
    }

  }

  static WorkerManager create(Context context) {
    return new Impl(
        WorkManager.getInstance(context),
        context.getSharedPreferences(WorkerManager.class.getCanonicalName(), Context.MODE_PRIVATE));
  }

  class Impl implements WorkerManager, Disposable {

    private final WorkManager mWorkManager;
    private final SharedPreferences mSharedPreferences;
    private final Map<Item, Registration> mRegistrations = new ConcurrentHashMap<>();

    public Impl(WorkManager workManager, SharedPreferences sharedPreferences) {
      mWorkManager = workManager;
      mSharedPreferences = sharedPreferences;
    }

    @Override
    public Optional<Observable<ItemStats>> register(Item item, Observable<OneTimeWorkRequest> workStream) {
      Timber.d("Registering work stream for item: %s", item.name());
      if (mRegistrations.containsKey(item)) {
        Timber.w("Work stream already registered for items: %s", item.name());
        return Optional.empty();
      }
      Subject<ItemStats> statsSubject = PublishSubject.create();
      Set<Runnable> cleanupTasks = Sets.newConcurrentHashSet();
      AtomicInteger numEncueuedReqeusts = new AtomicInteger();
      AtomicInteger numCompletedRequests = new AtomicInteger();
      AtomicLong lastEncueueTimeMs = new AtomicLong();
      AtomicLong lastCompletedTimeMs = new AtomicLong();
      Disposable d = workStream
          .doOnNext(r -> Timber.d("New request for item: %s", item.name()))
          .subscribe(request -> {
            mWorkManager.enqueue(request);
            numEncueuedReqeusts.incrementAndGet();
            lastEncueueTimeMs.set(System.currentTimeMillis());
            LiveData<WorkInfo> ld = mWorkManager.getWorkInfoByIdLiveData(request.getId());
            Observer<WorkInfo> o = workInfo -> {
              Timber.v("Publishing WorkInfo for item: %s", item.name());
              if (workInfo.getState().isFinished()) {
                numCompletedRequests.incrementAndGet();
                lastCompletedTimeMs.set(System.currentTimeMillis());
              }
              statsSubject.onNext(ItemStats.create(numCompletedRequests.get(), numCompletedRequests.get(), lastCompletedTimeMs.get(), lastEncueueTimeMs.get()));
            };
            cleanupTasks.add(() -> {
              ld.removeObserver(o);
            });
            ld.observeForever(o);
          }, t -> Timber.e(t, "Error in work stream for itesm: %s", item.name()));
      mRegistrations.put(item, Registration.create(statsSubject, cleanupTasks, d));
      return Optional.of(statsSubject);
    }

    @Override
    public Optional<Observable<ItemStats>> getUpdateStream(Item item) {
      return Optional.ofNullable(mRegistrations.get(item)).map(Registration::statsSubject);
    }

    @Override
    public boolean cancel(Item item) {
      Registration r = mRegistrations.remove(item);
      if (r == null) {
        Timber.w("No registration to cancel for item: %s", item.name());
        return false;
      }
      r.disposable().dispose();
      for (Runnable t : r.cleanupTasks()) {
        Timber.v("Running cleanup task for item: %s", item.name());
        t.run();
      }
      return true;
    }

    @Override
    public void dispose() {
      for (Registration r : mRegistrations.values()) {
        r.disposable().dispose();
      }
    }

    @Override
    public boolean isDisposed() {
      boolean hasActiveDisposable = false;
      for (Registration r : mRegistrations.values()) {
        if (!r.disposable().isDisposed()) {
          hasActiveDisposable = true;
          break;
        }
      }
      return !hasActiveDisposable;
    }

    @AutoValue
    abstract static class Registration {

      abstract Observable<ItemStats> statsSubject();
      abstract Set<Runnable> cleanupTasks();
      abstract Disposable disposable();

      public static Registration create(Observable<ItemStats> statsSubject, Set<Runnable> cleanupTasks, Disposable disposable) {
        return new AutoValue_WorkerManager_Impl_Registration(statsSubject, cleanupTasks, disposable);
      }

    }
  }
}
