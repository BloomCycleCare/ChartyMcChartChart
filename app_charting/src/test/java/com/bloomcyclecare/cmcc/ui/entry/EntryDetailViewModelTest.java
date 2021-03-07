package com.bloomcyclecare.cmcc.ui.entry;


import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.Entry;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.observation.ClarifyingQuestion;
import com.bloomcyclecare.cmcc.data.models.observation.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.entry.observation.ClarifyingQuestionUpdate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import net.lachlanmckee.timberjunit.TimberTestRule;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.reactivex.Flowable;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class EntryDetailViewModelTest {

  // TODO: test clarifying question updates.
  // TODO: test question resolution flow.
  // TODO: test PoC clarifying question flow.

  @Rule // From: https://jeroenmols.com/blog/2019/01/17/livedatajunit5/
  public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
  @Rule
  public TimberTestRule logRule = TimberTestRule.logAllWhenTestFails();

  @Mock
  private ChartingApp mockApplication;
  @Mock
  private CycleRenderer.EntryModificationContext mContext;
  @Mock
  private EntryDetailViewModel mViewModel;
  @Mock
  Observer<EntryDetailViewModel.ViewState> mObserver;

  private ArgumentCaptor<EntryDetailViewModel.ViewState> viewState = ArgumentCaptor.forClass(EntryDetailViewModel.ViewState.class);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    PreferenceRepo.PreferenceSummary mockPreferenceSummary = Mockito.mock(PreferenceRepo.PreferenceSummary.class);

    PreferenceRepo mockPreferenceRepo = Mockito.mock(PreferenceRepo.class);
    when(mockPreferenceRepo.summaries()).thenReturn(Flowable.just(mockPreferenceSummary));

    RWPregnancyRepo mockPregnancyRepo = Mockito.mock(RWPregnancyRepo.class);
    when(mockPregnancyRepo.getAll()).thenReturn(Flowable.just(ImmutableList.of()));

    when(mockApplication.preferenceRepo()).thenReturn(mockPreferenceRepo);
    when(mockApplication.pregnancyRepo(ArgumentMatchers.any())).thenReturn(mockPregnancyRepo);

    Cycle cycle = new Cycle("test", LocalDate.now(), null, null);
    mContext = new CycleRenderer.EntryModificationContext(
        cycle, ChartEntry.emptyEntry(LocalDate.now()));
  }

  private void initModel() {
    mViewModel = new EntryDetailViewModel(mockApplication, mContext);
    mViewModel.viewStates().observeForever(mObserver);
  }

  @Test
  public void testEmitsStateWithoutUpdates() {
    initModel();

    verify(mObserver, times(1)).onChanged(ArgumentMatchers.any());
  }

  @Test
  public void testObservationUpdate() {
    initModel();
    mViewModel.observationUpdates.onNext("Not an observation");
    mViewModel.observationUpdates.onNext("0 AD");

    verify(mObserver, times(4)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(4);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.observation).isNull();
    assertThat(initialValue.observationErrorText).isEmpty();


    EntryDetailViewModel.ViewState firstUpdate = viewState.getAllValues().get(1);
    assertThat(firstUpdate.chartEntry.observationEntry.observation).isNull();
    assertThat(firstUpdate.observationErrorText).isNotEmpty();

    // Skip second due to race

    EntryDetailViewModel.ViewState thirdUpdate = viewState.getAllValues().get(3);
    assertThat(thirdUpdate.chartEntry.observationEntry.observation).isNotNull();
    assertThat(thirdUpdate.observationErrorText).isEmpty();
  }

  @Test
  public void testPeakDayUpdates() {
    initModel();
    mViewModel.peakDayUpdates.onNext(true);

    verify(mObserver, times(2)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(2);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.peakDay).isFalse();

    EntryDetailViewModel.ViewState firstUpdate = viewState.getAllValues().get(1);
    assertThat(firstUpdate.chartEntry.observationEntry.peakDay).isTrue();
  }

  @Test
  public void testFirstDayOfCycleUpdates() {
    initModel();
    mViewModel.firstDayOfCycleUpdates.onNext(true);

    verify(mObserver, times(2)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(2);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.firstDay).isFalse();

    EntryDetailViewModel.ViewState firstUpdate = viewState.getAllValues().get(1);
    assertThat(firstUpdate.chartEntry.observationEntry.firstDay).isTrue();
  }

  @Test
  public void testPositivePregnancyTestUpdates() {
    initModel();
    mViewModel.positivePregnancyTestUpdates.onNext(true);

    verify(mObserver, times(2)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(2);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.positivePregnancyTest).isFalse();

    EntryDetailViewModel.ViewState firstUpdate = viewState.getAllValues().get(1);
    assertThat(firstUpdate.chartEntry.observationEntry.positivePregnancyTest).isTrue();
  }

  @Test
  public void testPointOfChange() {
    initModel();
    mViewModel.pointOfChangeUpdates.onNext(true);

    verify(mObserver, times(2)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(2);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.pointOfChange).isFalse();

    EntryDetailViewModel.ViewState firstUpdate = viewState.getAllValues().get(1);
    assertThat(firstUpdate.chartEntry.observationEntry.pointOfChange).isTrue();
  }

  @Test
  public void testNoteUpdates() {
    initModel();
    mViewModel.noteUpdates.onNext("some note");

    verify(mObserver, times(2)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(2);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.note).isEmpty();

    EntryDetailViewModel.ViewState firstUpdate = viewState.getAllValues().get(1);
    assertThat(firstUpdate.chartEntry.observationEntry.note).isEqualTo("some note");
  }

  @Test
  public void testIntercourseTimeOfDay() throws Exception {
    initModel();
    mViewModel.intercourseUpdates.onNext(true);
    mViewModel.timeOfDayUpdates.onNext(IntercourseTimeOfDay.END);
    mViewModel.intercourseUpdates.onNext(false);

    verify(mObserver, times(5)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(5);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.intercourse).isFalse();
    assertThat(initialValue.chartEntry.observationEntry.intercourseTimeOfDay).isEqualTo(IntercourseTimeOfDay.NONE);
    assertThat(initialValue.validationIssues).isEmpty();

    EntryDetailViewModel.ViewState firstUpdate = viewState.getAllValues().get(1);
    assertThat(firstUpdate.chartEntry.observationEntry.intercourse).isTrue();
    assertThat(firstUpdate.chartEntry.observationEntry.intercourseTimeOfDay).isEqualTo(IntercourseTimeOfDay.NONE);
    assertThat(firstUpdate.validationIssues).hasSize(1);
    assertThat(Iterables.getOnlyElement(firstUpdate.validationIssues).action).isEqualTo(EntryDetailViewModel.ValidationAction.BLOCK);

    EntryDetailViewModel.ViewState secondUpdate = viewState.getAllValues().get(2);
    assertThat(secondUpdate.chartEntry.observationEntry.intercourse).isTrue();
    assertThat(secondUpdate.chartEntry.observationEntry.intercourseTimeOfDay).isEqualTo(IntercourseTimeOfDay.END);
    assertThat(secondUpdate.validationIssues).isEmpty();

    // Skip validating the third update since there's a race when resetting time of day

    EntryDetailViewModel.ViewState fourthUpdate = viewState.getAllValues().get(4);
    assertThat(fourthUpdate.chartEntry.observationEntry.intercourse).isFalse();
    assertThat(fourthUpdate.chartEntry.observationEntry.intercourseTimeOfDay).isEqualTo(IntercourseTimeOfDay.NONE);
    assertThat(fourthUpdate.validationIssues).isEmpty();
  }

  @Test
  public void testAskEssentialSamenessQuestion() throws Exception {
    mContext.shouldAskEssentialSameness = true;
    initModel();

    toggleClarifyingQuestion(ClarifyingQuestion.ESSENTIAL_SAMENESS, true);
    toggleClarifyingQuestion(ClarifyingQuestion.ESSENTIAL_SAMENESS, false);

    verify(mObserver, times(5)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(5);

    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.chartEntry.observationEntry.isEssentiallyTheSame).isFalse();
    assertThat(initialValue.clarifyingQuestionState)
        .containsExactly(new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, false));
    assertThat(initialValue.previousPrompts).isEmpty();

    // Skip first update, race condition

    EntryDetailViewModel.ViewState secondUpdate = viewState.getAllValues().get(2);
    assertThat(secondUpdate.chartEntry.observationEntry.isEssentiallyTheSame).isTrue();
    assertThat(secondUpdate.clarifyingQuestionState).containsExactly(
        new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, true));
    assertThat(initialValue.previousPrompts).isEmpty();

    // Skip third update, race condition

    EntryDetailViewModel.ViewState fourthUpdate = viewState.getAllValues().get(4);
    assertThat(fourthUpdate.chartEntry.observationEntry.isEssentiallyTheSame).isFalse();
    assertThat(fourthUpdate.clarifyingQuestionState).containsExactly(
        new ClarifyingQuestionUpdate(ClarifyingQuestion.ESSENTIAL_SAMENESS, false));
  }

  @Test
  public void testAskDoublePeakQuestions() {
    mContext.shouldAskDoublePeakQuestions = true;
    initModel();

    verify(mObserver, times(1)).onChanged(viewState.capture());
    assertThat(viewState.getAllValues()).hasSize(1);


    EntryDetailViewModel.ViewState initialValue = viewState.getAllValues().get(0);
    assertThat(initialValue.clarifyingQuestionState)
        .containsExactly(
            new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_STRESS, false),
            new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_BUILDUP, false));
  }

  private void toggleClarifyingQuestion(ClarifyingQuestion question, boolean answer) {
    mViewModel.clarifyingQuestionUpdates.onNext(new ClarifyingQuestionUpdate(question, answer));
  }
}
