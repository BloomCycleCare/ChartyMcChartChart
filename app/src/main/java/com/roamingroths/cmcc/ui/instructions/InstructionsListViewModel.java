package com.roamingroths.cmcc.ui.instructions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;

import java.util.List;

import io.reactivex.subjects.BehaviorSubject;

public class InstructionsListViewModel extends AndroidViewModel {

  private final InstructionsRepo mInstructionsRepo;

  public InstructionsListViewModel(@NonNull Application application) {
    super(application);

    mInstructionsRepo = new InstructionsRepo(MyApplication.cast(application));
  }

  public LiveData<List<Instructions>> instructions() {
    return LiveDataReactiveStreams.fromPublisher(mInstructionsRepo.getAll());
  }

}
