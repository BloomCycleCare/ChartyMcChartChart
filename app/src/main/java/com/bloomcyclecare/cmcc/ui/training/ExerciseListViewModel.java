package com.bloomcyclecare.cmcc.ui.training;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.repos.exercise.RWExerciseRepo;
import com.google.auto.value.AutoValue;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class ExerciseListViewModel extends AndroidViewModel {

  private final Subject<ViewState> mViewState = BehaviorSubject.create();
  private final RWExerciseRepo mExerciseRepo;

  public ExerciseListViewModel(@NonNull Application application) {
    super(application);
    MyApplication myApp = MyApplication.cast(application);

    mExerciseRepo = myApp.exerciseRepo(ViewMode.TRAINING);

    mExerciseRepo.getAll().map(ViewState::create).toObservable().subscribe(mViewState);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract List<Exercise> exercises();

    public static ViewState create(List<Exercise> exercises) {
      return new AutoValue_ExerciseListViewModel_ViewState(exercises);
    }
  }
}
