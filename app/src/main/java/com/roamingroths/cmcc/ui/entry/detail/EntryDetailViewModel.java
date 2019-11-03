package com.roamingroths.cmcc.ui.entry.detail;

import android.app.Application;

import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.domain.IntercourseTimeOfDay;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.entities.SymptomEntry;
import com.roamingroths.cmcc.data.entities.WellnessEntry;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;
import com.roamingroths.cmcc.utils.BoolMapping;
import com.roamingroths.cmcc.utils.ErrorOr;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryDetailViewModel extends AndroidViewModel {

  final Subject<String> observationUpdates = BehaviorSubject.create();
  final Subject<Boolean> peakDayUpdates = BehaviorSubject.create();
  final Subject<Boolean> intercourseUpdates = BehaviorSubject.create();
  final Subject<Boolean> firstDayOfCycleUpdates = BehaviorSubject.create();
  final Subject<Boolean> pointOfChangeUpdates = BehaviorSubject.create();
  final Subject<Boolean> unusualBleedingUpdates = BehaviorSubject.create();
  final Subject<Boolean> isEssentiallyTheSameUpdates = BehaviorSubject.create();
  final Subject<IntercourseTimeOfDay> timeOfDayUpdates = BehaviorSubject.create();

  final Subject<BoolMapping> symptomUpdates = BehaviorSubject.create();
  final Subject<BoolMapping> wellnessUpdates = BehaviorSubject.create();

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();
  private final SingleSubject<CycleRenderer.EntryModificationContext> mEntryContext = SingleSubject.create();

  private final ChartEntryRepo mEntryRepo;
  private final CycleRepo mCycleRepo;

  public EntryDetailViewModel(@NonNull Application application) {
    super(application);

    MyApplication myApp = MyApplication.cast(application);
    mEntryRepo = new ChartEntryRepo(myApp.db());
    mCycleRepo = new CycleRepo(myApp.db());

    mDisposables.add(intercourseUpdates.subscribe(value -> {
      if (value) {
        timeOfDayUpdates.onNext(IntercourseTimeOfDay.ANY);
      } else {
        timeOfDayUpdates.onNext(IntercourseTimeOfDay.NONE);
      }
    }));

    Flowable<ErrorOr<Observation>> errorOrObservationStream = observationUpdates
        .toFlowable(BackpressureStrategy.DROP)
        .distinctUntilChanged()
        .map(observationStr -> {
          try {
            return ErrorOr.forValue(Observation.fromString(observationStr));
          } catch (Observation.InvalidObservationException ioe) {
            return ErrorOr.forError(ioe);
          }
        });

    Flowable<ObservationEntry> observationEntryStream = Flowable.combineLatest(
        mEntryContext.toFlowable()
            .distinctUntilChanged()
            .map(context -> context.entry.entryDate)
            .doOnNext(i -> Timber.v("New EntryRenderContext update")),
        errorOrObservationStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New errorOrObservation update")),
        peakDayUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New peakDay update")),
        intercourseUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New intercourse update")),
        firstDayOfCycleUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New firstDay update")),
        pointOfChangeUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New pointOfChange update")),
        unusualBleedingUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New unusualBleeding update")),
        isEssentiallyTheSameUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New essentialSameness update")),
        timeOfDayUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New timeOfDay update")),
        (entryDate, errorOrObservation, isPeakDay, hasHadIntercourse, isFirstDayOfCycle, isPointOfChange, isUnusualBleeding, isEssentaillyTheSame, timeOfDay) -> {
          Observation observation = null;
          if (!errorOrObservation.hasError()) {
            observation = errorOrObservation.get();
          }
          return new ObservationEntry(
              entryDate, observation, isPeakDay, hasHadIntercourse, isFirstDayOfCycle,
              isPointOfChange, isUnusualBleeding, timeOfDay, isEssentaillyTheSame);
        });

    Flowable<SymptomEntry> symptomEntryStream = Flowable.combineLatest(
        mEntryContext.toFlowable()
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New EntryRenderContext update")),
        symptomUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New symptom udpates")),
        (entryContext, activeSymptoms) -> new SymptomEntry(entryContext.entry.entryDate, activeSymptoms));

    Flowable<WellnessEntry> wellnessEntryStream = Flowable.combineLatest(
        mEntryContext.toFlowable()
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New EntryRenderContext update")),
        wellnessUpdates.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New wellness udpates")),
        (entryContext, activeItems) -> new WellnessEntry(entryContext.entry.entryDate, activeItems));

    Flowable.combineLatest(
        mEntryContext.toFlowable()
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New EntryRenderContext update")),
        errorOrObservationStream
            .map(errorOrObservation -> !errorOrObservation.hasError() ? "" : errorOrObservation.error().getMessage())
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New observation error")),
        observationEntryStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New observation entry")),
        symptomEntryStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New symptom entry")),
        wellnessEntryStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.v("New wellness entry")),
        (entryContext, observationError, observationEntry, symptomEntry, wellnessEntry) -> {
          ViewState state = new ViewState(entryContext,
              new ChartEntry(entryContext.entry.entryDate, observationEntry, wellnessEntry, symptomEntry), observationError);

          boolean entryHasBlood = observationEntry.observation != null && observationEntry.observation.hasBlood();
          if (entryHasBlood && !observationEntry.unusualBleeding && entryContext.expectUnusualBleeding && !observationEntry.firstDay) {
            state.validationIssues.add(new ValidationIssue("Unusual bleeding?", "Are you sure this bleedin is typical?"));
          }
          if (entryContext.shouldAskEssentialSameness && !observationEntry.isEssentiallyTheSame && !observationEntry.pointOfChange) {
            state.validationIssues.add(new ValidationIssue("Point of change?", "Are you sure this isn't essentially the same or a point of change?"));
          }

          return state;
        })
        .toObservable()
        .subscribe(mViewStates);
  }

  void initialize(CycleRenderer.EntryModificationContext context) {
    if (mEntryContext.hasValue()) {
      Timber.w("Reinitializing EntryRenderContext!");
    }
    mEntryContext.onSuccess(context);
    if (context.entry.observationEntry.observation != null) {
      observationUpdates.onNext(context.entry.observationEntry.observation.toString());
    } else {
      observationUpdates.onNext("");
    }
    peakDayUpdates.onNext(context.entry.observationEntry.peakDay);
    intercourseUpdates.onNext(context.entry.observationEntry.intercourse);
    firstDayOfCycleUpdates.onNext(context.entry.observationEntry.firstDay);
    unusualBleedingUpdates.onNext(context.entry.observationEntry.unusualBleeding);
    pointOfChangeUpdates.onNext(context.entry.observationEntry.pointOfChange);
    isEssentiallyTheSameUpdates.onNext(context.entry.observationEntry.isEssentiallyTheSame);
    timeOfDayUpdates.onNext(context.entry.observationEntry.intercourseTimeOfDay);

    symptomUpdates.onNext(context.entry.symptomEntry.symptoms);
    wellnessUpdates.onNext(context.entry.wellnessEntry.wellnessItems);
  }

  LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates
        .toFlowable(BackpressureStrategy.DROP)
        .doOnNext(viewState -> Timber.d("Publishing new ViewState")));
  }

  Single<Boolean> isDirty() {
    return mViewStates.firstOrError().map(ViewState::isDirty);
  }

  Maybe<List<String>> getSaveSummary() {
    return mViewStates
        .firstOrError()
        .flatMapMaybe(viewState -> !viewState.isDirty()
            ? Maybe.empty() : Maybe.just(viewState.chartEntry.getSummaryLines()));
  }

  Completable save(Function<ValidationIssue, Single<Boolean>> validationIssueResolver) {
    return mViewStates
        .firstOrError()
        .flatMapCompletable(viewState -> {
          if (!viewState.isDirty()) {
            return Completable.complete();
          }
          return validateEntry(viewState, validationIssueResolver)
              .observeOn(Schedulers.computation())
              .andThen(updateRepos(viewState));
        });
  }

  private Completable validateEntry(ViewState viewState, Function<ValidationIssue, Single<Boolean>> issueResolver) {
    return Observable
        .fromIterable(viewState.validationIssues)
        .flatMapCompletable(issue -> issueResolver
            .apply(issue)
            .flatMapCompletable(proceed -> {
              if (!proceed) {
                throw new Exception("FOO");
              }
              return Completable.complete();
            }));
  }

  private Completable updateRepos(ViewState viewState) {
    ChartEntry originalEntry = viewState.entryModificationContext.entry;
    ChartEntry updatedEntry = viewState.chartEntry;
    if (updatedEntry.equals(originalEntry)) {
      Timber.w("Trying to save clean entry!");
      return Completable.complete();
    }
    List<Completable> actions = new ArrayList<>();
    if (!originalEntry.observationEntry.firstDay && updatedEntry.observationEntry.firstDay) {
      // We need to split the current cycle...
      Cycle currentCycle = viewState.entryModificationContext.cycle;
      Cycle newCycle = new Cycle("asdf", updatedEntry.entryDate, currentCycle.endDate);
      actions.add(mCycleRepo.insertOrUpdate(newCycle));

      currentCycle.endDate = updatedEntry.entryDate.minusDays(1);
      actions.add(mCycleRepo.insertOrUpdate(currentCycle));
    }
    if (originalEntry.observationEntry.firstDay && !updatedEntry.observationEntry.firstDay) {
      // We need to join the current cycle with the previous...
      if (!viewState.entryModificationContext.hasPreviousCycle) {
        throw new IllegalStateException("No previous cycle to join");
      }
      Cycle currentCycle = viewState.entryModificationContext.cycle;
      actions.add(mCycleRepo
          .getPreviousCycle(currentCycle)
          .toSingle()
          .flatMapCompletable(previousCycle -> {
            previousCycle.endDate = currentCycle.endDate;
            return mCycleRepo.insertOrUpdate(previousCycle).andThen(mCycleRepo.delete(currentCycle));
          }));
    }
    actions.add(mEntryRepo.insert(updatedEntry));
    return Completable.merge(actions);
  }

  public static class ViewState {
    public final CycleRenderer.EntryModificationContext entryModificationContext;
    public final ChartEntry chartEntry;
    public final String observationErrorText;
    public final List<ValidationIssue> validationIssues = new ArrayList<>();

    public ViewState(CycleRenderer.EntryModificationContext entryModificationContext, ChartEntry chartEntry, String observationErrorText) {
      this.entryModificationContext = entryModificationContext;
      this.chartEntry = chartEntry;
      this.observationErrorText = observationErrorText;
    }

    public boolean isDirty() {
      return !chartEntry.equals(entryModificationContext.entry);
    }
  }

  public static class ValidationIssue {
    public final String summary;
    public final String details;

    public ValidationIssue(String summary, String details) {
      this.summary = summary;
      this.details = details;
    }
  }
}
