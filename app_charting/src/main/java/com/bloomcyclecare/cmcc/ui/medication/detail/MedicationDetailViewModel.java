package com.bloomcyclecare.cmcc.ui.medication.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.repos.medication.RWMedicationRepo;

import java.util.Optional;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;


public class MedicationDetailViewModel extends AndroidViewModel {

  @NonNull
  private final Medication mInitialValue;
  private final RWMedicationRepo mMedicationRepo;
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  final Subject<String> nameSubject;
  final Subject<String> descriptionSubject;
  final Subject<String> dosageSubject;
  final Subject<Boolean> takeMorningSubject;
  final Subject<Boolean> takeNoonSubject;
  final Subject<Boolean> takeEveningSubject;
  final Subject<Boolean> takeNightSubject;
  final Subject<Boolean> takeAsNeededSubject;

  public MedicationDetailViewModel(@NonNull Application application,
                                   @Nullable Medication initialMedication) {
    super(application);
    mMedicationRepo = ChartingApp.cast(application).medicationRepo(ViewMode.CHARTING);
    mInitialValue = initialMedication == null ? new Medication() : initialMedication;
    nameSubject = BehaviorSubject.createDefault(mInitialValue.name);
    descriptionSubject = BehaviorSubject.createDefault(mInitialValue.description);
    dosageSubject = BehaviorSubject.createDefault(mInitialValue.dosage);
    takeMorningSubject = BehaviorSubject.createDefault(mInitialValue.takeInMorning);
    takeNoonSubject = BehaviorSubject.createDefault(mInitialValue.takeAtNoon);
    takeEveningSubject = BehaviorSubject.createDefault(mInitialValue.takeInEvening);
    takeNightSubject = BehaviorSubject.createDefault(mInitialValue.takeAtNight);
    takeAsNeededSubject = BehaviorSubject.createDefault(mInitialValue.takeAsNeeded);

    String title = initialMedication == null ? "New Medication" : "Edit Medication";
    Observable.combineLatest(
        nameSubject.distinctUntilChanged(),
        descriptionSubject.distinctUntilChanged(),
        dosageSubject.distinctUntilChanged(),
        takeMorningSubject.distinctUntilChanged(),
        takeNoonSubject.distinctUntilChanged(),
        takeEveningSubject.distinctUntilChanged(),
        takeNightSubject.distinctUntilChanged(),
        takeAsNeededSubject.distinctUntilChanged(),
        (name, description, dosage, takeMorning, takeNoon, takeEvening, takeNight, takeAsNeeded) -> {
          Medication medication = new Medication(mInitialValue);
          medication.name = name;
          medication.description = description;
          medication.dosage = dosage;
          medication.takeInMorning = takeMorning && !takeAsNeeded;
          medication.takeAtNoon = takeNoon && !takeAsNeeded;
          medication.takeInEvening = takeEvening && !takeAsNeeded;
          medication.takeAtNight = takeNight && !takeAsNeeded;
          medication.takeAsNeeded = takeAsNeeded;
          return medication;
        })
        .map(medication -> new ViewState(title, medication))
        .subscribe(mViewStates);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Single<Boolean> dirty() {
    return mViewStates.firstOrError().map(vs -> vs.dirty(mInitialValue));
  }

  public Completable save() {
    return mViewStates.firstOrError().flatMapCompletable(vs -> {
      if (!vs.dirty(mInitialValue)) {
        Timber.d("Not saving, ViewStats is clean");
        return Completable.complete();
      }
      return mMedicationRepo.save(vs.medication).ignoreElement();
    });
  }

  public Completable delete() {
    return mViewStates.firstOrError().flatMapCompletable(vs -> {
      if (vs.medication.id <= 0) {
        Timber.d("Not deleting, medication hasn't yet been saved");
        return Completable.complete();
      }
      return mMedicationRepo.delete(vs.medication);
    });
  }

  static class ViewState {
    public final String title;
    public final String subtitle = "";
    public final Medication medication;

    ViewState(String title, Medication medication) {
      this.title = title;
      this.medication = medication;
    }

    boolean dirty(Medication medication) {
      return !this.medication.equals(medication);
    }
  }

  static class Factory implements ViewModelProvider.Factory {
    private final Application application;
    private final MedicationDetailFragmentArgs args;

    public Factory(@NonNull Application application, @NonNull MedicationDetailFragmentArgs args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> aClass) {
      Medication medication = Optional.ofNullable(args.getMedication())
          .map(m -> m.medication)
          .orElse(null);
      return (T) new MedicationDetailViewModel(application, medication);
    }
  }
}
