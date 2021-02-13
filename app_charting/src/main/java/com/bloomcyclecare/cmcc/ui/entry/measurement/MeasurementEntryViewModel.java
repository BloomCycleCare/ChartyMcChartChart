package com.bloomcyclecare.cmcc.ui.entry.measurement;

import android.app.Application;
import android.graphics.Color;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.measurement.LHTestResult;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.measurement.MonitorReading;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class MeasurementEntryViewModel extends ViewModel {

  public final Subject<Integer> monitorReadings = BehaviorSubject.create();
  public final Subject<Integer> lhTestResults = BehaviorSubject.create();

  private final Subject<MeasurementEntry> measurementEntries;
  private final Flowable<ViewState> viewStates;

  public MeasurementEntryViewModel(
      ChartingApp application, MeasurementEntry initialEntry) {
    measurementEntries = BehaviorSubject.createDefault(initialEntry);

    Observable.combineLatest(
        BehaviorSubject.createDefault(initialEntry.mEntryDate),
        monitorReadings
            .map(i -> MonitorReading.values()[i])
            .doOnError(Timber::w)
            .doOnNext(r -> Timber.v("Got new monitor reading: %s", r.name()))
            .onErrorReturnItem(MonitorReading.UNKNOWN),
        lhTestResults
            .map(i -> LHTestResult.values()[i])
            .doOnError(Timber::w)
            .doOnNext(r -> Timber.v("Got new LH test result: %s", r.name()))
            .onErrorReturnItem(LHTestResult.NONE),
        MeasurementEntry::new).subscribe(measurementEntries);

    PreferenceRepo.PreferenceSummary prefSummary = application.preferenceRepo().currentSummary();
    boolean showMonitorReading =
        prefSummary.clearblueMachineMeasurementEnabled() || initialEntry.monitorReading != MonitorReading.UNKNOWN;
    boolean showLHTestResult =
        prefSummary.lhTestMeasurementEnabled() || initialEntry.lhTestResult != LHTestResult.NONE;

    viewStates = measurementEntries
        .map(measurement -> new ViewState(measurement, showMonitorReading, showLHTestResult))
        .toFlowable(BackpressureStrategy.BUFFER);
  }

  Observable<MeasurementEntry> measurements() {
    return measurementEntries;
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(viewStates);
  }

  public static class ViewState {

    private final MeasurementEntry measurementEntry;
    private final boolean showMonitorReading;
    private final boolean showLHTestReading;

    public ViewState(MeasurementEntry measurementEntry, boolean showMonitorReading, boolean showLHTestResult) {
      this.measurementEntry = measurementEntry;
      this.showLHTestReading = showLHTestResult;
      this.showMonitorReading = showMonitorReading;
    }

    public boolean showMonitorReading() {
      return showMonitorReading;
    }

    public boolean showLHTestResult() {
      return showLHTestReading;
    }

    public int lhTestResultPosition() {
      return measurementEntry.lhTestResult.ordinal();
    }

    public int monitorReadingPosition() {
      return measurementEntry.monitorReading.ordinal();
    }

    public int monitorReadingBackgroundColor() {
      switch (monitorReadingPosition()) {
        case 0:
          return R.color.white;
        case 1:
          return R.color.clear_blue_low;
        case 2:
          return R.color.clear_blue_high;
        case 3:
          return R.color.clear_blue_peak;
        default:
          Timber.w("Invalid selection for monitor reading: %d", monitorReadingPosition());
          return R.color.white;
      }
    }

    public int monitorReadingTextColor() {
      if (monitorReadingPosition() < 2) {
        return Color.BLACK;
      } else {
        return Color.WHITE;
      }
    }
  }

  static class Factory implements ViewModelProvider.Factory {

    private final ChartingApp application;
    private final MeasurementEntry initialEntry;

    Factory(Application application, MeasurementEntry initialEntry) {
      this.initialEntry = initialEntry;
      this.application = ChartingApp.cast(application);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MeasurementEntryViewModel(application, initialEntry);
    }
  }
}
