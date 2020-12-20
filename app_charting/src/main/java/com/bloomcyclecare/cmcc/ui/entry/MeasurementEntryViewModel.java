package com.bloomcyclecare.cmcc.ui.entry;

import android.graphics.Color;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.measurement.LHTestResult;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.measurement.MonitorReading;

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

  public MeasurementEntryViewModel(MeasurementEntry initialEntry) {
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

    viewStates = measurementEntries.map(ViewState::new).toFlowable(BackpressureStrategy.BUFFER);
  }

  Observable<MeasurementEntry> measurements() {
    return measurementEntries;
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(viewStates);
  }

  public static class ViewState {

    private final MeasurementEntry measurementEntry;

    public ViewState(MeasurementEntry measurementEntry) {
      this.measurementEntry = measurementEntry;
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

    private final MeasurementEntry initialEntry;

    Factory(MeasurementEntry initialEntry) {
      this.initialEntry = initialEntry;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MeasurementEntryViewModel(initialEntry);
    }
  }
}
