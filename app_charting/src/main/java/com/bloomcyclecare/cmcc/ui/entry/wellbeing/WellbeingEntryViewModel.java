package com.bloomcyclecare.cmcc.ui.entry.wellbeing;

import android.app.Application;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class WellbeingEntryViewModel extends AndroidViewModel {

  public final Subject<Pair<WellbeingEntry.PainObservationTime, Integer>> painUpdateSubject = BehaviorSubject.create();
  public final Subject<Optional<Integer>> energyUpdateSubject = BehaviorSubject.create();

  private final Subject<WellbeingEntry> mUpdatedEntry = BehaviorSubject.create();

  public WellbeingEntryViewModel(@NonNull Application application, @NonNull WellbeingEntry initialEntry) {
    super(application);
    Observable<Map<WellbeingEntry.PainObservationTime, Integer>> painObservationMapStream =
        painUpdateSubject.scan(new HashMap<>(), (map, painObservationTimeIntegerPair) -> {
          WellbeingEntry.PainObservationTime time = painObservationTimeIntegerPair.first;
          Timber.v("Updating pain observation for %s", time);

          Map<WellbeingEntry.PainObservationTime, Integer> copy = new HashMap<>(map);
          Integer observation = painObservationTimeIntegerPair.second;
          if (observation == null) {
            copy.remove(time);
          } else {
            copy.put(time, observation);
          }
          return ImmutableMap.copyOf(copy);
        });

    Observable.combineLatest(
        painObservationMapStream.distinctUntilChanged(),
        energyUpdateSubject.distinctUntilChanged(),
        (painMap, energy) -> {
          WellbeingEntry entry = new WellbeingEntry(initialEntry);
          entry.energyLevel = energy.orElse(null);
          for (WellbeingEntry.PainObservationTime time : WellbeingEntry.PainObservationTime.values()) {
            entry.updatePainObservation(time, painMap.get(time));
          }
          return entry;
        })
        .subscribe(mUpdatedEntry);

    // Seed the update subject with the initial values
    for (WellbeingEntry.PainObservationTime time : WellbeingEntry.PainObservationTime.values()) {
      painUpdateSubject.onNext(Pair.create(time, initialEntry.getPainObservation(time)));
    }
    energyUpdateSubject.onNext(Optional.ofNullable(initialEntry.energyLevel));
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
