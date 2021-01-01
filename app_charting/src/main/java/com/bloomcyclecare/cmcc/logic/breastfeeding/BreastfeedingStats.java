package com.bloomcyclecare.cmcc.logic.breastfeeding;

import android.util.Pair;
import android.util.Range;

import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.entry.ROChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.ROPregnancyRepo;
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
  private final ROChartEntryRepo mChartEntryRepo;
  private final ROPregnancyRepo mPregnancyRepo;

  public BreastfeedingStats(BabyDaybookDB db, ROChartEntryRepo chartEntryRepo, ROPregnancyRepo pregnancyRepo) {
    mDB = db;
    mChartEntryRepo = chartEntryRepo;
    mPregnancyRepo = pregnancyRepo;
  }

  public static class AggregateStats {
    public final int nDaysWithStats;
    public final Range<LocalDate> dateRange;
    public final LocalDate longestGapDate;

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
      nDaysWithStats = dailyStats.size();

      List<Integer> nDays = dailyStats.stream().map(s -> s.nDay).collect(Collectors.toList());
      nDayMedian = Quantiles.median().compute(nDays);

      List<Integer> nNights = dailyStats.stream().map(s -> s.nNight).collect(Collectors.toList());
      nNightMedian = Quantiles.median().compute(nNights);

      List<Double> maxGaps = dailyStats.stream().map(s -> s.longestGapDuration.getStandardMinutes() / (double) 60).collect(Collectors.toList());
      maxGapMedian = Quantiles.median().compute(maxGaps);
      maxGapP95 = Quantiles.percentiles().index(95).compute(maxGaps);
      maxGapP99 = Quantiles.percentiles().index(99).compute(maxGaps);

      LocalDate firstDate = null, lastDate = null;
      int nDaySum = 0;
      int nNightSum = 0;
      DailyStats longestGap = null;
      for (DailyStats stats : dailyStats) {
        if (firstDate == null || stats.day.isBefore(firstDate)) {
          firstDate = stats.day;
        }
        if (lastDate == null || stats.day.isAfter(lastDate)) {
          lastDate = stats.day;
        }
        nDaySum += stats.nDay;
        nNightSum += stats.nNight;


        if (longestGap == null || stats.longestGapDuration.isLongerThan(longestGap.longestGapDuration)) {
          longestGap = stats;
        }
      }
      longestGapDate = longestGap.day;

      nDayMean = nDaySum / (float) nDaysWithStats;
      nNightMean = nNightSum / (float) nDaysWithStats;

      double nDaySquaredDiffSum = 0.0;
      double nNightSquaredDiffSum = 0.0;
      for (DailyStats stats : dailyStats) {
        nDaySquaredDiffSum += Math.pow(stats.nDay - nDayMean, 2);
        nNightSquaredDiffSum += Math.pow(stats.nNight - nNightMean, 2);
      }

      // TODO: use student's t thing if nDaysWithStats << 30
      nDayInterval = 1.96 * Math.sqrt(nDaySquaredDiffSum / nDaysWithStats) / Math.sqrt(nDaysWithStats);
      nNightInterval = 1.96 * Math.sqrt(nNightSquaredDiffSum / nDaysWithStats) / Math.sqrt(nDaysWithStats);

      dateRange = Range.create(firstDate, lastDate);
    }
  }

  public static AggregateStats aggregate(Collection<DailyStats> dailyStats) {
    return new AggregateStats(dailyStats);
  }

  public static class DailyStats {
    public final LocalDate day;
    public final int nDay;
    public final int nNight;
    public final Duration longestGapDuration;

    public DailyStats(LocalDate day, ImmutableSortedSet<Interval> gaps, int nDay, int nNight) {
      this(day, new Duration(gaps.last().getStart(), gaps.last().getEnd()), nDay, nNight);
    }

    public DailyStats(BreastfeedingEntry entry) {
      this(entry.mEntryDate, entry.maxGapBetweenFeedings, entry.numDayFeedings, entry.numNightFeedings);
    }

    public DailyStats(LocalDate day, Duration longestGapDuration, int nDay, int nNight) {
      this.day = day;
      this.longestGapDuration = longestGapDuration;
      this.nDay = nDay;
      this.nNight = nNight;
    }
  }

  public Single<ImmutableSortedMap<LocalDate, DailyStats>> dailyStatsFromRepo(String babyName) {
    return mPregnancyRepo.getAll().firstOrError()
        .map(pregnancies -> {
          for (Pregnancy pregnancy : pregnancies) {
            if (pregnancy.babyDaybookName != null && pregnancy.babyDaybookName.equals(babyName)) {
              return Optional.of(pregnancy);
            }
          }
          return Optional.<Pregnancy>empty();
        })
        .flatMap(pregnancy -> {
          if (!pregnancy.isPresent()) {
            return Single.just(ImmutableSortedMap.<LocalDate, DailyStats>naturalOrder().build());
          }
          LocalDate endDate = Optional.ofNullable(pregnancy.get().breastfeedingEndDate).orElse(LocalDate.now());
          return mChartEntryRepo
              .getAllBetween(pregnancy.get().breastfeedingStartDate, endDate)
              .firstOrError()
              .map(entries -> {
                ImmutableSortedMap.Builder<LocalDate, DailyStats> statsBuilder = ImmutableSortedMap.naturalOrder();
                for (ChartEntry entry : entries) {
                  // TODO: this could be better
                  if (entry.breastfeedingEntry.maxGapBetweenFeedings == null) {
                    continue;
                  }
                  if (entry.breastfeedingEntry.numDayFeedings < 0) {
                    continue;
                  }
                  if (entry.breastfeedingEntry.numNightFeedings < 0) {
                    continue;
                  }
                  statsBuilder.put(entry.entryDate, new DailyStats(entry.breastfeedingEntry));
                }
                return statsBuilder.build();
              });
        });
  }

  public Single<ImmutableSortedMap<LocalDate, DailyStats>> dailyStatsFromBabyDaybook(String babyName) {
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
