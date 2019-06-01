package com.roamingroths.cmcc.ui.instructions;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.domain.Instruction;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class InstructionsCrudViewModel extends AndroidViewModel {

  final BehaviorSubject<LocalDate> startDateUpdates = BehaviorSubject.create();
  final BehaviorSubject<Optional<LocalDate>> endDateUpdates = BehaviorSubject.create();
  private final Map<Instruction, BehaviorSubject<InstructionState>> instructionStates = new HashMap<>();

  private final BehaviorSubject<ViewState> mViewState = BehaviorSubject.create();

  private final CycleRepo mCycleRepo;
  private final InstructionsRepo mInstructionRepo;

  public InstructionsCrudViewModel(@NonNull Application application) {
    super(application);
    mInstructionRepo = new InstructionsRepo(MyApplication.cast(application));
    mCycleRepo = new CycleRepo(MyApplication.cast(application).db());

    for (Instruction instruction : Instruction.values()) {
      instructionStates.put(instruction, BehaviorSubject.create());
    }
  }

  void initialize(@Nullable Bundle args) {
    if (args != null && args.containsKey(Instructions.class.getCanonicalName())) {
      Instructions instructions = Parcels.unwrap(args.getParcelable(Instructions.class.getCanonicalName()));
      Preconditions.checkNotNull(instructions);
      startDateUpdates.onNext(instructions.startDate);
      endDateUpdates.onNext(Optional.fromNullable(instructions.endDate));
      for (Instruction instruction : Instruction.values()) {
        boolean isActive = instructions.activeItems.contains(instruction);
        updateInstruction(instruction, isActive);
      }
    }
  }

  void updateInstruction(Instruction instruction, boolean isActive) {
    if (!instructionStates.containsKey(instruction)) {
      Timber.w("No subject for %s", instruction.name());
    }
    instructionStates.get(instruction).onNext(new InstructionState(instruction,isActive));
  }

  LiveData<ViewState> viewState() {
    Observable<Set<Instruction>> activeStream = Observable.combineLatest(
        instructionStates.values(),
        states -> {
          Set<Instruction> activeInstructions = new HashSet<>();
          for (Object o : states) {
            InstructionState state = (InstructionState) o;
            if (state.isActive) {
              activeInstructions.add(state.instruction);
            }
          }
          return activeInstructions;
        });
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
    Flowable<ViewState> stream = Flowable.combineLatest(
        startDateUpdates.toFlowable(BackpressureStrategy.BUFFER),
        endDateUpdates.toFlowable(BackpressureStrategy.BUFFER),
        activeStream.toFlowable(BackpressureStrategy.BUFFER),
        collisionStream.toFlowable(BackpressureStrategy.BUFFER),
        (startDate, endDate, activeInstructionSet, collisions) -> {
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
          Instructions instructions = new Instructions(startDate, endDate, activeInstructions);
          ViewState viewState = new ViewState(instructions, collisionPrompt);
          viewState.startDateStr = DateUtil.toWireStr(instructions.startDate);
          viewState.endDateStr = instructions.endDate != null
              ? DateUtil.toWireStr(instructions.endDate) : "Ongoing";
          return viewState;
        });
    return LiveDataReactiveStreams.fromPublisher(stream);
  }

  private static final ImmutableSet<ImmutableSet<Instruction>> EXCLUSIVE_SETS = ImmutableSet.of(
      ImmutableSet.of(Instruction.E_1, Instruction.E_2),
      ImmutableSet.of(Instruction.E_4, Instruction.E_5, Instruction.E_6));

  public class ViewState {
    public String startDateStr;
    public String endDateStr;
    public String collisionPrompt = "";

    public Instructions instructions;

    private ViewState(Instructions instructions, String collisionPrompt) {
      this.instructions = instructions;
      this.collisionPrompt = collisionPrompt;

      startDateStr = DateUtil.toUiStr(instructions.startDate);
      if (instructions.endDate == null) {
        endDateStr = "Ongoing";
      } else {
        endDateStr = DateUtil.toUiStr(instructions.endDate);
      }
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
