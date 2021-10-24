package com.bloomcyclecare.cmcc.ui.cycle;

import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import timber.log.Timber;

public class CycleStatProvider {

  private final ImmutableMap<Stat, StatView> mStatViews;

  public CycleStatProvider(Collection<CycleRenderer.RenderableCycle> cycles) {
    mStatViews = getStatViews(cycles);
  }

  public StatView get(Stat stat) {
    return mStatViews.get(stat);
  }

  public enum Stat {
    FLOW,
    POC,
    PEAK,
    END
  }

  public static class StatView {
    public final String summary;
    public final ImmutableMap<Integer, Integer> dayCounts;

    StatView(String summary, ImmutableMap<Integer, Integer> dayCounts) {
      this.summary = summary;
      this.dayCounts = dayCounts;
    }
  }

  private static ImmutableMap<Stat, StatView> getStatViews(Collection<CycleRenderer.RenderableCycle> renderableCycles) {
    int maxCycleLength = 35;
    List<CycleRenderer.RenderableCycle> cycles = new ArrayList<>();
    Timber.v("Getting stats for %d cycles", renderableCycles.size());
    for (CycleRenderer.RenderableCycle cycle : renderableCycles.stream()
        .sorted((c1, c2) -> c2.cycle().compareTo(c1.cycle()))
        .collect(Collectors.toList())) {
      if (cycle.entries().size() <= maxCycleLength) {
        cycles.add(cycle);
      } else {
        Timber.v("Cycle starting %s had %d entries which exceeded the limit of %d", cycle.cycle().startDateStr, cycle.entries().size(), maxCycleLength);
        break;
      }
    }
    Timber.v("Only considering last %d cycles", cycles.size());
    ImmutableMap.Builder<Stat, StatView> builder = ImmutableMap.builder();

    builder.put(Stat.FLOW, getStatView(
        "Typical flow length: ", cycles,
        (c) -> c.stats().daysOfFlow() < c.entries().size(),  // Only look at cases where we are "past" the flow
        (c) -> c.stats().daysOfFlow()));
    //noinspection OptionalGetWithoutIsPresent
    builder.put(Stat.POC, getStatView(
        "Typical PoC: ", cycles,
        (c) -> c.stats().daysBeforePoC().isPresent(),  // Only look at cases where we are "past" the PoC
        (c) -> c.stats().daysBeforePoC().get() + 1));
    //noinspection OptionalGetWithoutIsPresent
    builder.put(Stat.PEAK, getStatView(
        "Typical peak day: ", cycles,
        (c) -> c.stats().daysPrePeak().isPresent(),  // Only consider cycles which have had a peak day
        (c) -> c.stats().daysPrePeak().get() + 1));
    builder.put(Stat.END, getStatView(
        "Typical cycle length: ", cycles,
        (c) -> c.cycle().endDate != null,  // Only consider cycles which have ended
        (c) -> c.entries().size()));

    return builder.build();
  }

  private static String getStatSummary(Map<Integer, Integer> dayCounts) {
    SummaryStatistics stats = new SummaryStatistics();
    dayCounts.entrySet().forEach(e -> {
      for (int i=0; i<e.getValue(); i++) {
        stats.addValue(e.getKey());
      }
    });
    long mean = Math.round(stats.getMean());
    double ci95 = 1.960 * stats.getStandardDeviation() / Math.sqrt(stats.getN());
    return String.format(Locale.getDefault(), "%d±%.1f", mean, ci95);
  }

  private static StatView getStatView(
      String summaryPrefix,
      Collection<CycleRenderer.RenderableCycle> cycles,
      Predicate<CycleRenderer.RenderableCycle> filterFn,
      Function<CycleRenderer.RenderableCycle, Integer> mapperFn) {
    Map<Integer, Integer> dayCounts = new HashMap<>();
    for (Integer day : cycles.stream()
        .filter(filterFn)
        .map(mapperFn)
        .collect(Collectors.toList())) {
      Integer count = dayCounts.get(day);
      if (count == null) {
        count = 0;
      }
      dayCounts.put(day, ++count);
    }
    return new StatView(summaryPrefix + getStatSummary(dayCounts), ImmutableMap.copyOf(dayCounts));
  }
}
