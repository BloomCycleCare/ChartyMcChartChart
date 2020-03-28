package com.bloomcyclecare.cmcc.ui.entry.detail;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.domain.ClarifyingQuestion;
import com.bloomcyclecare.cmcc.data.domain.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.domain.Observation;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.repos.ChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.CycleRepo;
import com.bloomcyclecare.cmcc.data.repos.PregnancyRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.bloomcyclecare.cmcc.utils.BoolMapping;
import com.bloomcyclecare.cmcc.utils.ErrorOr;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryDetailViewModel extends AndroidViewModel {

  final Subject<String> observationUpdates = BehaviorSubject.createDefault("");
  final Subject<Boolean> peakDayUpdates = BehaviorSubject.createDefault(false);
  final Subject<Boolean> intercourseUpdates = BehaviorSubject.createDefault(false);
  final Subject<Boolean> firstDayOfCycleUpdates = BehaviorSubject.createDefault(false);
  final Subject<Boolean> positivePregnancyTestUpdates = BehaviorSubject.createDefault(false);
  final Subject<Boolean> pointOfChangeUpdates = BehaviorSubject.createDefault(false);
  final Subject<IntercourseTimeOfDay> timeOfDayUpdates = BehaviorSubject.createDefault(IntercourseTimeOfDay.NONE);
  final Subject<String> noteUpdates = BehaviorSubject.createDefault("");
  final Subject<ClarifyingQuestionUpdate> clarifyingQuestionUpdates = PublishSubject.create();

  final Subject<BoolMapping> symptomUpdates = BehaviorSubject.createDefault(new BoolMapping());
  final Subject<BoolMapping> wellnessUpdates = BehaviorSubject.createDefault(new BoolMapping());

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();
  private final SingleSubject<CycleRenderer.EntryModificationContext> mEntryContext = SingleSubject.create();

  private final ChartEntryRepo mEntryRepo;
  private final CycleRepo mCycleRepo;
  private final PregnancyRepo mPregnancyRepo;

  public EntryDetailViewModel(@NonNull Application application) {
    super(application);

    MyApplication myApp = MyApplication.cast(application);
    mEntryRepo = myApp.entryRepo();
    mCycleRepo = myApp.cycleRepo();
    mPregnancyRepo = myApp.pregnancyRepo();

    mDisposables.add(intercourseUpdates.distinctUntilChanged().subscribe(value -> {
      if (!value) {
        timeOfDayUpdates.onNext(IntercourseTimeOfDay.NONE);
      }
    }));

    Flowable<ErrorOr<Observation>> errorOrObservationStream = observationUpdates
        .toFlowable(BackpressureStrategy.DROP)
        .distinctUntilChanged()
        .map(observationStr -> {
          try {
            return ErrorOr.forValue(ObservationParser.parse(observationStr).orNull());
          } catch (ObservationParser.InvalidObservationException ioe) {
            return ErrorOr.forError(ioe);
          }
        });

    Flowable<List<ClarifyingQuestionUpdate>> clarifyingQuestionUpdateList = RxUtil
        .aggregateLatest(clarifyingQuestionUpdates.toFlowable(BackpressureStrategy.BUFFER), u -> u.question);

    Flowable<ObservationEntry> observationEntryStream = mEntryContext
        .toFlowable().distinctUntilChanged()
        .map(context -> context.entry.observationEntry)
        .compose(RxUtil.update(errorOrObservationStream,
            (e, v) -> e.observation = v.or(null), "observation"))
        .compose(RxUtil.update(peakDayUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (e, v) -> e.peakDay = v, "peakDay"))
        .compose(RxUtil.update(intercourseUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (e, v) -> e.intercourse = v, "intercourse"))
        .compose(RxUtil.update(firstDayOfCycleUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (e, v) -> e.firstDay = v, "firstDay"))
        .compose(RxUtil.update(positivePregnancyTestUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (e, v) -> e.positivePregnancyTest = v, "positivePregnancyTest"))
        .compose(RxUtil.update(pointOfChangeUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (e, v) -> e.pointOfChange = v, "pointOfChange"))
        .compose(RxUtil.update(clarifyingQuestionUpdateList.distinctUntilChanged(),
            (e, v) -> {
              for (ClarifyingQuestionUpdate u : v) {
                e.updateClarifyingQuestion(u.question, u.answer);
              }
            }, "clarifyingQuestions"))
        .compose(RxUtil.update(timeOfDayUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (e, v) -> e.intercourseTimeOfDay = v, "timeOfDay"))
        .compose(RxUtil.update(noteUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (e, v) -> e.note = v, "notes"))
        ;

    Flowable<List<ClarifyingQuestionUpdate>> clarifyingQuestionRenderUpdates = Flowable.combineLatest(
        mEntryContext.toFlowable(), observationEntryStream, (entryContext, observationEntry) -> {
          ImmutableList.Builder<ClarifyingQuestionUpdate> builder = ImmutableList.builder();
          if (entryContext.shouldAskDoublePeakQuestions) {
            builder.add(new ClarifyingQuestionUpdate(
                ClarifyingQuestion.UNUSUAL_BUILDUP, observationEntry.unusualBuildup));
            builder.add(new ClarifyingQuestionUpdate(
                ClarifyingQuestion.UNUSUAL_STRESS, observationEntry.unusualStress));
          }
          if (entryContext.shouldAskEssentialSamenessIfMucus && observationEntry.hasMucus()) {
            builder.add(new ClarifyingQuestionUpdate(
                ClarifyingQuestion.ESSENTIAL_SAMENESS, observationEntry.isEssentiallyTheSame));
          }
          if (observationEntry.hasBlood() && !entryContext.allPreviousDaysHaveHadBlood) {
            builder.add(new ClarifyingQuestionUpdate(
                ClarifyingQuestion.UNUSUAL_BLEEDING, observationEntry.unusualBleeding));
          }
          return builder.build();
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
        clarifyingQuestionRenderUpdates,
        (entryContext, observationError, observationEntry, symptomEntry, wellnessEntry, clarifyingQuestionUpdates) -> {
          ViewState state = new ViewState(entryContext,
              new ChartEntry(entryContext.entry.entryDate, observationEntry, wellnessEntry, symptomEntry), observationError);

          state.clarifyingQuestionState.addAll(clarifyingQuestionUpdates);

          return state;
        })
        .toObservable()
        .subscribe(mViewStates);
  }

  private List<ClarifyingQuestionUpdate> getClarifyingQuestion(ViewState viewState) {
    ObservationEntry observationEntry = viewState.chartEntry.observationEntry;
    if (viewState.entryModificationContext.shouldAskDoublePeakQuestions) {
      return ImmutableList.of(
          new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_BUILDUP, observationEntry.unusualBuildup),
          new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_STRESS, observationEntry.unusualStress));
    }

    boolean shouldAskEssentialSameness = viewState.entryModificationContext.shouldAskEssentialSamenessIfMucus
        && observationEntry.hasMucus();

    if (shouldAskEssentialSameness
        && viewState.previousPrompts.contains(ClarifyingQuestion.ESSENTIAL_SAMENESS)
        && !observationEntry.isEssentiallyTheSame) {
      return ImmutableList.of(
          new ClarifyingQuestionUpdate(ClarifyingQuestion.POINT_OF_CHANGE, observationEntry.pointOfChange));
    }
    if (shouldAskEssentialSameness) {
      return ImmutableList.of(
          new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, observationEntry.isEssentiallyTheSame));
    }
    if (observationEntry.hasBlood() && !viewState.entryModificationContext.allPreviousDaysHaveHadBlood) {
      return ImmutableList.of(
          new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_BLEEDING, observationEntry.unusualBleeding));
    }
    return ImmutableList.of();
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
    positivePregnancyTestUpdates.onNext(context.entry.observationEntry.positivePregnancyTest);
    pointOfChangeUpdates.onNext(context.entry.observationEntry.pointOfChange);
    timeOfDayUpdates.onNext(context.entry.observationEntry.intercourseTimeOfDay);
    noteUpdates.onNext(Optional.fromNullable(context.entry.observationEntry.note).or(""));

    symptomUpdates.onNext(context.entry.symptomEntry.symptoms);
    wellnessUpdates.onNext(context.entry.wellnessEntry.wellnessItems);
  }

  LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates
        .toFlowable(BackpressureStrategy.DROP)
        .doOnNext(viewState -> Timber.d("Publishing new ViewState")));
  }

  Flowable<ViewState> viewStatesRx() {
    return mViewStates.toFlowable(BackpressureStrategy.BUFFER);
  }

  Single<Boolean> isDirty() {
    return mViewStates.firstOrError().map(ViewState::isDirty);
  }

  Maybe<List<String>> getSaveSummary(Function<ClarifyingQuestion, Single<Boolean>> questionResolver, Function<ValidationIssue, Single<Boolean>> issueResolver) {
    return mViewStates
        .firstOrError()
        .flatMapMaybe(viewState -> !viewState.isDirty() ? Maybe.empty() : Maybe.just(viewState))
        .flatMap(viewState -> resolveQuestions(viewState, questionResolver, clarifyingQuestionUpdates::onNext)
            .flatMapMaybe(updatedViewstate -> resolveValidationIssues(updatedViewstate, issueResolver)
                .andThen(Maybe.just(updatedViewstate))
                .onErrorComplete()
            .map(vs -> vs.chartEntry.getSummaryLines())));
  }

  Completable save(Function<ValidationIssue, Single<Boolean>> validationIssueResolver) {
    return mViewStates
        .firstOrError()
        .flatMapMaybe(viewState -> {
          if (!viewState.isDirty()) {
            return Maybe.empty();
          }
          return resolveValidationIssues(viewState, validationIssueResolver)
              .doOnComplete(() -> Timber.d("Entry validated"))
              .andThen(Maybe.just(viewState));
        })
        .observeOn(Schedulers.computation())
        .flatMapCompletable(this::updateRepos);
  }

  private Completable resolveValidationIssues(ViewState viewState, Function<ValidationIssue, Single<Boolean>> issueResolver) {
    if (viewState.validationIssues.isEmpty()) {
      return Completable.complete();
    }
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

  private Single<ViewState> resolveQuestions(ViewState viewState, Function<ClarifyingQuestion, Single<Boolean>> questionResolver, Consumer<ClarifyingQuestionUpdate> onResolve) {
    if (!viewState.promptForClarifyingQuestions()) {
      return Single.just(viewState);
    }
    List<ClarifyingQuestionUpdate> questions = getClarifyingQuestion(viewState);
    if (questions.isEmpty()) {
      return Single.just(viewState);
    }
    return Observable
        .fromIterable(questions)
        .map(s -> s.question)
        .filter(q -> !viewState.previousPrompts.contains(q))
        .flatMapSingle(question -> questionResolver
            .apply(question)
            .map(answer -> new ClarifyingQuestionUpdate(question, answer))
            .doOnSuccess(onResolve))
        .toList()
        .flatMap(answers -> {
          if (answers.isEmpty()) {
            return Single.just(viewState);
          }
          ViewState newState = new ViewState(viewState);
          for (ClarifyingQuestionUpdate update : answers) {
            newState.previousPrompts.add(update.question);
          }
          newState.clarifyingQuestionState.clear();
          newState.clarifyingQuestionState.addAll(answers);
          newState.applyQuestionUpdates();
          return resolveQuestions(newState, questionResolver, onResolve);
        });
  }

  private Completable updateRepos(ViewState viewState) {
    ChartEntry originalEntry = viewState.entryModificationContext.entry;
    viewState.applyQuestionUpdates();
    ChartEntry updatedEntry = viewState.chartEntry;
    if (updatedEntry.equals(originalEntry)) {
      Timber.w("Trying to save clean entry!");
      return Completable.complete();
    }
    List<Completable> actions = new ArrayList<>();
    Cycle currentCycle = viewState.entryModificationContext.cycle;
    if (updatedEntry.observationEntry.firstDay && !originalEntry.observationEntry.firstDay) {
      // Split for new cycle
      actions.add(mCycleRepo.splitCycle(currentCycle, updatedEntry.entryDate).ignoreElement());
    } else if (updatedEntry.observationEntry.positivePregnancyTest&& !originalEntry.observationEntry.positivePregnancyTest) {
      // Split for positive pregnancy test
      actions.add(mPregnancyRepo.startPregnancy(updatedEntry.entryDate));
    } else if (!updatedEntry.observationEntry.firstDay && originalEntry.observationEntry.firstDay) {
      // Join for new cycle
      actions.add(mCycleRepo.joinCycle(currentCycle, CycleRepo.JoinType.WITH_PREVIOUS).ignoreElement());
    } else if (!updatedEntry.observationEntry.positivePregnancyTest && originalEntry.observationEntry.positivePregnancyTest) {
      // Join for pregnancy test
      actions.add(mPregnancyRepo.revertPregnancy(updatedEntry.entryDate));
    }
    actions.add(mEntryRepo.insert(updatedEntry));
    return Completable.merge(actions);
  }

  static class ViewState {
    final CycleRenderer.EntryModificationContext entryModificationContext;
    final ChartEntry chartEntry;
    final String observationErrorText;
    final boolean isInPregnancy;
    final List<ClarifyingQuestionUpdate> clarifyingQuestionState = new ArrayList<>();
    final List<ValidationIssue> validationIssues = new ArrayList<>();
    final Set<ClarifyingQuestion> previousPrompts = new HashSet<>();

    ViewState(ViewState that) {
      this.entryModificationContext = that.entryModificationContext;
      this.chartEntry = that.chartEntry;
      this.observationErrorText = that.observationErrorText;
      this.clarifyingQuestionState.addAll(that.clarifyingQuestionState);
      this.validationIssues.addAll(that.validationIssues);
      this.previousPrompts.addAll(that.previousPrompts);
      this.isInPregnancy = that.isInPregnancy;
    }

    ViewState(CycleRenderer.EntryModificationContext entryModificationContext, ChartEntry chartEntry, String observationErrorText) {
      this.entryModificationContext = entryModificationContext;
      this.chartEntry = chartEntry;
      this.observationErrorText = observationErrorText;
      this.isInPregnancy = entryModificationContext.cycle.isPregnancy();
      validate();
    }

    private void validate() {
      ObservationEntry observationEntry = chartEntry.observationEntry;
      CycleRenderer.EntryModificationContext entryContext = entryModificationContext;

      boolean entryHasBlood = observationEntry.observation != null && observationEntry.observation.hasBlood();
      if (entryHasBlood && !observationEntry.unusualBleeding && !entryContext.allPreviousDaysHaveHadBlood && !observationEntry.firstDay) {
        validationIssues.add(ValidationIssue.confirm("Unusual bleeding?", "Are you sure this bleedin is typical?"));
      }
      boolean shouldBePocOrSame = entryContext.shouldAskEssentialSamenessIfMucus && observationEntry.hasMucus();
      boolean isPocOrSame = observationEntry.isEssentiallyTheSame || observationEntry.pointOfChange;
      boolean hasAskedPocAndSame = previousPrompts.contains(ClarifyingQuestion.ESSENTIAL_SAMENESS) && previousPrompts.contains(ClarifyingQuestion.POINT_OF_CHANGE);
      if (shouldBePocOrSame && !isPocOrSame && hasAskedPocAndSame) {
        validationIssues.add(ValidationIssue.confirm("Point of change?", "Are you sure this isn't essentially the same or a point of change?"));
      }
      if (!observationErrorText.isEmpty()) {
        validationIssues.add(ValidationIssue.block("Incomplete Observation", "Please complete or clear the observation before saving."));
      }
      if (observationEntry.intercourse && observationEntry.intercourseTimeOfDay == IntercourseTimeOfDay.NONE) {
        validationIssues.add(ValidationIssue.block("Missing Time of Day", "Please select a time of day for reporting intercourse."));
      }
    }

    boolean renderClarifyingQuestions() {
      return entryModificationContext.entry.observationEntry.observation != null;
    }

    boolean promptForClarifyingQuestions() {
      return !renderClarifyingQuestions();
    }

    boolean showPointOfChange() {
      return renderClarifyingQuestions()
          && entryModificationContext.shouldAskEssentialSamenessIfMucus
          && chartEntry.observationEntry.hasMucus();
    }

    boolean isDirty() {
      return !chartEntry.equals(entryModificationContext.entry);
    }

    private void applyQuestionUpdates() {
      for (ClarifyingQuestionUpdate update : clarifyingQuestionState) {
        switch (update.question) {
          case POINT_OF_CHANGE:
            chartEntry.observationEntry.pointOfChange = update.answer;
            break;
          case UNUSUAL_BLEEDING:
            chartEntry.observationEntry.unusualBleeding = update.answer;
            break;
          case UNUSUAL_STRESS:
            chartEntry.observationEntry.unusualStress = update.answer;
            break;
          case UNUSUAL_BUILDUP:
            chartEntry.observationEntry.unusualBuildup = update.answer;
            break;
          case ESSENTIAL_SAMENESS:
            chartEntry.observationEntry.isEssentiallyTheSame = update.answer;
            break;
          default:
            Timber.w("Fall through for ClarifyingQuestion!");
        }
      }
    }
  }

  enum ValidationAction {
    CONFIRM, BLOCK
  }

  public static class ValidationIssue {
    public final String summary;
    public final String details;
    public final ValidationAction action;

    private ValidationIssue(String summary, String details, ValidationAction action) {
      this.summary = summary;
      this.details = details;
      this.action = action;
    }

    public static ValidationIssue confirm(String summary, String details) {
      return new ValidationIssue(summary, details, ValidationAction.CONFIRM);
    }

    public static ValidationIssue block(String summary, String details) {
      return new ValidationIssue(summary, details, ValidationAction.BLOCK);
    }
  }
}
