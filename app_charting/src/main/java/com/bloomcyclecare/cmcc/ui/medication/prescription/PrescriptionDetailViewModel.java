package com.bloomcyclecare.cmcc.ui.medication.prescription;

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
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.bloomcyclecare.cmcc.data.repos.medication.RWMedicationRepo;
import com.bloomcyclecare.cmcc.data.repos.prescription.RWPrescriptionRepo;
import com.bloomcyclecare.cmcc.ui.medication.detail.MedicationDetailFragmentArgs;

import org.joda.time.LocalDate;

import java.util.Optional;
import java.util.function.Function;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;


public class PrescriptionDetailViewModel extends AndroidViewModel {

  @Nullable
  private final Prescription mInitialValue;
  private final RWPrescriptionRepo mPrescriptionRepo;
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  final Subject<String> dosageSubject;
  final Subject<Boolean> takeMorningSubject;
  final Subject<Boolean> takeNoonSubject;
  final Subject<Boolean> takeEveningSubject;
  final Subject<Boolean> takeNightSubject;
  final Subject<Boolean> takeAsNeededSubject;
  final Subject<LocalDate> startDateSubject;
  final Subject<Optional<LocalDate>> endDateSubject;

  public static String withDefault(@Nullable Prescription p, Function<Prescription, String> fn, String defaultValue) {
    return Optional.ofNullable(p).map(fn).orElse(defaultValue);
  }

  public static boolean withDefault(@Nullable Prescription p, Function<Prescription, Boolean> fn, boolean defaultValue) {
    return Optional.ofNullable(p).map(fn).orElse(defaultValue);
  }

  public PrescriptionDetailViewModel(@NonNull Application application,
                                     @Nullable Medication initialMedication,
                                     @Nullable Prescription initialPrescription) {
    super(application);
    mPrescriptionRepo = ChartingApp.cast(application).prescriptionRepo(ViewMode.CHARTING);

    mInitialValue = initialPrescription;

    startDateSubject = BehaviorSubject.createDefault(
        Optional.ofNullable(initialPrescription).map(Prescription::startDate).orElse(LocalDate.now()));
    endDateSubject = BehaviorSubject.createDefault(
        Optional.ofNullable(initialPrescription).map(Prescription::endDate));

    dosageSubject = BehaviorSubject.createDefault(
        withDefault(initialPrescription, Prescription::dosage, ""));
    takeMorningSubject = BehaviorSubject.createDefault(
        withDefault(initialPrescription, Prescription::takeInMorning, false));
    takeNoonSubject = BehaviorSubject.createDefault(
        withDefault(initialPrescription, Prescription::takeAtNoon, false));
    takeEveningSubject = BehaviorSubject.createDefault(
        withDefault(initialPrescription, Prescription::takeInEvening, false));
    takeNightSubject = BehaviorSubject.createDefault(
        withDefault(initialPrescription, Prescription::takeAtNight, false));
    takeAsNeededSubject = BehaviorSubject.createDefault(
        withDefault(initialPrescription, Prescription::takeAsNeeded, false));

    String title = initialMedication == null ? "New Prescription" : "Edit Prescription";
    Observable.combineLatest(
        startDateSubject.distinctUntilChanged(),
        endDateSubject.distinctUntilChanged(),
        dosageSubject.distinctUntilChanged(),
        takeMorningSubject.distinctUntilChanged(),
        takeNoonSubject.distinctUntilChanged(),
        takeEveningSubject.distinctUntilChanged(),
        takeNightSubject.distinctUntilChanged(),
        takeAsNeededSubject.distinctUntilChanged(),
        (startDate, endDate, dosage, takeMorning, takeNoon, takeEvening, takeNight, takeAsNeeded) -> {
          Prescription prescription = Prescription.create(
              (int) initialMedication.id(),
              startDate,
              endDate.orElse(null),
              dosage,
              takeMorning && !takeAsNeeded,
              takeNoon && !takeAsNeeded,
              takeEvening && !takeAsNeeded,
              takeNight && !takeAsNeeded,
              takeAsNeeded);
          return new ViewState(title, initialMedication, prescription);
        })
        .subscribe(mViewStates);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  public boolean dirty() {
    return mViewStates.blockingFirst().dirty(mInitialValue);
  }

  public Completable save() {
    return mViewStates.firstOrError().flatMapCompletable(vs -> {
      if (!vs.dirty(mInitialValue)) {
        Timber.d("Not saving, ViewStats is clean");
        return Completable.complete();
      }
      Completable deleteAction = Completable.complete();
      if (mInitialValue != null && !mInitialValue.startDate().equals(vs.prescription.startDate())) {
        deleteAction = mPrescriptionRepo.delete(mInitialValue);
      }
      return deleteAction.andThen(mPrescriptionRepo.save(vs.prescription).ignoreElement());
    });
  }

  public Completable delete() {
    return mViewStates.firstOrError().flatMapCompletable(vs -> {
      if (vs.medication.id() <= 0) {
        Timber.d("Not deleting, medication hasn't yet been saved");
        return Completable.complete();
      }
      return mPrescriptionRepo.delete(vs.prescription);
    });
  }

  static class ViewState {
    public final String title;
    public final String subtitle = "";
    public final Medication medication;
    public final Prescription prescription;

    ViewState(String title, Medication medication, Prescription prescription) {
      this.title = title;
      this.medication = medication;
      this.prescription = prescription;
    }

    boolean dirty(Prescription prescription) {
      return !this.prescription.equals(prescription);
    }
  }

  static class Factory implements ViewModelProvider.Factory {
    private final Application application;
    private final PrescriptionDetailFragmentArgs args;

    public Factory(@NonNull Application application, @NonNull PrescriptionDetailFragmentArgs args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> aClass) {
      Medication medication = Optional.ofNullable(args.getMedication()).orElse(null);
      Prescription prescription = Optional.ofNullable(args.getPrescription()).orElse(null);
      return (T) new PrescriptionDetailViewModel(application, medication, prescription);
    }
  }
}
