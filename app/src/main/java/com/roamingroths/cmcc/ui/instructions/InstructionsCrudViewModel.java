package com.roamingroths.cmcc.ui.instructions;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.domain.Instruction;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class InstructionsCrudViewModel extends AndroidViewModel {

  final BehaviorSubject<LocalDate> startDateUpdates = BehaviorSubject.create();
  private final BehaviorSubject<Instructions> initialInstructions = BehaviorSubject.create();
  private final Map<Instruction, BehaviorSubject<InstructionState>> instructionStates = new HashMap<>();

  private final InstructionsRepo mInstructionsRepo;

  private final BehaviorSubject<ViewState> mViewState = BehaviorSubject.create();
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  public InstructionsCrudViewModel(@NonNull Application application) {
    super(application);
    mInstructionsRepo = MyApplication.cast(application).instructionsRepo();

    for (Instruction instruction : Instruction.values()) {
      instructionStates.put(instruction, BehaviorSubject.create());
    }

    // Connect subject for the current ViewState
    viewStateStream()
        .toObservable()
        .observeOn(Schedulers.computation())
        .doOnNext(vs -> Timber.v("Storing updated ViewState"))
        .subscribe(mViewState);

    // Pass any updates to the instructions to the repo
    mDisposables.add(mViewState
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
            initialInstructions.onNext(viewState.instructions);
            return mInstructionsRepo
                .delete(viewState.initialInstructions)
                .toCompletable()
                .andThen(mInstructionsRepo.insertOrUpdate(viewState.instructions));
          }
        })
        .subscribe());
  }

  Completable updateStartDate(LocalDate startDate, Function<Set<Instructions>, Single<Boolean>> removeInstructionsPrompt) {
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
  }

  void initialize(@Nullable Bundle args) {
    if (args != null && args.containsKey(Instructions.class.getCanonicalName())) {
      Instructions instructions = Parcels.unwrap(args.getParcelable(Instructions.class.getCanonicalName()));
      if (instructions != null) {
        initialInstructions.onNext(instructions);
        startDateUpdates.onNext(instructions.startDate);
        for (Instruction instruction : Instruction.values()) {
          boolean isActive = instructions.activeItems.contains(instruction);
          updateInstruction(instruction, isActive);
        }
      }
    }
  }

  @Override
  protected void onCleared() {
    mDisposables.dispose();
    super.onCleared();
  }

  void updateInstruction(Instruction instruction, boolean isActive) {
    if (!instructionStates.containsKey(instruction)) {
      Timber.w("No subject for %s", instruction.name());
    }
    instructionStates.get(instruction).onNext(new InstructionState(instruction,isActive));
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  private Flowable<ViewState> viewStateStream() {
    List<Observable<InstructionState>> dedupedActiveUpdates = new ArrayList<>();
    for (BehaviorSubject<InstructionState> state : instructionStates.values()) {
      dedupedActiveUpdates.add(state.distinctUntilChanged());
    }
    Observable<Set<Instruction>> activeStream = Observable.combineLatest(
        dedupedActiveUpdates,
        states -> {
          Set<Instruction> activeInstructions = new HashSet<>();
          for (Object o : states) {
            InstructionState state = (InstructionState) o;
            if (state.isActive) {
              activeInstructions.add(state.instruction);
            }
          }
          return activeInstructions;
        }).distinctUntilChanged();
    Observable<List<Pair<Instruction, Instruction>>> collisionStream = activeStream
        .map(activeInstructions -> {
          Set<Instruction> entriesToDrop = new HashSet<>();
          List<Pair<Instruction, Instruction>> collisions = new ArrayList<>();
          for (Instruction activeInstruction : activeInstructions) {
            for (Set<Instruction> exclusiveSet : EXCLUSIVE_SETS) {
              if (exclusiveSet.contains(activeInstruction)) {
                for (Instruction exclusiveInstruction : exclusiveSet) {
                  if (exclusiveInstruction != activeInstruction
                      && !entriesToDrop.contains(activeInstruction)
                      && activeInstructions.contains(exclusiveInstruction)) {
                    entriesToDrop.add(exclusiveInstruction);
                    collisions.add(Pair.create(activeInstruction, exclusiveInstruction));
                  }
                }
              }
            }
          }
          return collisions;
        });
    return Flowable.merge(Flowable.combineLatest(
        initialInstructions.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged().doOnNext(v -> Timber.v("New initial instruction")),
        startDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged().doOnNext(v -> Timber.v("New startDateUpdate %s", v)),
        activeStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New activeItemUpdate")),
        collisionStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New collisions")),
        (initialInstructions, startDate, activeInstructionSet, collisions) -> mInstructionsRepo
            .hasAnyAfter(startDate)
            .map(hasAnyAfter -> {
              Set<Instruction> instructionsToDeactivate = new HashSet<>();
              for (Pair<Instruction, Instruction> collision : collisions) {
                instructionsToDeactivate.add(collision.second);
              }
              String collisionPrompt = collisions.isEmpty()
                  ? "" : String.format("%s and %s cannot both be active!", collisions.get(0).first, collisions.get(0).second);
              List<Instruction> activeInstructions = new ArrayList<>();
              for (Instruction activeInstruction : activeInstructionSet) {
                if (!instructionsToDeactivate.contains(activeInstruction)) {
                  activeInstructions.add(activeInstruction);
                }
              }
              Instructions instructions = new Instructions(startDate, activeInstructions);
              ViewState viewState = new ViewState(instructions, initialInstructions, collisionPrompt);
              viewState.startDateStr = DateUtil.toWireStr(instructions.startDate);
              viewState.statusStr = hasAnyAfter ? "Inactive" : "Active";
              return viewState;
            })
            .toFlowable()));
  }

  private static final ImmutableSet<ImmutableSet<Instruction>> EXCLUSIVE_SETS = ImmutableSet.of(
      ImmutableSet.of(Instruction.E_1, Instruction.E_2),
      ImmutableSet.of(Instruction.E_4, Instruction.E_5, Instruction.E_6));

  public class ViewState {
    public String startDateStr;
    public String statusStr;
    public String collisionPrompt = "";

    public Instructions instructions;
    @Deprecated
    public Instructions initialInstructions;

    private ViewState(Instructions instructions, Instructions initialInstructions, String collisionPrompt) {
      this.instructions = instructions;
      this.initialInstructions = initialInstructions;
      this.collisionPrompt = collisionPrompt;

      startDateStr = DateUtil.toUiStr(instructions.startDate);
    }
  }

  private static class InstructionState {
    public final Instruction instruction;
    public final boolean isActive;

    private InstructionState(Instruction instruction, boolean isActive) {
      this.instruction = instruction;
      this.isActive = isActive;
    }
  }
}
