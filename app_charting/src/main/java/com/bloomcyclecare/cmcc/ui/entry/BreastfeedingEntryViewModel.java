package com.bloomcyclecare.cmcc.ui.entry;

import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;

import org.joda.time.Duration;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class BreastfeedingEntryViewModel extends AndroidViewModel {

  final Subject<Optional<Integer>> numNightFeedingsSubject = BehaviorSubject.create();
  final Subject<Optional<Integer>> numDadyFeedingsSubject = BehaviorSubject.create();
  final Subject<Optional<Duration>> longestFeedingGapHours = BehaviorSubject.create();

  private final Subject<BreastfeedingEntry> mEntriesSubject = BehaviorSubject.create();
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  public BreastfeedingEntryViewModel(
      @NonNull Application application, @NonNull BreastfeedingEntry initialEntry) {
    super(application);
    mEntriesSubject.onNext(initialEntry);

    numNightFeedingsSubject.onNext(initialEntry.numDayFeedings < 0 ? Optional.empty() : Optional.of(initialEntry.numNightFeedings));
    numDadyFeedingsSubject.onNext(initialEntry.numDayFeedings < 0 ? Optional.empty() : Optional.of(initialEntry.numDayFeedings));
    longestFeedingGapHours.onNext(Optional.ofNullable(initialEntry.maxGapBetweenFeedings));

    Observable.combineLatest(
        numNightFeedingsSubject.distinctUntilChanged(),
        numDadyFeedingsSubject.distinctUntilChanged(),
        longestFeedingGapHours.distinctUntilChanged(),
        (numNightFeedings, numDayFeedings, longestGapBetweenFeedings) -> new BreastfeedingEntry(
            initialEntry.mEntryDate,
            numDayFeedings.orElse(-1),
            numNightFeedings.orElse(-1),
            longestGapBetweenFeedings.orElse(null)))
        .doOnNext(e -> Timber.v("Got new BreastfeedingEntry"))
        .subscribe(mEntriesSubject);

    mEntriesSubject
        .map(ViewState::new)
        .doOnNext(vs -> Timber.v("Emitting new ViewState"))
        .subscribe(mViewStates);
  }

  Observable<BreastfeedingEntry> entries() {
    return mEntriesSubject;
  }

  LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  static class ViewState {
    final BreastfeedingEntry entry;

    ViewState(BreastfeedingEntry entry) {
      this.entry = entry;
    }
  }

  static class Factory implements ViewModelProvider.Factory {

    private final ChartingApp application;
    private final BreastfeedingEntry initialEntry;

    Factory(Application application, BreastfeedingEntry initialEntry) {
      this.initialEntry = initialEntry;
      this.application = ChartingApp.cast(application);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new BreastfeedingEntryViewModel(application, initialEntry);
    }
  }
}
