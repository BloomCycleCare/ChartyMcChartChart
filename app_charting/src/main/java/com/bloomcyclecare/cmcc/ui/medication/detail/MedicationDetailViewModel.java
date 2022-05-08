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
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.bloomcyclecare.cmcc.data.repos.medication.RWMedicationRepo;
import com.bloomcyclecare.cmcc.data.repos.prescription.RWPrescriptionRepo;
import com.google.common.collect.ImmutableList;

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
  private final Subject<Medication> mInitialValue;
  private final RWMedicationRepo mMedicationRepo;
  private final RWPrescriptionRepo mPrescriptionRepo;
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  final Subject<String> nameSubject;
  final Subject<String> descriptionSubject;

  public MedicationDetailViewModel(@NonNull Application application,
                                   @Nullable Medication initialMedication) {
    super(application);
    mMedicationRepo = ChartingApp.cast(application).medicationRepo(ViewMode.CHARTING);
    mPrescriptionRepo = ChartingApp.cast(application).prescriptionRepo(ViewMode.CHARTING);
    mInitialValue = BehaviorSubject.createDefault(
        initialMedication == null ? Medication.create(0, "", "") : initialMedication);
    nameSubject = BehaviorSubject.createDefault(mInitialValue.blockingFirst().name());
    descriptionSubject = BehaviorSubject.createDefault(mInitialValue.blockingFirst().description());

    Observable.merge(Observable.combineLatest(
        mInitialValue.distinctUntilChanged(),
        nameSubject.distinctUntilChanged(),
        descriptionSubject.distinctUntilChanged(),
        (medication, name, description) -> mPrescriptionRepo.getPrescriptions(medication)
            .map(prescriptions -> new ViewState(
                medication.id() > 0 ? "Edit Medication" : "New Medication",
                Medication.create(medication.id(), name, description),
                prescriptions)))).subscribe(mViewStates);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Medication initialValue() {
    return mInitialValue.blockingFirst();
  }

  public Single<Boolean> dirty() {
    return Single.zip(
        mViewStates.firstOrError(),
        mInitialValue.firstOrError(),
        ViewState::dirty);
  }

  public Single<Medication> save() {
    return dirty().flatMap(isDirty -> {
      if (!isDirty) {
        Timber.d("Not saving, not dirty");
        return mInitialValue.firstOrError();
      }
      return mViewStates.firstOrError()
          .flatMap(viewState -> mMedicationRepo
              .save(viewState.medication)
              .doOnSuccess(mInitialValue::onNext));
    });
  }

  public Completable delete() {
    return mViewStates.firstOrError().flatMapCompletable(vs -> {
      if (vs.medication.id() <= 0) {
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
    public final ImmutableList<Prescription> prescriptions;

    ViewState(String title, Medication medication, ImmutableList<Prescription> prescriptions) {
      this.title = title;
      this.medication = medication;
      this.prescriptions = prescriptions;
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
