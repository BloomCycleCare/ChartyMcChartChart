package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
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
  private final Subject<Optional<Cycle>> mCycleSubject = BehaviorSubject.createDefault(Optional.empty());
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  final Subject<Integer> mSpinnerSelectionSubject = BehaviorSubject.create();

  public InitCycleViewModel(@NonNull Application application) {
    super(application);
    MyApplication myApp = MyApplication.cast(application);
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);
    mPregnancyRepo = myApp.pregnancyRepo(ViewMode.CHARTING);

    Observable.combineLatest(
        mCycleSubject.map(Optional::isPresent),
        mSpinnerSelectionSubject,
        (hasCycle, selection) -> {
          String promptText = "";
          String dateDialogTitle = "";
          if (selection == 1) {
            promptText = "Something here saying select the date of the positive pregnancy test";
            dateDialogTitle = "Test Date";
          } else if (selection == 2) {
            promptText = "Something here saying select the delivery date";
            dateDialogTitle = "Delivery Date";
          } else if (selection == 3) {
            promptText = "Something here saying select the first day of your last period";
            dateDialogTitle = "First Day of Last Period";
          }
          Optional<VerificationError> verificationError = Optional.empty();
          if (!hasCycle) {
            verificationError = Optional.of(new VerificationError("Cycle required to proceed"));
          }
          return ViewState.create(selection, promptText, dateDialogTitle, hasCycle, verificationError);
        })
        .subscribe(mViewStateSubject);
  }

  void setCycleDate(LocalDate date) {
    mSpinnerSelectionSubject.firstOrError()
        .flatMapCompletable(selection -> {
          if (selection == 0 || selection > 3) {
            return Completable.error(new IllegalStateException());
          }
          if (selection == 1) {
            return mPregnancyRepo.startPregnancy(date);
          }
          Cycle firstCycle = new Cycle("first", date.plusDays(1), null, null);
          return mCycleRepo.insertOrUpdate(firstCycle);
        })
        .andThen(mCycleRepo.getCurrentCycle())
        .map(Optional::of)
        .toObservable()
        .subscribe(mCycleSubject);
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract int spinnerSelection();
    public abstract String selectionPromptText();
    public abstract String dateDialogTitle();
    public abstract boolean hasCycle();
    public abstract Optional<VerificationError> verificationError();

    public static ViewState create(int spinnerSelection, String selectionPromptText, String dateDialogTitle, boolean hasCycle, Optional<VerificationError> verificationError) {
      return new AutoValue_InitCycleViewModel_ViewState(spinnerSelection, selectionPromptText, dateDialogTitle, hasCycle, verificationError);
    }

  }
}
