package com.bloomcyclecare.cmcc.ui.entry.wellbeing;

import android.app.Application;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class WellbeingEntryViewModel extends AndroidViewModel {

  public final Subject<Pair<WellbeingEntry.PainObservationTime, Integer>> painUpdateSubject = BehaviorSubject.create();

  private final Subject<WellbeingEntry> mUpdatedEntry = BehaviorSubject.create();

  public WellbeingEntryViewModel(@NonNull Application application, @NonNull WellbeingEntry initialEntry) {
    super(application);

    // Seed the update subject with the initial values
    for (WellbeingEntry.PainObservationTime time : WellbeingEntry.PainObservationTime.values()) {
      painUpdateSubject.onNext(Pair.create(time, initialEntry.getPainObservation(time)));
    }

    painUpdateSubject.scan(new WellbeingEntry(initialEntry), (entry, painObservationTimeIntegerPair) -> {
      WellbeingEntry.PainObservationTime time = painObservationTimeIntegerPair.first;
      Timber.v("Updating pain observation for %s", time);

      WellbeingEntry copy = new WellbeingEntry(entry);
      copy.updatePainObservation(time, painObservationTimeIntegerPair.second);
      return copy;
    }).subscribe(mUpdatedEntry);
  }

  @NonNull
  public Observable<WellbeingEntry> updatedEntry() {
    return mUpdatedEntry.doOnNext(e -> Timber.v("Emitting new LifestyleEntry"));
  }

  static class Factory implements ViewModelProvider.Factory {

    private final ChartingApp application;
    private final WellbeingEntry initialEntry;

    Factory(Application application, WellbeingEntry initialEntry) {
      this.initialEntry = initialEntry;
      this.application = ChartingApp.cast(application);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new WellbeingEntryViewModel(application, initialEntry);
    }
  }
}
