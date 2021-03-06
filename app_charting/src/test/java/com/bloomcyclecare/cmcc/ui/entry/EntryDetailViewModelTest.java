package com.bloomcyclecare.cmcc.ui.entry;


import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.observation.ClarifyingQuestion;
import com.bloomcyclecare.cmcc.data.models.observation.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.bloomcyclecare.cmcc.ui.entry.observation.ClarifyingQuestionUpdate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.reactivex.Flowable;

import static com.google.common.truth.Truth.assertThat;


public class EntryDetailViewModelTest {

  private ChartingApp mockApplication;
  private CycleRenderer.EntryModificationContext mContext;
  private EntryDetailViewModel mViewModel;

  @Before
  public void setUp() {

    PreferenceRepo.PreferenceSummary mockPreferenceSummary = Mockito.mock(PreferenceRepo.PreferenceSummary.class);

    PreferenceRepo mockPreferenceRepo = Mockito.mock(PreferenceRepo.class);
    Mockito.when(mockPreferenceRepo.summaries()).thenReturn(Flowable.just(mockPreferenceSummary));

    RWPregnancyRepo mockPregnancyRepo = Mockito.mock(RWPregnancyRepo.class);
    Mockito.when(mockPregnancyRepo.getAll()).thenReturn(Flowable.just(ImmutableList.of()));

    mockApplication = Mockito.mock(ChartingApp.class);
    Mockito.when(mockApplication.preferenceRepo()).thenReturn(mockPreferenceRepo);
    Mockito.when(mockApplication.pregnancyRepo(ArgumentMatchers.any())).thenReturn(mockPregnancyRepo);

    Cycle cycle = new Cycle("test", LocalDate.now(), null, null);
    mContext = new CycleRenderer.EntryModificationContext(
        cycle, ChartEntry.emptyEntry(LocalDate.now()));
  }

  private void initModel() {
    mViewModel = new EntryDetailViewModel(mockApplication, mContext);
  }

  @Test
  public void testEmitsStateWithoutUpdates() {
    initModel();

    mViewModel.viewStatesRx()
        .test()
        .awaitCount(1)
        .assertValueCount(1)
        .dispose();
  }

  @Test
  public void testNoteUpdates() {
    initModel();

    mViewModel.noteUpdates.onNext("some note");
    mViewModel.viewStatesRx()
        .test()
        .awaitCount(1)
        .assertValue(s -> s.chartEntry.observationEntry.note.equals("some note"))
        .dispose();
  }

  @Test
  public void testIntercourseTimeOfDay() throws Exception {
    initModel();

    mViewModel.intercourseUpdates.onNext(true);
    mViewModel.timeOfDayUpdates.onNext(IntercourseTimeOfDay.END);
    mViewModel.viewStatesRx()
        .test()
        .awaitCount(1)
        .assertValue(s -> s.chartEntry.observationEntry.intercourseTimeOfDay.equals(IntercourseTimeOfDay.END));
  }

  @Test
  public void testIntercourseWithoutTimeOfDay() throws Exception {
    initModel();

    mViewModel.intercourseUpdates.onNext(true);

    mViewModel.viewStatesRx()
        .test()
        .awaitCount(1)
        .assertValue(s -> Iterables.getOnlyElement(s.validationIssues).action.equals(EntryDetailViewModel.ValidationAction.BLOCK));
  }

  @Test
  public void testAskEssentialSamenessQuestion() throws Exception {
    mContext.shouldAskEssentialSameness = true;
    mContext.entry.observationEntry.observation = ObservationParser.parse("6CX1").orElse(null);
    assertThat(mContext.entry.observationEntry.hasMucus()).isTrue();
    initModel();

    EntryDetailViewModel.ViewState state = mViewModel.viewStatesRx().blockingFirst();
    assertThat(state.chartEntry.observationEntry.isEssentiallyTheSame).isFalse();
    assertThat(state.clarifyingQuestionState)
        .containsExactly(new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, false));

    toggleClarifyingQuestion(ClarifyingQuestion.ESSENTIAL_SAMENESS, true);
    state = mViewModel.viewStatesRx().blockingFirst();
    assertThat(state.chartEntry.observationEntry.isEssentiallyTheSame).isTrue();
    assertThat(state.clarifyingQuestionState).containsExactly(
        new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, true));

    toggleClarifyingQuestion(ClarifyingQuestion.ESSENTIAL_SAMENESS, false);
    state = mViewModel.viewStatesRx().blockingFirst();
    assertThat(state.chartEntry.observationEntry.isEssentiallyTheSame).isFalse();
    assertThat(state.clarifyingQuestionState).containsExactly(
        new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, false));
  }

  @Test
  public void testAskDoublePeakQuestions() {
    mContext.shouldAskDoublePeakQuestions = true;
    initModel();

    assertThat(mViewModel.viewStatesRx().blockingFirst().clarifyingQuestionState)
        .containsExactly(
            new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_STRESS, false),
            new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_BUILDUP, false));
  }

  private void toggleClarifyingQuestion(ClarifyingQuestion question, boolean answer) {
    mViewModel.clarifyingQuestionUpdates.onNext(new ClarifyingQuestionUpdate(question, answer));
  }
}
