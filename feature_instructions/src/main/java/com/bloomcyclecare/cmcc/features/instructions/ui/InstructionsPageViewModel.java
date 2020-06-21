package com.bloomcyclecare.cmcc.features.instructions.ui;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
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

    Flowable.combineLatest(
        instructionsStream,
        isActiveStream,
        (i, isActive) -> i
            .map(value -> new ViewState(DateUtil.toUiStr(value.startDate), isActive ? "current" : "previous"))
            .orElseGet(() -> new ViewState("TBD", "TBD")))
        .toObservable()
        .subscribe(mViewState);

    // Pass any updates to the instructions to the repo
    /*mDisposables.add(mViewState
        .observeOn(Schedulers.computation())
        .distinctUntilChanged()
        .doOnNext(i -> Timber.v("Passing Instructions update to repo"))
        .flatMapCompletable(viewState -> {
          if (viewState.instructions.startDate.equals(viewState.initialInstructions.startDate)) {
            Timber.v("Updating existing instructions.");
            return mInstructionsRepo.insertOrUpdate(viewState.instructions);
          } else {
            Timber.v("Dropping entry for %s", viewState.initialInstructions.startDate);
            Timber.v("Inserting entry for %s", viewState.instructions.startDate);
            //initialInstructions.onNext(viewState.instructions);
            return mInstructionsRepo
                .delete(viewState.initialInstructions)
                .toCompletable()
                .andThen(mInstructionsRepo.insertOrUpdate(viewState.instructions));
          }
        })
        .subscribe());*/
  }

  /*Completable updateStartDate(LocalDate startDate, Function<Set<Instructions>, Single<Boolean>> removeInstructionsPrompt) {
    return mViewState.flatMapCompletable(viewState -> {
      Instructions copyOfInstructionsBeingUpdate = new Instructions(viewState.instructions);
      copyOfInstructionsBeingUpdate.startDate = startDate;
      return mInstructionsRepo
          .getAll()
          .firstOrError()
          .flatMapCompletable(instructions -> {
            Set<Instructions> instructionsToDrop = new HashSet<>();
            if (startDate.isBefore(viewState.instructions.startDate)) {
              List<Instructions> instructionsSortedDesc = new ArrayList<>(instructions);
              Collections.sort(instructionsSortedDesc, (a, b) -> b.startDate.compareTo(a.startDate));
              int instructionsBetween = 0;
              for (Instructions existingInstruction : instructionsSortedDesc) {
                if (existingInstruction.startDate.isAfter(viewState.instructions.startDate)) {
                  continue;
                }
                instructionsBetween++;
                if (existingInstruction.startDate.isAfter(startDate) && instructionsBetween > 1) {
                  instructionsToDrop.add(existingInstruction);
                }
              }
            } else {
              for (Instructions existingInstruction : instructions) {
                if (!existingInstruction.startDate.isAfter(viewState.instructions.startDate)) {
                  continue;
                }
                if (existingInstruction.startDate.isAfter(startDate)) {
                  continue;
                }
                instructionsToDrop.add(existingInstruction);
              }
            }
            Single<Boolean> continueUpdate = Single.just(true);
            if (!instructionsToDrop.isEmpty()) {
              continueUpdate = removeInstructionsPrompt
                  .apply(instructionsToDrop)
                  .flatMap(Single::just);
            }
            return continueUpdate.flatMapCompletable(c -> {
              if (!c) {
                return Completable.complete();
              }
              List<Completable> actions = new ArrayList<>();
              actions.add(mInstructionsRepo.insertOrUpdate(copyOfInstructionsBeingUpdate));
              instructionsToDrop.add(viewState.instructions);
              for (Instructions i : instructionsToDrop) {
                actions.add(mInstructionsRepo.delete(i).toCompletable());
              }
              return Completable.concat(actions);
            }).andThen(Completable.defer(() -> {
              startDateUpdates.onNext(startDate);
              return Completable.complete();
            }));
          });
    });
  }*/

  @Override
  protected void onCleared() {
    mDisposables.dispose();
    super.onCleared();
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }


  public class ViewState {
    public String startDateStr;
    public String statusStr;

    private ViewState(String startDateStr, String statusStr) {
      this.startDateStr = startDateStr;
      this.statusStr = statusStr;
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
