package com.bloomcyclecare.cmcc.ui.pregnancy.detail;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.logic.breastfeeding.BreastfeedingStats;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class PregnancyDetailViewModel extends AndroidViewModel {

  private final BreastfeedingStats mStats;
  private final RWPregnancyRepo mPregnancyRepo;
  private final SingleSubject<Pregnancy> mPregnancy = SingleSubject.create();
  private final Subject<Optional<LocalDate>> mDueDateUpdates = BehaviorSubject.create();
  private final Subject<Optional<LocalDate>> mDeliveryDateUpdates = BehaviorSubject.create();
  private final Subject<Optional<LocalDate>> mBreastfeedingStartDateUpdates = BehaviorSubject.create();
  private final Subject<Optional<LocalDate>> mBreastfeedingEndDateUpdates = BehaviorSubject.create();
  private final Subject<ViewState> mState = BehaviorSubject.create();
  private final Subject<Boolean> mBreastfeedingUpdates = BehaviorSubject.create();
  private final Subject<Optional<String>> mBabyNameUpdates = BehaviorSubject.create();

  public PregnancyDetailViewModel(@NonNull Application application, Pregnancy pregnancy) {
    super(application);
    mPregnancyRepo = ChartingApp.cast(application).pregnancyRepo();
    mStats = new BreastfeedingStats(null, ChartingApp.cast(application).entryRepo(ViewMode.CHARTING), mPregnancyRepo);

    Timber.v("Initializing with %s", pregnancy);
    mDueDateUpdates.onNext(Optional.ofNullable(pregnancy.dueDate));
    mDeliveryDateUpdates.onNext(Optional.ofNullable(pregnancy.deliveryDate));
    onBreastfeedingToggle(pregnancy.breastfeedingStartDate != null);
    mBreastfeedingStartDateUpdates.onNext(Optional.ofNullable(pregnancy.breastfeedingStartDate));
    mBreastfeedingEndDateUpdates.onNext(Optional.ofNullable(pregnancy.breastfeedingEndDate));
    mBabyNameUpdates.onNext(Optional.ofNullable(pregnancy.babyDaybookName));
    mPregnancy.onSuccess(pregnancy);

    clearOnDisabled(mBreastfeedingUpdates, mBreastfeedingStartDateUpdates);
    clearOnDisabled(mBreastfeedingUpdates, mBreastfeedingEndDateUpdates);
    clearOnDisabled(mBreastfeedingUpdates, mBabyNameUpdates);

    stateStream().subscribe(mState);
  }

  private static <T> void clearOnDisabled(Subject<Boolean> switchSubject, Subject<Optional<T>> target) {
    switchSubject
        .filter(v -> !v)
        .map(v -> Optional.<T>empty())
        .doOnNext(v -> Timber.v("Clearing on disable"))
        .subscribe(target);
  }

  void onBreastfeedingToggle(boolean value) {
    mBreastfeedingUpdates.onNext(value);
  }

  void onNewDueDate(@Nullable LocalDate date) {
    mDueDateUpdates.onNext(Optional.ofNullable(date));
  }

  void onNewDeliveryDate(@Nullable LocalDate date) {
    mDeliveryDateUpdates.onNext(Optional.ofNullable(date));
  }

  void onNewBreastfeedingStartDate(@Nullable LocalDate date) {
    mBreastfeedingStartDateUpdates.onNext(Optional.ofNullable(date));
  }

  void onNewBreastfeedingEndDate(@Nullable LocalDate date) {
    mBreastfeedingEndDateUpdates.onNext(Optional.ofNullable(date));
  }

  void onBabyNameUpdate(String name) {
    mBabyNameUpdates.onNext(Strings.isNullOrEmpty(name) ? Optional.empty() : Optional.of(name));
  }

  Completable onSave() {
    return mState.firstElement()
        .toSingle()
        .flatMapCompletable(viewState -> mPregnancyRepo.update(viewState.pregnancy));
  }

  Maybe<ViewState> currentState() {
    return mState.firstElement();
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mState.toFlowable(BackpressureStrategy.BUFFER));
  }

  private Observable<ViewState> stateStream() {
    return Flowable.combineLatest(
        mPregnancy.toFlowable().distinctUntilChanged(),
        mDueDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mDeliveryDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mBreastfeedingUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mBreastfeedingStartDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mBreastfeedingEndDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mBabyNameUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mPregnancy.toFlowable().distinctUntilChanged()
            .flatMapSingle(pregnancy -> {
              if (Strings.isNullOrEmpty(pregnancy.babyDaybookName)) {
                return Single.just(ImmutableList.<String>of());
              }
              // TODO: this should be hot
              return mStats.dailyStatsFromRepo(pregnancy.babyDaybookName)
                  .map(m -> BreastfeedingStats.aggregate(m.values()))
                  .map(aggregateStats -> {
                    ImmutableList.Builder<String> lines = ImmutableList.builder();
                    lines.add(String.format("Number of day feedings: %.2f±%.2f", aggregateStats.nDayMean, aggregateStats.nDayInterval));
                    lines.add(String.format("Number of night feedings: %.2f±%.2f", aggregateStats.nNightMean, aggregateStats.nNightInterval));

                    lines.add(String.format("Max gap between feedings (median): %.2f",aggregateStats.maxGapMedian));
                    lines.add(String.format("Max gap between feedings (p95): %.2f", aggregateStats.maxGapP95));

                    lines.add("Longest gap on: " + aggregateStats.longestGapDate);
                    return lines.build();
                  });
            }),
        (pregnancy, dueDate, deliveryDate, breastfeedingSwitchValue, breastfeedingStart, breastfeedingEnd, babyName, stats) -> {
          Pregnancy updatedPregnancy = pregnancy.copy();
          updatedPregnancy.dueDate = dueDate.orElse(null);
          updatedPregnancy.deliveryDate = deliveryDate.orElse(null);
          updatedPregnancy.breastfeedingStartDate = breastfeedingStart.orElse(null);
          updatedPregnancy.breastfeedingEndDate = breastfeedingEnd.orElse(null);
          updatedPregnancy.babyDaybookName = babyName.orElse(null);
          return new ViewState(updatedPregnancy, breastfeedingSwitchValue, Joiner.on("\n").join(stats));
        })
        .toObservable();
  }

  public static class ViewState {

    public final Pregnancy pregnancy;
    public final boolean showBreastfeedingSection;
    public final boolean showBreastfeedingStartDate;
    public final boolean showBreastfeedingEndDate;
    public final String stats;

    private ViewState(Pregnancy pregnancy, boolean breastfeedingToggleValue, String stats) {
      this.pregnancy = pregnancy;
      this.showBreastfeedingSection = pregnancy.deliveryDate != null;
      this.showBreastfeedingStartDate = showBreastfeedingSection && breastfeedingToggleValue;
      this.showBreastfeedingEndDate = showBreastfeedingStartDate && pregnancy.breastfeedingStartDate != null;
      this.stats = stats;
    }
  }

  static class Factory implements ViewModelProvider.Factory {

    private final @NonNull Application application;
    private final @NonNull Pregnancy pregnancy;

    public Factory(@NonNull Application application, @NonNull Pregnancy pregnancy) {
      this.application = application;
      this.pregnancy = pregnancy;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new PregnancyDetailViewModel(application, pregnancy);
    }
  }
}
