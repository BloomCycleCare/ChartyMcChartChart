package com.bloomcyclecare.cmcc.features.instructions.ui;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.data.serialization.InstructionsSerializer;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class InstructionsListViewModel extends AndroidViewModel {

  private final BehaviorSubject<ViewState> mViewState = BehaviorSubject.create();
  private final BehaviorSubject<Boolean> mHasStartedNewInsructions = BehaviorSubject.createDefault(false);
  private final BehaviorSubject<Optional<Instructions>> mInstructionsForFocus = BehaviorSubject.createDefault(Optional.absent());
  private final BehaviorSubject<Optional<Instructions>> mFocusedInstructions = BehaviorSubject.createDefault(Optional.absent());

  private final RWInstructionsRepo mInstructionsRepo;
  private final RWCycleRepo mCycleRepo;

  public InstructionsListViewModel(@NonNull Application application) {
    super(application);

    DataRepos repos = DataRepos.fromApp(application);
    mInstructionsRepo = repos.instructionsRepo(ViewMode.CHARTING);
    mCycleRepo = repos.cycleRepo(ViewMode.CHARTING);

    Flowable<List<Instructions>> instructionsStream = mInstructionsRepo
        .getAll()
        .distinctUntilChanged()
        .flatMap(instructions -> {
          if (instructions.isEmpty()) {
            Timber.i("No existing entries, adding one for current cycle.");
            return mCycleRepo
                .getCurrentCycle().toSingle()
                .flatMapPublisher(currentCycle -> {
                  List<Instructions> updatedInstructions = new ArrayList<>(instructions);
                  Instructions newInstructions = new Instructions(currentCycle.startDate, ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
                  updatedInstructions.add(newInstructions);
                  Timber.i("Inserting instructions for cycle beginning %s", currentCycle.startDate);
                  return mInstructionsRepo
                      .insertOrUpdate(newInstructions)
                      .andThen(Flowable.just(updatedInstructions));
                });
          }
          return Flowable.just(instructions);
        })
        .flatMap(instructions -> Flowable
            .fromIterable(instructions)
            .sorted((a, b) -> b.startDate.compareTo(a.startDate))
            .toList()
            .toFlowable())
        .subscribeOn(Schedulers.computation());

    Observable.combineLatest(
        instructionsStream.toObservable(),
        mHasStartedNewInsructions,
        mInstructionsForFocus,
        mFocusedInstructions,
        ViewState::new).subscribe(mViewState);
  }

  Single<Boolean> isDirty() {
    return mInstructionsRepo.isDirty();
  }

  Completable save() {
    return mInstructionsRepo.commit();
  }

  Completable clearPending() {
    return mInstructionsRepo.clearPending();
  }

  void setFocusedInstructions(Instructions instructions) {
    mFocusedInstructions.onNext(Optional.fromNullable(instructions));
  }

  Single<Optional<String>> applyUpdate(@NonNull String text) {
    String sanitizedText = text.replaceAll(" ", "");
    if (Strings.isNullOrEmpty(sanitizedText)) {
      return Single.just(Optional.of("Input required!"));
    } else if (text.length() != 16) {
      return Single.just(Optional.of("Code should be 16 digits long"));
    } else {
      return mFocusedInstructions.firstOrError().flatMap(i -> {
        if (!i.isPresent()) {
          return Single.error(new IllegalStateException("Missing instructions"));
        }
        Instructions decoded = InstructionsSerializer.decode(i.get().startDate, sanitizedText);
        Instructions.DiffResult diffResult = i.get().diff(decoded);
        return Single.just(Optional.absent());
      });
    }
  }


  Completable createNewInstructions(LocalDate newInstructionsStartDate) {
    return Completable.defer(() -> {
      mHasStartedNewInsructions.onNext(true);
      Instructions newInstructions = new Instructions(newInstructionsStartDate, ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
      return mInstructionsRepo.insertOrUpdate(newInstructions);
    });
  }

  Completable addPreviousInstructions(LocalDate newInstructionsStartDate) {
    return Completable.defer(() -> {
      Instructions newInstructions = new Instructions(newInstructionsStartDate, ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
        mInstructionsForFocus.onNext(Optional.of(newInstructions));
        return mInstructionsRepo.insertOrUpdate(newInstructions);
      });
  }

  Completable delete(Instructions instructions) {
    return mInstructionsRepo.delete(instructions).flatMapCompletable(instructionsOut -> {
      if (!instructionsOut.equals(instructions)) {
        mInstructionsForFocus.onNext(Optional.of(instructionsOut));
        if (instructions.startDate.equals(LocalDate.now())) {
          mHasStartedNewInsructions.onNext(false);
        }
      }
      return Completable.complete();
    });
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Single<ViewState> currentViewState() {
    return mViewState.firstOrError();
  }

  public void clearFocus() {
    mInstructionsForFocus.onNext(Optional.absent());
  }

  static class ViewState {
    public final List<Instructions> instructions;
    public final boolean hasStartedNewInstructions;
    @Nullable public final Instructions instructionsForFocus;
    @Nullable public final Instructions focusedInstructions;

    ViewState(List<Instructions> instructions, boolean hasStartedNewInstructions, Optional<Instructions> instructionsForFocus, Optional<Instructions> focusedInstructions) {
      this.instructions = instructions;
      this.hasStartedNewInstructions = hasStartedNewInstructions;
      this.instructionsForFocus = instructionsForFocus.orNull();
      this.focusedInstructions = focusedInstructions.orNull();
    }
  }
}
