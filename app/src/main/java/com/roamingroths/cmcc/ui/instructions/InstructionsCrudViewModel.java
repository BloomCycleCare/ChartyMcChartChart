package com.roamingroths.cmcc.ui.instructions;

import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.utils.DateUtil;

import org.parceler.Parcels;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;

public class InstructionsCrudViewModel extends AndroidViewModel {

  private BehaviorSubject<ViewState> mViewState = BehaviorSubject.create();

  public InstructionsCrudViewModel(@NonNull Application application) {
    super(application);
  }

  void initialize(@Nullable Bundle args) {
    if (args != null && args.containsKey(Instructions.class.getCanonicalName())) {
      Instructions instructions = Parcels.unwrap(args.getParcelable(Instructions.class.getCanonicalName()));
      mViewState.onNext(new ViewState(instructions));
    }
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.DROP));
  }

  public class ViewState {
    public String startDateStr;
    public String endDateStr;

    public Instructions instructions;

    private ViewState(Instructions instructions) {
      this.instructions = instructions;

      startDateStr = DateUtil.toUiStr(instructions.startDate);
      if (instructions.endDate == null) {
        endDateStr = "Ongoing";
      } else {
        endDateStr = DateUtil.toUiStr(instructions.endDate);
      }
    }
  }
}
