package com.bloomcyclecare.cmcc.ui.training;

import android.app.Application;

import com.google.auto.value.AutoValue;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class TrainingViewModel extends AndroidViewModel {

  private final Subject<String> mTitle = BehaviorSubject.create();
  private final Subject<String> mSubtitle = BehaviorSubject.create();
  private final Subject<Optional<Fragment>> mFragmentTransition = BehaviorSubject.create();

  private final Subject<ViewState> mViewState = BehaviorSubject.create();

  public TrainingViewModel(@NonNull Application application) {
    super(application);

    Observable.combineLatest(mTitle, mSubtitle, mFragmentTransition, ViewState::create)
        .subscribe(mViewState);

    mTitle.onNext("Training initializing");
    mSubtitle.onNext("please standby");
    //mFragmentTransition.onNext(Optional.empty());
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  void transitionToFragment(Fragment fragment) {
    mFragmentTransition.onNext(Optional.of(fragment));
    //mFragmentTransition.onNext(Optional.empty());
  }

  void completeTransition(String title, String subtitle) {
    mTitle.onNext(title);
    updateSubtitle(subtitle);
  }

  void updateSubtitle(String subtitle) {
    mSubtitle.onNext(subtitle);
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract String title();
    public abstract String subtitle();
    public abstract Optional<Fragment> fragmentTransition();

    public static ViewState create(String title, String subtitle, Optional<Fragment> fragmentTransition) {
      return new AutoValue_TrainingViewModel_ViewState(title, subtitle, fragmentTransition);
    }

  }
}
