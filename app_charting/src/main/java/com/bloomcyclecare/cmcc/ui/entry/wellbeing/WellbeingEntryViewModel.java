package com.bloomcyclecare.cmcc.ui.entry.wellbeing;

import android.app.Application;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationRef;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntryWithRelations;
import com.bloomcyclecare.cmcc.data.repos.medication.ROMedicationRepo;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class WellbeingEntryViewModel extends AndroidViewModel {

  public final Subject<Pair<WellbeingEntry.PainObservationTime, Integer>> painUpdateSubject = BehaviorSubject.create();
  public final Subject<Optional<Integer>> energyUpdateSubject = BehaviorSubject.create();
  public final Subject<MedicationUpdate> medicationUpdateSubject = BehaviorSubject.create();

  private final Subject<WellbeingEntryWithRelations> mUpdatedEntry = BehaviorSubject.create();
  private final ROMedicationRepo mMedicationRepo;

  public static class MedicationUpdate {
    public final Medication medication;
    public final Medication.TimeOfDay time;
    public final boolean checked;

    public MedicationUpdate(Medication medication, Medication.TimeOfDay time, boolean checked) {
      this.medication = medication;
      this.time = time;
      this.checked = checked;
    }
  }

  private static Map<Medication, Map<Medication.TimeOfDay, Boolean>> initMap(Collection<Medication> medications) {
    ImmutableMap.Builder<Medication, Map<Medication.TimeOfDay, Boolean>> builder = ImmutableMap.builder();
    medications.forEach(medication -> {
      ImmutableMap.Builder<Medication.TimeOfDay, Boolean> innerBuilder = ImmutableMap.builder();
      for (Medication.TimeOfDay timeOfDay : Medication.TimeOfDay.values()) {
        if (medication.shouldTake(timeOfDay)) {
          innerBuilder.put(timeOfDay, false);
        }
      }
      builder.put(medication, innerBuilder.build());
    });
    return builder.build();
  }

  public WellbeingEntryViewModel(@NonNull Application application, @NonNull WellbeingEntryWithRelations initialEntry) {
    super(application);
    mMedicationRepo = ChartingApp.cast(application).medicationRepo(ViewMode.CHARTING);

    Set<Medication> medications = new HashSet<>(mMedicationRepo.getAll(false).blockingFirst());

    Observable<List<MedicationRef>> medicationStream = medicationUpdateSubject.scan(initMap(medications), (map, update) -> {
      Timber.v("Updating medications %s %s %b", update.medication.name, update.time.name(), update.checked);
      if (!medications.contains(update.medication)) {
        throw new IllegalArgumentException("Update for unknown medications: " + update.medication.name);
      }
      ImmutableMap.Builder<Medication, Map<Medication.TimeOfDay, Boolean>> copyBuilder = ImmutableMap.builder();
      map.forEach((medication, timeMap) -> {
        if (!medication.equals(update.medication)) {
          copyBuilder.put(medication, timeMap);
        } else {
          ImmutableMap.Builder<Medication.TimeOfDay, Boolean> timeMapBuilder = ImmutableMap.builder();
          timeMap.forEach((time, value) -> {
            if (time != update.time) {
              timeMapBuilder.put(time, value);
            } else {
              timeMapBuilder.put(time, update.checked);
            }
          });
          copyBuilder.put(medication, timeMapBuilder.build());
        }
      });
      return copyBuilder.build();
    })
    .doOnNext(WellbeingEntryViewModel::logMedications)
    .map(map -> {
      List<MedicationRef> refs = new ArrayList<>();
      map.forEach((medication, timeMap) -> timeMap.forEach((time, value) -> {
        if (value) {
          refs.add(MedicationRef.create(initialEntry.wellbeingEntry, medication, time));
        }
      }));
      return refs;
    });

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
        medicationStream.distinctUntilChanged(),
        (painMap, energy, medicationRefs) -> {
          WellbeingEntry entry = new WellbeingEntry(initialEntry.wellbeingEntry);
          entry.energyLevel = energy.orElse(null);
          for (WellbeingEntry.PainObservationTime time : WellbeingEntry.PainObservationTime.values()) {
            entry.updatePainObservation(time, painMap.get(time));
          }
          WellbeingEntryWithRelations entryWithRelations = new WellbeingEntryWithRelations();
          entryWithRelations.wellbeingEntry = entry;
          entryWithRelations.medicationRefs = medicationRefs;
          return entryWithRelations;
        })
        .subscribe(mUpdatedEntry);

    // Seed the update subject with the initial values
    for (WellbeingEntry.PainObservationTime time : WellbeingEntry.PainObservationTime.values()) {
      painUpdateSubject.onNext(Pair.create(time, initialEntry.wellbeingEntry.getPainObservation(time)));
    }
    energyUpdateSubject.onNext(Optional.ofNullable(initialEntry.wellbeingEntry.energyLevel));

    Map<Long, Medication> medicationIndex = new HashMap<>();
    medications.forEach(medication -> medicationIndex.put(medication.id, medication));
    initialEntry.medicationRefs.forEach(ref -> {
      Medication medication = medicationIndex.get((long) ref.medicationId);
      medicationUpdateSubject.onNext(new MedicationUpdate(medication, ref.time, true));
    });
  }

  private static void logMedications(Map<Medication, Map<Medication.TimeOfDay, Boolean>> map) {
    List<String> lines = new ArrayList<>();
    map.forEach((medication, timeMap) -> {
      List<String> parts = new ArrayList<>();
      timeMap.forEach((time, value) -> parts.add(String.format("%s:%b", time.name(), value)));
      lines.add(String.format("%s [%s]", medication.name, Joiner.on(", ").join(parts)));
    });
    Timber.d("Medication: %s", Joiner.on(", ").join(lines));
  }

  public Observable<List<Medication>> medications() {
    return mMedicationRepo.getAll(false).toObservable();
  }

  @NonNull
  public Observable<WellbeingEntryWithRelations> updatedEntry() {
    return mUpdatedEntry.doOnNext(e -> Timber.v("Emitting new WellbeingEntryWithRelations"));
  }

  static class Factory implements ViewModelProvider.Factory {

    private final ChartingApp application;
    private final WellbeingEntryWithRelations initialEntry;

    Factory(Application application, WellbeingEntryWithRelations initialEntry) {
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
