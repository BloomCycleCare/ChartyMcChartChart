package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.Application;

import com.google.auto.value.AutoValue;
import com.stepstone.stepper.VerificationError;

import java.util.Map;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class TosStepViewModel extends AndroidViewModel {

  private final StepperViewModel mStepperViewModel;

  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  public TosStepViewModel(@NonNull Application application, StepperViewModel stepperViewModel) {
    super(application);
    mStepperViewModel = stepperViewModel;

    mStepperViewModel.viewStateStream().map(viewState -> {
      int numTosAgreements = TosItem.values().length;
      boolean allAgreed = viewState.tosItems().values().stream().filter(v -> v).count() == numTosAgreements;
      Optional<VerificationError> verificationError =
          allAgreed ? Optional.empty() : Optional.of(new VerificationError("Some terms missing"));
      boolean showButtons = viewState.tosItems().size() == numTosAgreements && verificationError.isPresent();
      return ViewState.create(viewState.tosItems(), showButtons, verificationError);
    }).subscribe(mViewStateSubject);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Single<ViewState> currentViewState() {
    return mViewStateSubject.firstOrError();
  }

  void recordTosAgreement(TosItem tosItem, boolean agreed) {
    mStepperViewModel.recordTosAgreement(tosItem, agreed);
  }

  @AutoValue
  public static abstract class ViewState {
    abstract Map<TosItem, Boolean> tosItems();
    abstract boolean showButtons();
    abstract Optional<VerificationError> verificationError();

    public static ViewState create(Map<TosItem, Boolean> tosItems, boolean showButtons, Optional<VerificationError> verificationError) {
      return new AutoValue_TosStepViewModel_ViewState(tosItems, showButtons, verificationError);
    }
  }

  static class Factory implements ViewModelProvider.Factory {

    private final Application application;
    private final StepperViewModel stepperViewModel;

    Factory(Application application, StepperViewModel stepperViewModel) {
      this.application = application;
      this.stepperViewModel = stepperViewModel;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new TosStepViewModel(application, stepperViewModel);
    }
  }
}
