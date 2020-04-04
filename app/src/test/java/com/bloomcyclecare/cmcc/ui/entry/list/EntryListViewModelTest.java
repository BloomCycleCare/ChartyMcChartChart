package com.bloomcyclecare.cmcc.ui.entry.list;

import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class EntryListViewModelTest {

  private static final LocalDate ONE_WEEK_AGO = LocalDate.now().minusWeeks(1);
  private static final LocalDate THIRTY_DAYS_AGO = LocalDate.now().minusDays(30);

  @Test
  public void testNoCycles() {
    assertThat(EntryListViewModel.subtitle(ImmutableList.of(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("No data...");
  }

  @Test
  public void testInvalidIndex() {
    List<CycleRenderer.CycleStats> stats = ImmutableList.of(
        new CycleRenderer.CycleStats(ONE_WEEK_AGO));
    assertThat(EntryListViewModel.subtitle(stats, 1, LocalDate::now, Optional.absent()))
        .isEqualTo("Invalid index!");
  }

  @Test
  public void testPreviousCycle() {
    CycleRenderer.CycleStats stats = new CycleRenderer.CycleStats(THIRTY_DAYS_AGO);
    stats.daysPrePeak = 4;
    stats.daysPostPeak = 12;
    assertThat(EntryListViewModel.subtitle(ImmutableList.of(stats), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("Pre: 4 Post: 12");
  }

  @Test
  public void testPrePeakPhase() {
    CycleRenderer.CycleStats stats = new CycleRenderer.CycleStats(ONE_WEEK_AGO);
    assertThat(EntryListViewModel.subtitle(ImmutableList.of(stats), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("In prepeak phase");
  }

  @Test
  public void testPeakDay() {
    CycleRenderer.CycleStats stats = new CycleRenderer.CycleStats(ONE_WEEK_AGO);
    stats.daysPrePeak = 7;
    assertThat(EntryListViewModel.subtitle(ImmutableList.of(stats), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("0 days postpeak");
  }

  @Test
  public void testPostPeakPrediction() {
    ImmutableList.Builder<CycleRenderer.CycleStats> statsBuilder = ImmutableList.builder();

    int typicalPostPeakLength = 12;

    CycleRenderer.CycleStats currentStats = new CycleRenderer.CycleStats(THIRTY_DAYS_AGO);
    currentStats.daysPrePeak = 20;
    statsBuilder.add(currentStats);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats firstPreviousStats = new CycleRenderer.CycleStats(LocalDate.now().minusMonths(2));
    firstPreviousStats.daysPrePeak = 17;
    firstPreviousStats.daysPostPeak = typicalPostPeakLength;
    statsBuilder.add(firstPreviousStats);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats secondPreviousStats = new CycleRenderer.CycleStats(LocalDate.now().minusMonths(3));
    secondPreviousStats.daysPrePeak = 17;
    secondPreviousStats.daysPostPeak = typicalPostPeakLength - 1;
    statsBuilder.add(secondPreviousStats);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats thirdPreviousStats = new CycleRenderer.CycleStats(LocalDate.now().minusMonths(4));
    thirdPreviousStats.daysPrePeak = 17;
    thirdPreviousStats.daysPostPeak = typicalPostPeakLength;
    statsBuilder.add(thirdPreviousStats);

    LocalDate expectedPrediction = THIRTY_DAYS_AGO.plusDays(20).plusDays(1).plusDays(typicalPostPeakLength);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("Potential end: 2±0.7 days");
  }

  @Test
  public void testIrregularPostPeak() {
    ImmutableList.Builder<CycleRenderer.CycleStats> statsBuilder = ImmutableList.builder();

    CycleRenderer.CycleStats currentStats = new CycleRenderer.CycleStats(THIRTY_DAYS_AGO);
    currentStats.daysPrePeak = 20;
    statsBuilder.add(currentStats);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats firstPreviousStats = new CycleRenderer.CycleStats(LocalDate.now().minusMonths(2));
    firstPreviousStats.daysPrePeak = 17;
    firstPreviousStats.daysPostPeak = 10;
    statsBuilder.add(firstPreviousStats);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats secondPreviousStats = new CycleRenderer.CycleStats(LocalDate.now().minusMonths(3));
    secondPreviousStats.daysPrePeak = 17;
    secondPreviousStats.daysPostPeak = 15;
    statsBuilder.add(secondPreviousStats);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("10 days postpeak");

    CycleRenderer.CycleStats thirdPreviousStats = new CycleRenderer.CycleStats(LocalDate.now().minusMonths(4));
    thirdPreviousStats.daysPrePeak = 17;
    thirdPreviousStats.daysPostPeak = 13;
    statsBuilder.add(thirdPreviousStats);

    assertThat(EntryListViewModel.subtitle(statsBuilder.build(), 0, LocalDate::now, Optional.absent()))
        .isEqualTo("10 days postpeak");
  }
}
