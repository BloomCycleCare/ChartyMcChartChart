package com.bloomcyclecare.cmcc.ui.babydaybook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.logic.breastfeeding.BabyDaybookDB;
import com.bloomcyclecare.cmcc.logic.breastfeeding.BreastfeedingStats;
import com.bloomcyclecare.cmcc.ui.init.SplashFragment;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;

import org.joda.time.LocalDate;

import java.util.Map;

import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class BabyDaybookImport extends SplashFragment {

  public BabyDaybookImport() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    mainViewModel.updateTitle("Baby Daybook Import");

    Disposable d = BabyDaybookDB.fromIntent(requireActivity().getIntent(), requireContext())
        .doOnSubscribe(d1 -> updateStatus("Reading data from Baby Daybook"))
        // Get all the start times
        .doOnSuccess(db -> updateStatus("Generating stats"))
        .map(BreastfeedingStats::new)
        .flatMap(stats -> stats.dailyStats("Gladys"))
        .subscribe(out -> {
          for (Map.Entry<LocalDate, BreastfeedingStats.DailyStats> entry : out.entrySet()) {
            BreastfeedingStats.DailyStats stats = entry.getValue();
            double shortestHours = stats.shortestGapDuration().getStandardMinutes() / 60.0;
            double longestHours = stats.longestGapDuration().getStandardMinutes() / 60.0;
            Timber.i(" \t %s \t %f \t %f \t (between %s and %s) \t %d \t %d",
                entry.getKey(), shortestHours, longestHours,
                stats.gaps.last().getStart(), stats.gaps.last().getEnd(),
                stats.nDay, stats.nNight);
          }
          BreastfeedingStats.AggregateStats aggregateStats = BreastfeedingStats.aggregate(out.values());
          Timber.i("Aggregate stats { nDay %f±%f p50 %f, nNight %f±%f p50 %f, maxGap p50 %f p95 %f max %f}",
              aggregateStats.nDayMean, aggregateStats.nDayInterval, aggregateStats.nDayMedian,
              aggregateStats.nNightMean, aggregateStats.nNightInterval, aggregateStats.nNightMedian,
              aggregateStats.maxGapMedian, aggregateStats.maxGapP95, aggregateStats.maxGapP95);
          updateStatus("Success!");
        }, t -> {
          showError("Error importing data!");
          Timber.e(t);
        });
    return view;
  }
}