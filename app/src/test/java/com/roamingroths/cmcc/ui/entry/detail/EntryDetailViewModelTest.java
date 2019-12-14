package com.roamingroths.cmcc.ui.entry.detail;


import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.domain.ClarifyingQuestion;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;


public class EntryDetailViewModelTest {

  private CycleRenderer.EntryModificationContext mContext;
  private EntryDetailViewModel mViewModel;

  @Before
  public void setUp() {
    MyApplication mockApplication = Mockito.mock(MyApplication.class);
    mViewModel = new EntryDetailViewModel(mockApplication);

    mContext = new CycleRenderer.EntryModificationContext();
    mContext.entry = ChartEntry.emptyEntry(LocalDate.now());
  }

  private void initModel() {
    mViewModel.initialize(mContext);
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
  public void testAskEssentialSamenessQuestion() throws Exception {
    mContext.shouldAskEssentialSamenessIfMucus = true;
    mContext.entry.observationEntry.observation = Observation.fromString("6CX1");
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
  public void testAskUnusualBleeding() {
    initModel();
    EntryDetailViewModel.ViewState state = mViewModel.viewStatesRx().blockingFirst();
    assertThat(state.clarifyingQuestionState).isEmpty();

    mViewModel.observationUpdates.onNext("H");
    state = mViewModel.viewStatesRx().blockingFirst();
    assertThat(state.chartEntry.observationEntry.unusualBleeding).isFalse();
    assertThat(state.clarifyingQuestionState).containsExactly(
        new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_BLEEDING, false));

    toggleClarifyingQuestion(ClarifyingQuestion.UNUSUAL_BLEEDING, true);
    state = mViewModel.viewStatesRx().blockingFirst();
    assertThat(state.chartEntry.observationEntry.unusualBleeding).isTrue();
    assertThat(state.clarifyingQuestionState).containsExactly(
        new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_BLEEDING, true));

    toggleClarifyingQuestion(ClarifyingQuestion.UNUSUAL_BLEEDING, false);
    state = mViewModel.viewStatesRx().blockingFirst();
    assertThat(state.chartEntry.observationEntry.unusualBleeding).isFalse();
    assertThat(state.clarifyingQuestionState).containsExactly(
        new ClarifyingQuestionUpdate(ClarifyingQuestion.UNUSUAL_BLEEDING, false));
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
