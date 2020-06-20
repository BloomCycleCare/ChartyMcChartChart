package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

public class CyclePageViewModelTest {

  private static final LocalDate ONE_WEEK_AGO = LocalDate.now().minusWeeks(1);
  private static final LocalDate THIRTY_DAYS_AGO = LocalDate.now().minusDays(30);

  @Test
  public void testNoCycles() {
    assertThat(CyclePageViewModel.subtitle(ImmutableList.of(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("No data...");
  }

  @Test
  public void testInvalidIndex() {
    List<CycleRenderer.CycleStats> stats = ImmutableList.of(
        CycleRenderer.CycleStats.builder().cycleStartDate(ONE_WEEK_AGO).build());
    assertThat(CyclePageViewModel.subtitle(stats, 1, LocalDate::now, Optional.empty()))
        .isEqualTo("Invalid index!");
  }

  @Test
  public void testPreviousCycle() {
    CycleRenderer.CycleStats stats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(THIRTY_DAYS_AGO)
        .daysPrePeak(Optional.of(4))
        .daysPostPeak(Optional.of(12))
        .build();
    assertThat(CyclePageViewModel.subtitle(ImmutableList.of(stats), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("Pre: 4 Post: 12");
  }

  @Test
  public void testPrePeakPhase() {
    CycleRenderer.CycleStats stats = CycleRenderer.CycleStats.builder().cycleStartDate(ONE_WEEK_AGO).build();
    assertThat(CyclePageViewModel.subtitle(ImmutableList.of(stats), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("In prepeak phase");
  }

  @Test
  public void testPeakDay() {
    CycleRenderer.CycleStats stats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(ONE_WEEK_AGO)
        .daysPrePeak(Optional.of(7))
        .build();
    assertThat(CyclePageViewModel.subtitle(ImmutableList.of(stats), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("0 days postpeak");
  }

  @Test
  public void testPostPeakPrediction() {
    ImmutableList.Builder<CycleRenderer.CycleStats> statsBuilder = ImmutableList.builder();

    int typicalPostPeakLength = 12;

    CycleRenderer.CycleStats currentStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(THIRTY_DAYS_AGO)
        .daysPrePeak(Optional.of(20))
        .build();
    statsBuilder.add(currentStats);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats firstPreviousStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(LocalDate.now().minusMonths(2))
        .daysPrePeak(Optional.of(17))
        .daysPostPeak(Optional.of(typicalPostPeakLength))
        .build();
    statsBuilder.add(firstPreviousStats);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats secondPreviousStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(LocalDate.now().minusMonths(3))
        .daysPrePeak(Optional.of(17))
        .daysPostPeak(Optional.of(typicalPostPeakLength - 1))
        .build();
    statsBuilder.add(secondPreviousStats);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats thirdPreviousStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(LocalDate.now().minusMonths(4))
        .daysPrePeak(Optional.of(17))
        .daysPostPeak(Optional.of(typicalPostPeakLength))
        .build();
    statsBuilder.add(thirdPreviousStats);

    LocalDate expectedPrediction = THIRTY_DAYS_AGO.plusDays(20).plusDays(1).plusDays(typicalPostPeakLength);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("Potential end: 2±0.7 days");
  }

  @Test
  public void testIrregularPostPeak() {
    ImmutableList.Builder<CycleRenderer.CycleStats> statsBuilder = ImmutableList.builder();

    CycleRenderer.CycleStats currentStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(THIRTY_DAYS_AGO)
        .daysPrePeak(Optional.of(20))
        .build();
    statsBuilder.add(currentStats);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats firstPreviousStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(LocalDate.now().minusMonths(2))
        .daysPrePeak(Optional.of(17))
        .daysPostPeak(Optional.of(10))
        .build();
    statsBuilder.add(firstPreviousStats);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats secondPreviousStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(LocalDate.now().minusMonths(3))
        .daysPrePeak(Optional.of(17))
        .daysPostPeak(Optional.of(15))
        .build();
    statsBuilder.add(secondPreviousStats);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats thirdPreviousStats = CycleRenderer.CycleStats.builder()
        .cycleStartDate(LocalDate.now().minusMonths(4))
        .daysPrePeak(Optional.of(17))
        .daysPostPeak(Optional.of(13))
        .build();
    statsBuilder.add(thirdPreviousStats);

    assertThat(CyclePageViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.empty()))
        .isEqualTo("10 days postpeak");
  }
}
