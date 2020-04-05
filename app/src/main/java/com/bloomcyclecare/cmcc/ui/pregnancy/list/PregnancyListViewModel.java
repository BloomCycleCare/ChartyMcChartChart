package com.bloomcyclecare.cmcc.ui.pregnancy.list;

import android.app.Application;
import android.util.SparseIntArray;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.bloomcyclecare.cmcc.utils.RxPrompt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.Completable;
import io.reactivex.Flowable;

public class PregnancyListViewModel extends AndroidViewModel {

  private final RWPregnancyRepo mPregnancyRepo;
  private final RWCycleRepo mCycleRepo;

  public PregnancyListViewModel(@NonNull Application application) {
    super(application);
    mPregnancyRepo = MyApplication.cast(application).pregnancyRepo();
    mCycleRepo = MyApplication.cast(application).cycleRepo();
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(Flowable.zip(
        mPregnancyRepo.getAll(),
        mCycleRepo.getStream(),
        (pregnancies, cycles) -> {
          Set<Long> pregnancyIndexesAttached = new HashSet<>();
          SparseIntArray cycleIndex = new SparseIntArray();
          for (int i=0; i<cycles.size(); i++) {
            Cycle cycle = cycles.get(i);
            if (cycle.pregnancyId != null) {
              pregnancyIndexesAttached.add(cycle.pregnancyId);
              cycleIndex.put(cycle.pregnancyId.intValue(), i);
            }
          }
          List<RxPrompt> prompts = new ArrayList<>();
          List<PregnancyViewModel> models = new ArrayList<>(pregnancies.size());
          for (int i=0; i<pregnancies.size(); i++) {
            Pregnancy pregnancy = pregnancies.get(i);
            PregnancyViewModel model = new PregnancyViewModel(
                pregnancy,
                i+1,
                Optional.of(cycleIndex.get((int) pregnancy.id)).orElse(-1));
            models.add(model);
            if (!pregnancyIndexesAttached.contains(pregnancy.id)) {
              prompts.add(new RxPrompt(
                  "Found corrupt pregnancy",
                  "Would you like to delete it?",
                  Completable.defer(() -> mPregnancyRepo.delete(pregnancy)),
                  Completable.complete()));
            }
          }
          return new ViewState(models, prompts);
        }));
  }

  public static class ViewState {
    final List<PregnancyViewModel> viewModels;
    final List<RxPrompt> prompts;

    private ViewState(List<PregnancyViewModel> viewModels, List<RxPrompt> prompts) {
      this.viewModels = viewModels;
      this.prompts = prompts;
    }
  }

  public static class Prompt {
    public String title = "Some title";
    public String message = "Some message";
    Completable onPositive = Completable.complete();
    Completable onNegative = Completable.complete();
  }

  public static class PregnancyViewModel {
    final int pregnancyNum;
    final int cycleAscIndex;
    final Pregnancy pregnancy;

    private PregnancyViewModel(Pregnancy pregnancy, int pregnancyNum, int cycleAscIndex) {
      this.pregnancyNum = pregnancyNum;
      this.pregnancy = pregnancy;
      this.cycleAscIndex = cycleAscIndex;
    }

    public String getInfo() {
      return String.format("#%d test date %s", pregnancyNum, DateUtil.toNewUiStr(pregnancy.positiveTestDate));
    }
  }
}
