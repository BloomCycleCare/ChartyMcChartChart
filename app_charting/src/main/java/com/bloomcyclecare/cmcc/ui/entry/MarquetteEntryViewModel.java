package com.bloomcyclecare.cmcc.ui.entry;

import android.graphics.Color;

import com.bloomcyclecare.cmcc.R;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class MarquetteEntryViewModel extends ViewModel {

  public final Subject<Integer> monitorReadings = BehaviorSubject.create();

  private final Flowable<ViewState> viewStates;

  public MarquetteEntryViewModel() {
    viewStates = monitorReadings.map(ViewState::new).toFlowable(BackpressureStrategy.BUFFER);
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(viewStates);
  }

  public static class ViewState {

    private final int monitorReadingIndex;

    public ViewState(int monitorReadingIndex) {
      this.monitorReadingIndex = monitorReadingIndex;
    }

    public int monitorReadingBackgroundColor() {
      switch (monitorReadingIndex) {
        case 0:
          return R.color.white;
        case 1:
          return R.color.clear_blue_low;
        case 2:
          return R.color.clear_blue_high;
        case 3:
          return R.color.clear_blue_peak;
        default:
          Timber.w("Invalid selection for monitor reading: %d", monitorReadingIndex);
          return R.color.white;
      }
    }

    public int monitorReadingTextColor() {
      if (monitorReadingIndex < 2) {
        return Color.BLACK;
      } else {
        return Color.WHITE;
      }
    }
  }
}
