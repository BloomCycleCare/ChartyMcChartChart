package com.bloomcyclecare.cmcc.reminder;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class ReminderManager implements Disposable {

  private final Subject<DateTime> mTimeTriggers = PublishSubject.create();
  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final ReminderStore mReminderStore;

  ReminderManager() {
    mReminderStore = new DummyReminderStore();

    mDisposables.add(mTimeTriggers
        .doOnNext(t -> Timber.v("Got time trigger: %v", t))
        // Find the rules which are currently active and should be triggered
        .flatMap(time -> mReminderStore.getActiveRules()
            .doOnSuccess(rules -> Timber.v("Found %d active rules", rules.size()))
            // Get all he rules from the store
            .flatMapObservable(Observable::fromIterable)
            // Filter down to only the ones which are time triggered
            .filter(rule -> rule.localTime().isPresent())
            .doOnNext(r -> Timber.v("Processing rule #%d", r.reminderId))
            .flatMapSingle(rule -> mReminderStore.getLatestResult(rule.reminderId)
                .doOnSuccess(r -> Timber.v("Found previous result? %b", r.isPresent()))
                .map(result -> {
                  boolean shouldTrigger = result
                      .map(r -> r.triggerTime.isAfter(time))
                      .orElse(true);
                  if (shouldTrigger) {
                    return Optional.of(rule);
                  }
                  return Optional.<ReminderRule>empty();
                }))
            .filter(Optional::isPresent)
            .map(Optional::get))
        .doOnNext(r -> Timber.d("Triggering run for rule %d", r.reminderId))
        // Run the rule
        .subscribe(this::run));
  }

  public void trigger(DateTime value) {
    Timber.d("Got time trigger %v", value);
    mTimeTriggers.onNext(value);
  }

  private void run(ReminderRule rule) {
    Timber.d("Run succeeded for rule %d", rule.reminderId);
  }

  @Override
  public void dispose() {
    mDisposables.dispose();
  }

  @Override
  public boolean isDisposed() {
    return mDisposables.isDisposed();
  }

  private static class DummyReminderStore implements ReminderStore {

    private static final ImmutableList<ReminderRule> RULES = ImmutableList.of(
        new ReminderRule(1, null),
        new ReminderRule(2, DateTime.now().plusMinutes(5).toLocalTime()),
        new ReminderRule(3, DateTime.now().plusMinutes(10).toLocalTime()));

    @NotNull
    @Override
    public Single<List<ReminderRule>> getActiveRules() {
      return Single.just(RULES);
    }

    @NotNull
    @Override
    public Single<Optional<ReminderResult>> getLatestResult(long reminderId) {
      return Single.just(Optional.empty());
    }
  }
}
