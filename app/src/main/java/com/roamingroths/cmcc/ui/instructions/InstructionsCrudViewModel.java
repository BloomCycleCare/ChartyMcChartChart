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
import com.roamingroths.cmcc.data.domain.BasicInstruction;
import com.roamingroths.cmcc.data.domain.SpecialInstruction;
import com.roamingroths.cmcc.data.domain.YellowStampInstruction;
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
  private final Map<BasicInstruction, BehaviorSubject<ToggleState<BasicInstruction>>> instructionStates = new HashMap<>();
  private final Map<SpecialInstruction, BehaviorSubject<ToggleState<SpecialInstruction>>> specialInstructionStates = new HashMap<>();
  private final Map<YellowStampInstruction, BehaviorSubject<ToggleState<YellowStampInstruction>>> yellowStampInstructionStates = new HashMap<>();

  private final InstructionsRepo mInstructionsRepo;

  private final BehaviorSubject<ViewState> mViewState = BehaviorSubject.create();
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  public InstructionsCrudViewModel(@NonNull Application application) {
    super(application);
    mInstructionsRepo = MyApplication.cast(application).instructionsRepo();

    for (BasicInstruction basicInstruction : BasicInstruction.values()) {
      instructionStates.put(basicInstruction, BehaviorSubject.create());
    }

    for (SpecialInstruction specialInstruction : SpecialInstruction.values()) {
      specialInstructionStates.put(specialInstruction, BehaviorSubject.create());
    }

    for (YellowStampInstruction yellowStampInstruction : YellowStampInstruction.values()) {
      yellowStampInstructionStates.put(yellowStampInstruction, BehaviorSubject.create());
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
        for (BasicInstruction basicInstruction : BasicInstruction.values()) {
          updateInstruction(basicInstruction, instructions.isActive(basicInstruction));
        }
        for (SpecialInstruction instruction : SpecialInstruction.values()) {
          updateSpecialInstruction(instruction, instructions.isActive(instruction));
        }
        for (YellowStampInstruction instruction : YellowStampInstruction.values()) {
          updateYellowStampInstruction(instruction, instructions.isActive(instruction));
        }
      }
    }
  }

  @Override
  protected void onCleared() {
    mDisposables.dispose();
    super.onCleared();
  }

  void updateInstruction(BasicInstruction basicInstruction, boolean isActive) {
    if (!instructionStates.containsKey(basicInstruction)) {
      Timber.w("No subject for %s", basicInstruction.name());
    }
    instructionStates.get(basicInstruction).onNext(new ToggleState<>(basicInstruction,isActive));
  }

  void updateSpecialInstruction(SpecialInstruction instruction, boolean isActive) {
    if (!specialInstructionStates.containsKey(instruction)) {
      Timber.w("No subject for %s", instruction.name());
    }
    specialInstructionStates.get(instruction).onNext(new ToggleState<>(instruction,isActive));
  }

  void updateYellowStampInstruction(YellowStampInstruction instruction, boolean isActive) {
    if (!yellowStampInstructionStates.containsKey(instruction)) {
      Timber.w("No subject for %s", instruction.name());
    }
    yellowStampInstructionStates.get(instruction).onNext(new ToggleState<>(instruction,isActive));
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  private Flowable<ViewState> viewStateStream() {
    List<Observable<ToggleState<BasicInstruction>>> dedupedActiveUpdates = new ArrayList<>();
    for (BehaviorSubject<ToggleState<BasicInstruction>> state : instructionStates.values()) {
      dedupedActiveUpdates.add(state.distinctUntilChanged());
    }
    Observable<Set<BasicInstruction>> activeStream = Observable.combineLatest(
        dedupedActiveUpdates,
        states -> {
          Set<BasicInstruction> activeBasicInstructions = new HashSet<>();
          for (Object o : states) {
            ToggleState<BasicInstruction> state = (ToggleState<BasicInstruction>) o;
            if (state.isActive) {
              activeBasicInstructions.add(state.value);
            }
          }
          return activeBasicInstructions;
        }).distinctUntilChanged();
    List<Observable<ToggleState<SpecialInstruction>>> dedupedSpecialActiveUpdates = new ArrayList<>();
    for (BehaviorSubject<ToggleState<SpecialInstruction>> state : specialInstructionStates.values()) {
      dedupedSpecialActiveUpdates.add(state.distinctUntilChanged());
    }
    Observable<Set<SpecialInstruction>> activeSpecialStream = Observable.combineLatest(
        dedupedSpecialActiveUpdates,
        states -> {
          Set<SpecialInstruction> activeInstructions = new HashSet<>();
          for (Object o : states) {
            ToggleState<SpecialInstruction> state = (ToggleState<SpecialInstruction>) o;
            if (state.isActive) {
              activeInstructions.add(state.value);
            }
          }
          return activeInstructions;
        }).distinctUntilChanged();
    List<Observable<ToggleState<YellowStampInstruction>>> dedupedYellowStampActiveUpdates = new ArrayList<>();
    for (BehaviorSubject<ToggleState<YellowStampInstruction>> state : yellowStampInstructionStates.values()) {
      dedupedYellowStampActiveUpdates.add(state.distinctUntilChanged());
    }
    Observable<Set<YellowStampInstruction>> activeYellowStampStream = Observable.combineLatest(
        dedupedYellowStampActiveUpdates,
        states -> {
          Set<YellowStampInstruction> activeInstructions = new HashSet<>();
          for (Object o : states) {
            ToggleState<YellowStampInstruction> state = (ToggleState<YellowStampInstruction>) o;
            if (state.isActive) {
              activeInstructions.add(state.value);
            }
          }
          return activeInstructions;
        }).distinctUntilChanged();
    Observable<List<Pair<BasicInstruction, BasicInstruction>>> collisionStream = activeStream
        .map(activeInstructions -> {
          Set<BasicInstruction> entriesToDrop = new HashSet<>();
          List<Pair<BasicInstruction, BasicInstruction>> collisions = new ArrayList<>();
          for (BasicInstruction activeInstruction : activeInstructions) {
            for (Set<BasicInstruction> exclusiveSet : EXCLUSIVE_SETS) {
              if (exclusiveSet.contains(activeInstruction)) {
                for (BasicInstruction exclusiveInstruction : exclusiveSet) {
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
        activeSpecialStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New special")),
        activeYellowStampStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New yellow stamps")),
        collisionStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New collisions")),
        (initialInstructions, startDate, activeInstructionSet, activeSpecialInstructionSet, activeYellowStampSet, collisions) -> mInstructionsRepo
            .hasAnyAfter(startDate)
            .map(hasAnyAfter -> {
              Set<BasicInstruction> instructionsToDeactivate = new HashSet<>();
              for (Pair<BasicInstruction, BasicInstruction> collision : collisions) {
                instructionsToDeactivate.add(collision.second);
              }
              String collisionPrompt = collisions.isEmpty()
                  ? "" : String.format("%s and %s cannot both be active!", collisions.get(0).first, collisions.get(0).second);
              List<BasicInstruction> activeBasicInstructions = new ArrayList<>();
              for (BasicInstruction activeBasicInstruction : activeInstructionSet) {
                if (!instructionsToDeactivate.contains(activeBasicInstruction)) {
                  activeBasicInstructions.add(activeBasicInstruction);
                }
              }
              Instructions instructions = new Instructions(startDate, activeBasicInstructions, new ArrayList<>(activeSpecialInstructionSet), new ArrayList<>(activeYellowStampSet));
              ViewState viewState = new ViewState(instructions, initialInstructions, collisionPrompt);
              viewState.startDateStr = DateUtil.toWireStr(instructions.startDate);
              viewState.statusStr = hasAnyAfter ? "Inactive" : "Active";
              return viewState;
            })
            .toFlowable()));
  }

  private static final ImmutableSet<ImmutableSet<BasicInstruction>> EXCLUSIVE_SETS = ImmutableSet.of(
      ImmutableSet.of(BasicInstruction.E_1, BasicInstruction.E_2),
      ImmutableSet.of(BasicInstruction.E_4, BasicInstruction.E_5, BasicInstruction.E_6),
      BasicInstruction.postPeakYellowBasicInstructions);

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

  private static class ToggleState<T> {
    public final T value;
    public final boolean isActive;

    private ToggleState(T value, boolean isActive) {
      this.value = value;
      this.isActive = isActive;
    }
  }
}
