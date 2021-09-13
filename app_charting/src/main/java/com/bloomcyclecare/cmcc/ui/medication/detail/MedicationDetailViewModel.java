package com.bloomcyclecare.cmcc.ui.medication.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import java.util.Optional;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;


public class MedicationDetailViewModel extends AndroidViewModel {

  @NonNull
  private final Medication mInitialValue;
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  final Subject<String> nameSubject;
  final Subject<String> descriptionSubject;
  final Subject<String> dosageSubject;
  final Subject<String> frequencySubject;
  final Subject<Boolean> activeSubject;

  public MedicationDetailViewModel(@NonNull Application application,
                                   @Nullable Medication initialMedication) {
    super(application);
    mInitialValue = initialMedication == null ? new Medication() : initialMedication;
    nameSubject = BehaviorSubject.createDefault(mInitialValue.name);
    descriptionSubject = BehaviorSubject.createDefault(mInitialValue.description);
    dosageSubject = BehaviorSubject.createDefault(mInitialValue.dosage);
    frequencySubject = BehaviorSubject.createDefault(mInitialValue.frequency);
    activeSubject = BehaviorSubject.createDefault(mInitialValue.active);

    String title = initialMedication == null ? "New Medication" : "Edit Medication";
        Observable.combineLatest(
        nameSubject.distinctUntilChanged(),
        descriptionSubject.distinctUntilChanged(),
        dosageSubject.distinctUntilChanged(),
        frequencySubject.distinctUntilChanged(),
        activeSubject.distinctUntilChanged(),
        (name, description, dosage, frequency, active) -> {
          Medication medication = new Medication();
          medication.name = name;
          medication.description = description;
          medication.dosage = dosage;
          medication.frequency = frequency;
          medication.active = active;
          return medication;
        })
        .map(medication -> new ViewState(title, medication))
        .subscribe(mViewStates);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Single<Boolean> dirty() {
    return mViewStates.firstOrError().map(mInitialValue::equals);
  }

  public Completable save() {
    return mViewStates.firstOrError().ignoreElement();
  }

  public Completable delete() {
    return mViewStates.firstOrError().ignoreElement();
  }

  static class ViewState {
    public final String title;
    public final String subtitle = "";
    public final Medication medication;

    ViewState(String title, Medication medication) {
      this.title = title;
      this.medication = medication;
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
