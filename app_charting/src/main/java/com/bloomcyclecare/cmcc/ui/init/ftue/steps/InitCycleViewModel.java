package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.google.auto.value.AutoValue;
import com.stepstone.stepper.VerificationError;

import org.joda.time.LocalDate;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class InitCycleViewModel extends AndroidViewModel {

  private final RWCycleRepo mCycleRepo;
  private final RWPregnancyRepo mPregnancyRepo;
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  final Subject<Integer> mSpinnerSelectionSubject = BehaviorSubject.create();

  public InitCycleViewModel(@NonNull Application application) {
    super(application);
    ChartingApp myApp = ChartingApp.cast(application);
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);
    mPregnancyRepo = myApp.pregnancyRepo(ViewMode.CHARTING);

    Observable.combineLatest(
        mCycleRepo.getStream().toObservable(),
        mSpinnerSelectionSubject.map(i -> InitCycleType.values()[i]),
        (cycles, initCycleType) -> {
          Optional<VerificationError> verificationError = Optional.empty();
          if (cycles.isEmpty()) {
            verificationError = Optional.of(new VerificationError("Cycle required to proceed"));
          }
          return ViewState.create(initCycleType, initCycleType.promptText, initCycleType.dialogTitle, !cycles.isEmpty(), verificationError);
        })
        .subscribe(mViewStateSubject);
  }

  Completable createCycle(LocalDate date) {
    return mSpinnerSelectionSubject.firstOrError()
        .map(i -> InitCycleType.values()[i])
        .flatMapCompletable(initCycleType -> {
          if (initCycleType == InitCycleType.PREGNANT) {
            return mPregnancyRepo.startPregnancy(date);
          }
          Cycle firstCycle = new Cycle("first", date.plusDays(1), null, null);
          return mCycleRepo.insertOrUpdate(firstCycle);
        });
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract InitCycleType initCycleType();
    public abstract String selectionPromptText();
    public abstract String dateDialogTitle();
    public abstract boolean hasCycle();
    public abstract Optional<VerificationError> verificationError();

    public static ViewState create(InitCycleType initCycleType, String selectionPromptText, String dateDialogTitle, boolean hasCycle, Optional<VerificationError> verificationError) {
      return new AutoValue_InitCycleViewModel_ViewState(initCycleType, selectionPromptText, dateDialogTitle, hasCycle, verificationError);
    }

  }
}
