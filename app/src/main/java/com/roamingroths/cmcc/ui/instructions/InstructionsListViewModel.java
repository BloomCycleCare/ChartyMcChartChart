package com.roamingroths.cmcc.ui.instructions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.google.common.base.Optional;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class InstructionsListViewModel extends AndroidViewModel {

  private final InstructionsRepo mInstructionsRepo;
  private final CycleRepo mCycleRepo;

  public InstructionsListViewModel(@NonNull Application application) {
    super(application);

    mInstructionsRepo = new InstructionsRepo(MyApplication.cast(application));
    mCycleRepo = new CycleRepo(MyApplication.cast(application).db());
  }

  LiveData<List<Instructions>> instructionsStream() {
    Flowable<List<Instructions>> stream = mInstructionsRepo
        .getAll()
        .flatMap(instructions -> {
          if (instructions.isEmpty()) {
            return mCycleRepo
                .getCurrentCycle().toSingle()
                .flatMapPublisher(currentCycle -> {
                  Instructions newInstructions = new Instructions(
                      currentCycle.startDate, Optional.absent(), new ArrayList<>());
                  instructions.add(newInstructions);
                  return Flowable.just(instructions);
                });
          }
          return Flowable.just(instructions);
        })
        .map(instructions -> {
          Instructions newInstruction = new Instructions(LocalDate.now(), Optional.absent(), new ArrayList<>());
          instructions.add(newInstruction);
          return instructions;
        })
        .flatMap(instructions -> Flowable
            .fromIterable(instructions)
            .sorted((a, b) -> b.startDate.compareTo(a.startDate))
            .toList()
            .toFlowable())
        .subscribeOn(Schedulers.computation());
    return LiveDataReactiveStreams.fromPublisher(stream);
  }
}
