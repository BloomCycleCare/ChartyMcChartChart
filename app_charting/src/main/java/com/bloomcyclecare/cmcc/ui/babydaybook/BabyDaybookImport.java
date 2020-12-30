package com.bloomcyclecare.cmcc.ui.babydaybook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.logic.breastfeeding.BabyDaybookDB;
import com.bloomcyclecare.cmcc.logic.breastfeeding.BreastfeedingStats;

import org.joda.time.LocalDate;

import java.util.Map;

import androidx.fragment.app.Fragment;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BabyDaybookImport#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BabyDaybookImport extends Fragment {

  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_PARAM1 = "param1";
  private static final String ARG_PARAM2 = "param2";

  // TODO: Rename and change types of parameters
  private String mParam1;
  private String mParam2;

  public BabyDaybookImport() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param param1 Parameter 1.
   * @param param2 Parameter 2.
   * @return A new instance of fragment BabyDaybookImport.
   */
  // TODO: Rename and change types and number of parameters
  public static BabyDaybookImport newInstance(String param1, String param2) {
    BabyDaybookImport fragment = new BabyDaybookImport();
    Bundle args = new Bundle();
    args.putString(ARG_PARAM1, param1);
    args.putString(ARG_PARAM2, param2);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Disposable d = BabyDaybookDB.fromIntent(requireActivity().getIntent(), requireContext())
        // Get all the start times
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
        }, Timber::e);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_baby_daybook_import, container, false);
  }
}