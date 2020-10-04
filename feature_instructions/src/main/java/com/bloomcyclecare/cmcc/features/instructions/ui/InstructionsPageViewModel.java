package com.bloomcyclecare.cmcc.features.instructions.ui;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.data.serialization.InstructionsSerializer;
import com.bloomcyclecare.cmcc.utils.DateUtil;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

public class InstructionsPageViewModel extends AndroidViewModel {

  private final RWInstructionsRepo mInstructionsRepo;

  private final BehaviorSubject<ViewState> mViewState = BehaviorSubject.create();
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  public InstructionsPageViewModel(@NonNull Application application, Instructions instructions) {
    super(application);
    mInstructionsRepo = DataRepos.fromApp(application).instructionsRepo(ViewMode.CHARTING);

    Flowable<Optional<Instructions>> instructionsStream = Flowable.just(Optional.empty());
    if (instructions != null) {
      instructionsStream = mInstructionsRepo.get(instructions.startDate).map(Optional::of);
    }

    Flowable<Boolean> isActiveStream = instructionsStream.flatMapSingle(i -> {
      if (!i.isPresent()) {
        return Single.just(false);
      }
      return mInstructionsRepo.hasAnyAfter(i.get().startDate).map(v -> !v);
    });

    Flowable<String> summaryStream = instructionsStream.map(i -> {
      if (!i.isPresent()) {
        return "";
      }
      return InstructionsSerializer.encode(i.get(), true);
    });

    Flowable.combineLatest(
        instructionsStream,
        isActiveStream,
        summaryStream,
        (i, isActive, summary) -> i
            .map(value -> new ViewState(DateUtil.toUiStr(value.startDate), isActive ? "current" : "previous", summary))
            .orElseGet(() -> new ViewState("TBD", "TBD", "TBD")))
        .toObservable()
        .subscribe(mViewState);
  }

  @Override
  protected void onCleared() {
    mDisposables.dispose();
    super.onCleared();
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Single<String> currentSummary() {
    return mViewState.firstOrError().map(viewState -> viewState.summaryStr);
  }

  public static class ViewState {
    public String startDateStr;
    public String statusStr;
    public String summaryStr;

    private ViewState(String startDateStr, String statusStr, String summaryStr) {
      this.startDateStr = startDateStr;
      this.statusStr = statusStr;
      this.summaryStr = summaryStr;
    }
  }

  static class Factory implements ViewModelProvider.Factory {
    private final Application application;
    private final Instructions instructions;

    Factory(Application application, Instructions instructions) {
      this.application = application;
      this.instructions = instructions;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new InstructionsPageViewModel(application, instructions);
    }
  }
}
