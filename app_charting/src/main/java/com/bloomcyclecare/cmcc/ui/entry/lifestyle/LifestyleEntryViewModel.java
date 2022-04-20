package com.bloomcyclecare.cmcc.ui.entry.lifestyle;

import android.app.Application;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.lifestyle.LifestyleEntry;
import com.bloomcyclecare.cmcc.ui.entry.breastfeeding.BreastfeedingEntryViewModel;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class LifestyleEntryViewModel extends AndroidViewModel {

  public final Subject<Pair<LifestyleEntry.PainObservationTime, Integer>> painUpdateSubject = BehaviorSubject.create();

  private final Subject<LifestyleEntry> mUpdatedEntry = BehaviorSubject.create();

  public LifestyleEntryViewModel(@NonNull Application application, @NonNull LifestyleEntry initialEntry) {
    super(application);

    // Seed the update subject with the initial values
    for (LifestyleEntry.PainObservationTime time : LifestyleEntry.PainObservationTime.values()) {
      painUpdateSubject.onNext(Pair.create(time, initialEntry.getPainObservation(time)));
    }

    painUpdateSubject.scan(new LifestyleEntry(initialEntry), (entry, painObservationTimeIntegerPair) -> {
      LifestyleEntry.PainObservationTime time = painObservationTimeIntegerPair.first;
      Timber.v("Updating pain observation for %s", time);

      LifestyleEntry copy = new LifestyleEntry(entry);
      copy.updatePainObservation(time, painObservationTimeIntegerPair.second);
      return copy;
    }).subscribe(mUpdatedEntry);
  }

  @NonNull
  public Observable<LifestyleEntry> updatedEntry() {
    return mUpdatedEntry.doOnNext(e -> Timber.v("Emitting new LifestyleEntry"));
  }

  static class Factory implements ViewModelProvider.Factory {

    private final ChartingApp application;
    private final LifestyleEntry initialEntry;

    Factory(Application application, LifestyleEntry initialEntry) {
      this.initialEntry = initialEntry;
      this.application = ChartingApp.cast(application);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new LifestyleEntryViewModel(application, initialEntry);
    }
  }
}
