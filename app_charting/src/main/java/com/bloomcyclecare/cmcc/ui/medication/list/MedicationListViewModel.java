package com.bloomcyclecare.cmcc.ui.medication.list;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationWithRelations;
import com.bloomcyclecare.cmcc.data.repos.medication.RWMedicationRepo;

import java.util.Collections;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;


public class MedicationListViewModel extends AndroidViewModel {

  private final RWMedicationRepo mMedicationRepo;
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();


  public MedicationListViewModel(@NonNull Application application) {
    super(application);
    mMedicationRepo = ChartingApp.cast(application).medicationRepo(ViewMode.CHARTING);

    mMedicationRepo.getAllWithRelations(true)
        .map(medications -> {
          Collections.sort(medications);
          return medications;
        })
        .map(ViewState::new)
        .toObservable()
        .subscribe(mViewStates);
  }

  public Completable delete(Medication medication) {
    return mMedicationRepo.delete(medication);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  static class ViewState {
    public final String title = "Your Medications";
    public final String subtitle = "";
    public final List<MedicationWithRelations> medications;

    ViewState(List<MedicationWithRelations> medications) {
      this.medications = medications;
    }
  }
}
