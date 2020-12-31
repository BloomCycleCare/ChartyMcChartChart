package com.bloomcyclecare.cmcc.ui.babydaybook;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.logic.breastfeeding.BabyDaybookDB;
import com.bloomcyclecare.cmcc.logic.breastfeeding.BreastfeedingStats;
import com.bloomcyclecare.cmcc.ui.init.SplashFragment;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.google.common.base.Strings;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
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

    ChartingApp app = ChartingApp.cast(requireActivity().getApplication());
    RWChartEntryRepo chartEntryRepo = app.entryRepo(ViewMode.CHARTING);

    Disposable d = Single.merge(Single.zip(
        BabyDaybookDB
            .fromIntent(requireActivity().getIntent(), requireContext())
            .doOnSubscribe(d1 -> updateStatus("Parsing data from Baby Daybook"))
            .map(db -> new BreastfeedingStats(db, chartEntryRepo, app.pregnancyRepo(ViewMode.CHARTING))),
        app.pregnancyRepo(ViewMode.CHARTING)
            .getAll()
            .firstOrError()
            .map(pregnancies -> {
              Pregnancy mostRecentPreganancy = null;
              for (Pregnancy p : pregnancies) {
                if (mostRecentPreganancy == null || mostRecentPreganancy.positiveTestDate.isBefore(p.positiveTestDate)) {
                  mostRecentPreganancy = p;
                }
              }
              return Optional.ofNullable(mostRecentPreganancy);
            }),
        (stats, pregnancy) -> {
          if (!pregnancy.isPresent()) {
            return Single.just(Optional.of("No pregnancy, bailing out"));
          }
          if (pregnancy.get().breastfeedingStartDate == null) {
            return Single.just(Optional.of("Breastfeeding not active"));
          }
          if (Strings.isNullOrEmpty(pregnancy.get().babyDaybookName)) {
            return Single.just(Optional.of("Pregnancy had no baby name"));
          }

          updateStatus(String.format("Reading data for %s", pregnancy.get().babyDaybookName));
          LocalDate lastEntryDate = pregnancy.map(p -> p.breastfeedingEndDate).orElse(LocalDate.now());
          return Single.merge(Single.zip(
              stats.dailyStatsFromBabyDaybook(pregnancy.get().babyDaybookName),
              chartEntryRepo
                  .getAllBetween(pregnancy.get().breastfeedingStartDate, lastEntryDate)
                  .firstOrError(),
              (dailyStats, entries) -> {
                List<Completable> actions = new ArrayList<>();
                for (ChartEntry entry : entries) {
                  BreastfeedingStats.DailyStats ds = dailyStats.get(entry.entryDate);
                  boolean entryUpdated = false;
                  if (ds == null) {
                    Timber.d("No daily stats found for %s", entry.entryDate);
                    continue;
                  }
                  if (entry.breastfeedingEntry.numDayFeedings < 0) {
                    entry.breastfeedingEntry.numDayFeedings = ds.nDay;
                    entryUpdated = true;
                  }
                  if (entry.breastfeedingEntry.numNightFeedings < 0) {
                    entry.breastfeedingEntry.numNightFeedings = ds.nNight;
                    entryUpdated = true;
                  }
                  if (entry.breastfeedingEntry.maxGapBetweenFeedings == null) {
                    entry.breastfeedingEntry.maxGapBetweenFeedings = ds.longestGapDuration;
                    entryUpdated = true;
                  }
                  if (entryUpdated) {
                    Timber.v("Updating breastfeeding entry for %s", entry.entryDate);
                    actions.add(chartEntryRepo.insert(entry));
                  }
                }
                BreastfeedingStats.AggregateStats aggregateStats = BreastfeedingStats.aggregate(dailyStats.values());
                Timber.i("Aggregate stats { nDay %f±%f p50 %f, nNight %f±%f p50 %f, maxGap p50 %f p95 %f max %f}",
                    aggregateStats.nDayMean, aggregateStats.nDayInterval, aggregateStats.nDayMedian,
                    aggregateStats.nNightMean, aggregateStats.nNightInterval, aggregateStats.nNightMedian,
                    aggregateStats.maxGapMedian, aggregateStats.maxGapP95, aggregateStats.maxGapP95);

                updateStatus("Updating " + actions.size() + " entries");
                return promptForConfirmation(actions.size())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.computation())
                    .flatMap(canContinue -> {
                      if (!canContinue) {
                        return Single.just(Optional.of("Updates not saved!"));
                      }
                      return Completable.merge(actions).andThen(Single.just(Optional.<String>empty()));
                    });
              }));
        }))
        .subscribe(errorMessage -> {
          if (errorMessage.isPresent()) {
            showError(errorMessage.get());
          } else {
            updateStatus("Success!");
          }
        }, t -> {
          showError("Error importing data!");
          Timber.e(t);
        });
    return view;
  }

  private Single<Boolean> promptForConfirmation(int numberOfUpdates) {
    return Single.create(emitter -> {
      new AlertDialog.Builder(requireContext())
          .setTitle("Confirm Import")
          .setMessage("Would you like to update " + numberOfUpdates + " entries with data from Baby Daybook?")
          .setPositiveButton("Yes", (d,w) -> {
            emitter.onSuccess(true);
            d.dismiss();
          })
          .setNegativeButton("No", (d,w) -> {
            emitter.onSuccess(false);
            d.dismiss();
          })
          .show();
    });
  }
}