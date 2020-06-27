package com.bloomcyclecare.cmcc.features.instructions.ui;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.google.common.collect.ImmutableSet;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class InstructionSelectionViewModel extends AndroidViewModel {

  private static final ImmutableSet<ImmutableSet<BasicInstruction>> EXCLUSIVE_SETS = ImmutableSet.of(
      ImmutableSet.of(BasicInstruction.E_1, BasicInstruction.E_2),
      ImmutableSet.of(BasicInstruction.E_4, BasicInstruction.E_5, BasicInstruction.E_6),
      BasicInstruction.postPeakYellowBasicInstructions);

  private final BehaviorSubject<ViewState> mViewState = BehaviorSubject.create();
  private final BehaviorSubject<Instructions> initialInstructions = BehaviorSubject.create();
  private final Map<BasicInstruction, BehaviorSubject<ToggleState<BasicInstruction>>> instructionStates = new HashMap<>();
  private final Map<SpecialInstruction, BehaviorSubject<ToggleState<SpecialInstruction>>> specialInstructionStates = new HashMap<>();
  private final Map<YellowStampInstruction, BehaviorSubject<ToggleState<YellowStampInstruction>>> yellowStampInstructionStates = new HashMap<>();

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final RWInstructionsRepo mInstructionsRepo;

  public InstructionSelectionViewModel(@NonNull Application application, LocalDate startDate, @Nullable Instructions instructions) {
    super(application);
    mInstructionsRepo = DataRepos.fromApp(application).instructionsRepo(ViewMode.CHARTING);

    for (BasicInstruction basicInstruction : BasicInstruction.values()) {
      instructionStates.put(basicInstruction, BehaviorSubject.create());
    }
    for (SpecialInstruction specialInstruction : SpecialInstruction.values()) {
      specialInstructionStates.put(specialInstruction, BehaviorSubject.create());
    }
    for (YellowStampInstruction yellowStampInstruction : YellowStampInstruction.values()) {
      yellowStampInstructionStates.put(yellowStampInstruction, BehaviorSubject.create());
    }

    if (instructions != null) {
      initialInstructions.onNext(instructions);
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

    // Connect subject for the current ViewState
    viewStateStream(startDate)
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

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  private Flowable<ViewState> viewStateStream(LocalDate startDate) {
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
    return Flowable.combineLatest(
        initialInstructions.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged().doOnNext(v -> Timber.v("New initial instruction")),
        activeStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New activeItemUpdate")),
        activeSpecialStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New special")),
        activeYellowStampStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New yellow stamps")),
        collisionStream.toFlowable(BackpressureStrategy.BUFFER).doOnNext(v -> Timber.v("New collisions")),
        (initialInstructions, activeInstructionSet, activeSpecialInstructionSet, activeYellowStampSet, collisions) -> {
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
          return new ViewState(instructions, initialInstructions, collisionPrompt);
        });
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


  public class ViewState {
    public String collisionPrompt = "";

    public Instructions instructions;
    @Deprecated
    public Instructions initialInstructions;

    private ViewState(Instructions instructions, Instructions initialInstructions, String collisionPrompt) {
      this.instructions = instructions;
      this.initialInstructions = initialInstructions;
      this.collisionPrompt = collisionPrompt;
    }
  }

  private static class ToggleState<T> {
    public final T value;
    final boolean isActive;

    private ToggleState(T value, boolean isActive) {
      this.value = value;
      this.isActive = isActive;
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
      LocalDate startDate = null;
      if (instructions != null) {
        startDate = instructions.startDate;
      }
      return (T) new InstructionSelectionViewModel(application, startDate, instructions);
    }
  }
}
