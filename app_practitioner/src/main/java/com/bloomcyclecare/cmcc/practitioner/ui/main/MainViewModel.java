package com.bloomcyclecare.cmcc.practitioner.ui.main;

import android.app.Application;

import com.google.auto.value.AutoValue;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class MainViewModel extends AndroidViewModel {

  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  public MainViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    public static ViewState create() {
      return new AutoValue_MainViewModel_ViewState();
    }

  }
}
