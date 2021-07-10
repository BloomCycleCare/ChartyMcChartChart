package com.bloomcyclecare.cmcc.ui.entry;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.observation.ClarifyingQuestion;
import com.bloomcyclecare.cmcc.data.models.observation.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.observation.SymptomEntry;
import com.bloomcyclecare.cmcc.data.models.observation.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.bloomcyclecare.cmcc.ui.entry.observation.ClarifyingQuestionUpdate;
import com.bloomcyclecare.cmcc.utils.BoolMapping;
import com.bloomcyclecare.cmcc.utils.Copyable;
import com.bloomcyclecare.cmcc.utils.ErrorOr;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryDetailViewModel extends AndroidViewModel {

  public final Subject<String> observationUpdates = BehaviorSubject.createDefault("");
  public final Subject<Boolean> peakDayUpdates = BehaviorSubject.createDefault(false);
  public final Subject<Boolean> intercourseUpdates = BehaviorSubject.createDefault(false);
  public final Subject<Boolean> uncertainUpdates = BehaviorSubject.createDefault(false);
  public final Subject<Boolean> firstDayOfCycleUpdates = BehaviorSubject.createDefault(false);
  public final Subject<Boolean> positivePregnancyTestUpdates = BehaviorSubject.createDefault(false);
  public final Subject<Boolean> pointOfChangeUpdates = BehaviorSubject.createDefault(false);
  public final Subject<IntercourseTimeOfDay> timeOfDayUpdates = BehaviorSubject.createDefault(IntercourseTimeOfDay.NONE);
  public final Subject<String> noteUpdates = BehaviorSubject.createDefault("");
  public final Subject<ClarifyingQuestionUpdate> clarifyingQuestionUpdates = PublishSubject.create();

  public final Subject<BoolMapping> symptomUpdates = BehaviorSubject.createDefault(new BoolMapping());
  public final Subject<BoolMapping> wellnessUpdates = BehaviorSubject.createDefault(new BoolMapping());

  public final Subject<MeasurementEntry> measurementEntries = BehaviorSubject.create();
  public final Subject<BreastfeedingEntry> breastfeedingEntrySubject = BehaviorSubject.create();

  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  private final SingleSubject<Boolean> shouldShowMeasurementPage = SingleSubject.create();
  private final SingleSubject<Boolean> shouldShowBreastfeedingPage = SingleSubject.create();

  private final RWChartEntryRepo mEntryRepo;
  private final RWCycleRepo mCycleRepo;
  private final RWPregnancyRepo mPregnancyRepo;

  public EntryDetailViewModel(
      @NonNull Application application, CycleRenderer.EntryModificationContext context) {
    super(application);
    ChartingApp myApp = ChartingApp.cast(application);
    mEntryRepo = myApp.entryRepo(ViewMode.CHARTING);
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);
    mPregnancyRepo = myApp.pregnancyRepo(ViewMode.CHARTING);

    LocalDate entryDate = context.entry.entryDate;
    if (context.entry.observationEntry.observation != null) {
      observationUpdates.onNext(context.entry.observationEntry.observation.toString());
    } else {
      observationUpdates.onNext("");
    }
    peakDayUpdates.onNext(context.entry.observationEntry.peakDay);
    intercourseUpdates.onNext(context.entry.observationEntry.intercourse);
    uncertainUpdates.onNext(context.entry.observationEntry.uncertain);
    firstDayOfCycleUpdates.onNext(context.entry.observationEntry.firstDay);
    positivePregnancyTestUpdates.onNext(context.entry.observationEntry.positivePregnancyTest);
    pointOfChangeUpdates.onNext(context.entry.observationEntry.pointOfChange);
    timeOfDayUpdates.onNext(Optional.ofNullable(context.entry.observationEntry.intercourseTimeOfDay).orElse(IntercourseTimeOfDay.NONE));
    noteUpdates.onNext(Optional.ofNullable(context.entry.observationEntry.note).orElse(""));

    symptomUpdates.onNext(context.entry.symptomEntry.symptoms);
    wellnessUpdates.onNext(context.entry.wellnessEntry.wellnessItems);
    measurementEntries.onNext(context.entry.measurementEntry);
    breastfeedingEntrySubject.onNext(context.entry.breastfeedingEntry);

    Observable.combineLatest(
        intercourseUpdates,
        timeOfDayUpdates,
        (intercourse, timeOfDay) -> !intercourse && timeOfDay != IntercourseTimeOfDay.NONE)
        .filter(v -> v)
        .doOnNext(i -> Timber.v("Resetting intercourse time of day"))
        .map(v -> IntercourseTimeOfDay.NONE)
        .subscribe(timeOfDayUpdates);

    // Hook up when to show measurement page
    myApp.preferenceRepo().summaries().firstOrError()
        .map(summary -> summary.lhTestMeasurementEnabled() || summary.clearblueMachineMeasurementEnabled())
        .map(enabledInSettings -> enabledInSettings || !context.entry.measurementEntry.isEmpty())
        .subscribe(shouldShowMeasurementPage);

    // Hook up when to show breastfeeding page
    myApp.pregnancyRepo(ViewMode.CHARTING)
        .getAll()
        .firstOrError()
        .map(pregnancies -> {
          Pregnancy mostRecentPregnancy = null;
          for (Pregnancy p : pregnancies) {
            if (p.positiveTestDate.isAfter(entryDate)) {
              continue;
            }
            if (mostRecentPregnancy == null || p.positiveTestDate.isAfter(mostRecentPregnancy.positiveTestDate)) {
              mostRecentPregnancy = p;
            }
          }
          return Optional.ofNullable(mostRecentPregnancy);
        })
        .map(pregnancy -> {
          if (!pregnancy.isPresent()) {
            return false;
          }
          Timber.v("Found pregnancy %s", pregnancy.get());
          if (pregnancy.get().breastfeedingStartDate == null || entryDate.isBefore(pregnancy.get().breastfeedingStartDate)) {
            return false;
          }
          return pregnancy.get().breastfeedingEndDate == null || !entryDate.isAfter(pregnancy.get().breastfeedingEndDate);
        })
        .subscribe(shouldShowBreastfeedingPage);

    Flowable<ErrorOr<Observation>> errorOrObservationStream = observationUpdates
        .toFlowable(BackpressureStrategy.DROP)
        .distinctUntilChanged()
        .map(observationStr -> {
          try {
            return ErrorOr.forValue(ObservationParser.parse(observationStr).orElse(null));
          } catch (ObservationParser.InvalidObservationException ioe) {
            return ErrorOr.forError(ioe);
          }
        });

    Flowable<List<ClarifyingQuestionUpdate>> clarifyingQuestionUpdateList = RxUtil
        .aggregateLatest(clarifyingQuestionUpdates.toFlowable(BackpressureStrategy.BUFFER), u -> u.question);

    Flowable<ObservationEntry> observationEntryStream = Flowable.just(context.entry.observationEntry)
        .compose(update(errorOrObservationStream, (e, v) -> e.observation = v.or(null), "observation"))
        .compose(update(peakDayUpdates, (e, v) -> e.peakDay = v, "peakDay"))
        .compose(update(uncertainUpdates, (e, v) -> e.uncertain = v, "uncertain"))
        .compose(update(intercourseUpdates, (e, v) -> e.intercourse = v, "intercourse"))
        .compose(update(firstDayOfCycleUpdates, (e, v) -> e.firstDay = v, "firstDay"))
        .compose(update(positivePregnancyTestUpdates, (e, v) -> e.positivePregnancyTest = v, "positivePregnancyTest"))
        .compose(update(pointOfChangeUpdates, (e, v) -> e.pointOfChange = v, "pointOfChange"))
        .compose(update(timeOfDayUpdates, (e, v) -> e.intercourseTimeOfDay = v, "timeOfDay"))
        .compose(update(noteUpdates, (e, v) -> e.note = v, "notes"))
        .compose(update(clarifyingQuestionUpdateList,
            (e, v) -> {
              for (ClarifyingQuestionUpdate u : v) {
                e.updateClarifyingQuestion(u.question, u.answer);
              }
            }, "clarifyingQuestions"))
        ;

    Flowable<List<ClarifyingQuestionUpdate>> clarifyingQuestionRenderUpdates = observationEntryStream
        .distinctUntilChanged()
        .map(observationEntry -> {
          ImmutableList.Builder<ClarifyingQuestionUpdate> builder = ImmutableList.builder();
          if (context.shouldAskDoublePeakQuestions) {
            builder.add(new ClarifyingQuestionUpdate(
                ClarifyingQuestion.UNUSUAL_BUILDUP, observationEntry.unusualBuildup));
            builder.add(new ClarifyingQuestionUpdate(
                ClarifyingQuestion.UNUSUAL_STRESS, observationEntry.unusualStress));
          }
          if (context.shouldAskEssentialSameness) {
            builder.add(new ClarifyingQuestionUpdate(
                ClarifyingQuestion.ESSENTIAL_SAMENESS, observationEntry.isEssentiallyTheSame));
          }
          return builder.build();
        });

    Flowable<SymptomEntry> symptomEntryStream = symptomUpdates
        .toFlowable(BackpressureStrategy.BUFFER)
        .distinctUntilChanged()
        .doOnNext(i -> Timber.v("New symptom updates"))
        .map(activeSymptoms -> new SymptomEntry(context.entry.symptomEntry, activeSymptoms));

    Flowable<WellnessEntry> wellnessEntryStream = wellnessUpdates
        .toFlowable(BackpressureStrategy.BUFFER)
        .distinctUntilChanged()
        .doOnNext(i -> Timber.v("New wellness updates"))
        .map(activeItems -> new WellnessEntry(context.entry.wellnessEntry, activeItems));

    Flowable<Optional<StickerSelection>> stickerSelectionStream =
        Flowable.just(Optional.ofNullable(context.entry.stickerSelection));

    Flowable.combineLatest(
        errorOrObservationStream
            .map(errorOrObservation -> !errorOrObservation.hasError() ? "" : errorOrObservation.error().getMessage())
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW observation error")),
        observationEntryStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW observation entry")),
        symptomEntryStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW symptom entry")),
        wellnessEntryStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW wellness entry")),
        measurementEntries.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW measurement entry")),
        breastfeedingEntrySubject.toFlowable(BackpressureStrategy.BUFFER)
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW breastfeeding entry")),
        stickerSelectionStream
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW sticker selection")),
        clarifyingQuestionRenderUpdates
            .distinctUntilChanged()
            .doOnNext(i -> Timber.d("NEW clarifying quesiton render updates")),
        (observationError, observationEntry, symptomEntry, wellnessEntry, measurementEntry, breastfeedingEntry, stickerSelection, clarifyingQuestionUpdates) -> {
          ViewState state = new ViewState(
              context,
              new ChartEntry(entryDate, observationEntry, wellnessEntry, symptomEntry, measurementEntry, breastfeedingEntry, stickerSelection.orElse(null)),
              observationError);

          state.clarifyingQuestionState.addAll(clarifyingQuestionUpdates);

          return state;
        })
        .distinctUntilChanged()
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

    if (viewState.entryModificationContext.shouldAskEssentialSameness
        && viewState.previousPrompts.contains(ClarifyingQuestion.ESSENTIAL_SAMENESS)
        && !observationEntry.isEssentiallyTheSame) {
      return ImmutableList.of(
          new ClarifyingQuestionUpdate(ClarifyingQuestion.POINT_OF_CHANGE, observationEntry.pointOfChange));
    }
    if (viewState.entryModificationContext.shouldAskEssentialSameness) {
      return ImmutableList.of(
          new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, observationEntry.isEssentiallyTheSame));
    }
    return ImmutableList.of();
  }

  Single<Boolean> showMeasurementPage() {
    return shouldShowMeasurementPage;
  }

  Single<Boolean> showBreastfeedingPage() {
    return shouldShowBreastfeedingPage;
  }

  public LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates
        .toFlowable(BackpressureStrategy.DROP)
        .doOnNext(viewState -> Timber.d("PUBLISHING new ViewState")));
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
          return Maybe.just(viewState);
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
      actions.add(mCycleRepo.joinCycle(currentCycle, RWCycleRepo.JoinType.WITH_PREVIOUS).ignoreElement());
    } else if (!updatedEntry.observationEntry.positivePregnancyTest && originalEntry.observationEntry.positivePregnancyTest) {
      // Join for pregnancy test
      actions.add(mPregnancyRepo.revertPregnancy(updatedEntry.entryDate));
    }
    actions.add(mEntryRepo.insert(updatedEntry));
    return Completable.merge(actions);
  }

  public static class ViewState {
    public final CycleRenderer.EntryModificationContext entryModificationContext;
    public final ChartEntry chartEntry;
    public final String observationErrorText;
    public final boolean isInPregnancy;
    public final List<ClarifyingQuestionUpdate> clarifyingQuestionState = new ArrayList<>();
    public final List<ValidationIssue> validationIssues = new ArrayList<>();
    public final Set<ClarifyingQuestion> previousPrompts = new HashSet<>();

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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ViewState viewState = (ViewState) o;
      return isInPregnancy == viewState.isInPregnancy &&
          entryModificationContext.equals(viewState.entryModificationContext) &&
          chartEntry.equals(viewState.chartEntry) &&
          observationErrorText.equals(viewState.observationErrorText) &&
          clarifyingQuestionState.equals(viewState.clarifyingQuestionState) &&
          validationIssues.equals(viewState.validationIssues) &&
          previousPrompts.equals(viewState.previousPrompts);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entryModificationContext, chartEntry, observationErrorText, isInPregnancy, clarifyingQuestionState, validationIssues, previousPrompts);
    }

    private void validate() {
      ObservationEntry observationEntry = chartEntry.observationEntry;
      CycleRenderer.EntryModificationContext entryContext = entryModificationContext;

      if (!observationErrorText.isEmpty()) {
        validationIssues.add(ValidationIssue.block("Incomplete Observation", "Please complete or clear the observation before saving."));
      }
      if (observationEntry.intercourse && observationEntry.intercourseTimeOfDay == IntercourseTimeOfDay.NONE) {
        validationIssues.add(ValidationIssue.block("Missing Time of Day", "Please select a time of day for reporting intercourse."));
      }
    }

    public boolean renderClarifyingQuestions() {
      return entryModificationContext.entry.observationEntry.observation != null;
    }

    public boolean promptForClarifyingQuestions() {
      return !renderClarifyingQuestions();
    }

    public boolean showPointOfChange() {
      return renderClarifyingQuestions() && entryModificationContext.shouldAskEssentialSameness;
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

  static class Factory implements ViewModelProvider.Factory {
    private final @NonNull Application application;
    private final CycleRenderer.EntryModificationContext context;

    public Factory(@NonNull Application application, CycleRenderer.EntryModificationContext context) {
      this.application = application;
      this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new EntryDetailViewModel(application, context);
    }
  }

  private static <T extends Copyable<T>, X> FlowableTransformer<T, T> update(Flowable<X> source, BiConsumer<T, X> consumerFn, String name) {
    return RxUtil.update(source.distinctUntilChanged(), (t, x) -> {
      Timber.v("Updating %s to %s", name, x.toString());
      consumerFn.accept(t, x);
    }, name);
  }

  private static <T extends Copyable<T>, X> FlowableTransformer<T, T> update(Observable<X> source, BiConsumer<T, X> consumerFn, String name) {
    return update(source.toFlowable(BackpressureStrategy.BUFFER), consumerFn, name);
  }

}
