package com.bloomcyclecare.cmcc.logic.breastfeeding;

import android.util.Pair;
import android.util.Range;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.math.Quantiles;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.Single;
import timber.log.Timber;

public class BreastfeedingStats {

  private static final Duration MIN_GAP = Duration.standardMinutes(15);

  private final BabyDaybookDB mDB;

  public BreastfeedingStats(BabyDaybookDB db) {
    mDB = db;
  }

  public static class AggregateStats {
    public final Range<LocalDate> dateRange;

    public final double nDayMedian;
    public final double nDayMean;
    public final double nDayInterval;

    public final double nNightMedian;
    public final double nNightMean;
    public final double nNightInterval;

    public final double maxGapMedian;
    public final double maxGapP95;
    public final double maxGapP99;

    private AggregateStats(Collection<DailyStats> dailyStats) {
      int n = dailyStats.size();

      List<Integer> nDays = dailyStats.stream().map(s -> s.nDay).collect(Collectors.toList());
      nDayMedian = Quantiles.median().compute(nDays);

      List<Integer> nNights = dailyStats.stream().map(s -> s.nNight).collect(Collectors.toList());
      nNightMedian = Quantiles.median().compute(nNights);

      List<Double> maxGaps = dailyStats.stream().map(s -> s.longestGapDuration().getStandardMinutes() / (double) 60).collect(Collectors.toList());
      maxGapMedian = Quantiles.median().compute(maxGaps);
      maxGapP95 = Quantiles.percentiles().index(95).compute(maxGaps);
      maxGapP99 = Quantiles.percentiles().index(99).compute(maxGaps);

      LocalDate firstDate = null, lastDate = null;
      int nDaySum = 0;
      int nNightSum = 0;
      for (DailyStats stats : dailyStats) {
        if (firstDate == null || stats.day.isBefore(firstDate)) {
          firstDate = stats.day;
        }
        if (lastDate == null || stats.day.isAfter(lastDate)) {
          lastDate = stats.day;
        }
        nDaySum += stats.nDay;
        nNightSum += stats.nNight;
      }

      nDayMean = nDaySum / (float) n;
      nNightMean = nNightSum / (float) n;

      double nDaySquaredDiffSum = 0.0;
      double nNightSquaredDiffSum = 0.0;
      for (DailyStats stats : dailyStats) {
        nDaySquaredDiffSum += Math.pow(stats.nDay - nDayMean, 2);
        nNightSquaredDiffSum += Math.pow(stats.nNight - nNightMean, 2);
      }

      nDayInterval = 1.96 * Math.sqrt(nDaySquaredDiffSum / n) / Math.sqrt(n);
      nNightInterval = 1.96 * Math.sqrt(nNightSquaredDiffSum / n) / Math.sqrt(n);

      dateRange = Range.create(firstDate, lastDate);
    }
  }

  public static AggregateStats aggregate(Collection<DailyStats> dailyStats) {
    return new AggregateStats(dailyStats);
  }

  public static class DailyStats {
    public final LocalDate day;
    public final ImmutableSortedSet<Interval> gaps;
    public final int nDay;
    public final int nNight;

    public DailyStats(LocalDate day, ImmutableSortedSet<Interval> gaps, int nDay, int nNight) {
      this.day = day;
      this.gaps = gaps;
      this.nDay = nDay;
      this.nNight = nNight;
    }

    public Duration shortestGapDuration() {
      return duration(gaps.first());
    }

    public Duration longestGapDuration() {
      return duration(gaps.last());
    }
  }

  public Single<ImmutableSortedMap<LocalDate, DailyStats>> dailyStats(String babyName) {
    return indexByDay(mDB.actionIntervals(babyName, "breastfeeding"), true, MIN_GAP)
        .flatMap(m -> Observable.fromIterable(m.entrySet())
            .map(e -> {
              int nDay = 0, nNight = 0;
              for (BabyDaybookDB.ActionInterval ai : e.getValue()) {
                int startHour = ai.interval.getStart().getHourOfDay();
                if (startHour < 8 || startHour > 21) {
                  nNight++;
                } else {
                  nDay++;
                }
              }
              ImmutableSortedSet<Interval> gaps = getGaps(e.getValue());
              return new DailyStats(e.getKey(), gaps, nDay, nNight);
            })
            .collectInto(ImmutableSortedMap.<LocalDate, DailyStats>naturalOrder(), (b, s) -> b.put(s.day, s))
            .map(ImmutableSortedMap.Builder::build));
  }

  private static Single<ImmutableMap<LocalDate, ImmutableSortedSet<BabyDaybookDB.ActionInterval>>> indexByDay(
      Single<List<BabyDaybookDB.ActionInterval>> actions, boolean includeOverlaps, Duration minGap) {
    return actions
        // Merge adjacent intervals with < minGap
        .map(intervals -> mergeAdjacent(intervals, minGap))
        .flatMap(startTimes -> Observable.fromIterable(startTimes)
            .groupBy(ai -> new LocalDate(ai.interval.getStart()))
            // Flatten group into (day, [intervals])
            .flatMap(group -> group
                .sorted()
                .collectInto(new LinkedList<BabyDaybookDB.ActionInterval>(), LinkedList::add)
                .map(is -> Pair.create(group.getKey(), is))
                .toObservable()
            )
            // Add pairs to the index builder
            .collectInto(
                new HashMap<LocalDate, LinkedList<BabyDaybookDB.ActionInterval>>(),
                (b, p) -> b.put(p.first, p.second)))
        .map(index -> {
          if (!includeOverlaps) {
            return index;
          }
          return addOverlaps(index);
        })
        // Finalize the index
        .map(index -> {
          ImmutableMap.Builder<LocalDate, ImmutableSortedSet<BabyDaybookDB.ActionInterval>> out =
              ImmutableMap.builder();
          for (Map.Entry<LocalDate, ? extends List<BabyDaybookDB.ActionInterval>> e : index.entrySet()) {
            out.put(e.getKey(), ImmutableSortedSet.copyOf(e.getValue()));
          }
          return out.build();
        });
  }

  private static List<BabyDaybookDB.ActionInterval> mergeAdjacent(List<BabyDaybookDB.ActionInterval> intervals, Duration minGap) {
    List<BabyDaybookDB.ActionInterval> out = new ArrayList<>();
    BabyDaybookDB.ActionInterval previousInterval = null;
    for (BabyDaybookDB.ActionInterval ai : intervals) {
      if (previousInterval == null) {
        previousInterval = ai;
        continue;
      }
      Duration gap = new Duration(previousInterval.interval.getEnd(), ai.interval.getStart());
      if (gap.isShorterThan(minGap)) {
        Timber.v("Merging %s and %s", previousInterval, ai);
        previousInterval = new BabyDaybookDB.ActionInterval(
            ai.type, new Interval(previousInterval.interval.getStart(), ai.interval.getEnd()));
      } else {
        out.add(previousInterval);
        previousInterval = ai;
      }
    }
    if (previousInterval != null) {
      out.add(previousInterval);
    }
    return out;
  }

  private static Map<LocalDate, ? extends List<BabyDaybookDB.ActionInterval>> addOverlaps(
      Map<LocalDate, LinkedList<BabyDaybookDB.ActionInterval>> index) {
    for (Map.Entry<LocalDate, LinkedList<BabyDaybookDB.ActionInterval>> entry : index.entrySet()) {
      // Check last interval of previous day
      /*Optional.ofNullable(index.get(entry.getKey().minusDays(1)))
          .flatMap(entries -> entries.isEmpty() ?
              Optional.empty() : Optional.of(entries.getLast()))
          .flatMap(i -> new LocalDate(i.interval.getEnd()).equals(entry.getKey()) ?
              Optional.empty() : Optional.of(i))
          .ifPresent(i -> entry.getValue().addFirst(i));*/
      // Check first interval of next day
      Optional.ofNullable(index.get(entry.getKey().plusDays(1)))
          .flatMap(entries -> entries.isEmpty() ?
              Optional.empty() : Optional.of(entries.getFirst()))
          .flatMap(i -> new LocalDate(i.interval.getStart()).equals(entry.getKey()) ?
              Optional.empty() : Optional.of(i))
          .ifPresent(i -> entry.getValue().addLast(i));
    }
    return index;
  }

  private static ImmutableSortedSet<Interval> getGaps(ImmutableSortedSet<BabyDaybookDB.ActionInterval> intervals) {
    ImmutableSortedSet.Builder<Interval> gaps = ImmutableSortedSet.orderedBy((a, b) -> new Duration(a.getStart(), a.getEnd()).compareTo(new Duration(b.getStart(), b.getEnd())));
    Interval previousInterval = null;
    for (BabyDaybookDB.ActionInterval ai : intervals) {
      if (previousInterval != null) {
        if (ai.interval.isBefore(previousInterval)) {
          throw new IllegalArgumentException("Intervals should be sorted!");
        }
        Interval gap = previousInterval.gap(ai.interval);
        if (gap == null) {
          Timber.d("No gap between interval");
        } else {
          gaps.add(gap);
        }
      }
      previousInterval = ai.interval;
    }
    return gaps.build();
  }

  private static Duration duration(Interval interval) {
    return new Duration(interval.getStart(), interval.getEnd());
  }
}
