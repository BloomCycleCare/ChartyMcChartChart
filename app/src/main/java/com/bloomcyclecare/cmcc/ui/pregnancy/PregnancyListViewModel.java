package com.bloomcyclecare.cmcc.ui.pregnancy;

import android.app.Application;
import android.util.SparseArray;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.CycleRepo;
import com.bloomcyclecare.cmcc.data.repos.PregnancyRepo;
import com.bloomcyclecare.cmcc.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.Flowable;

public class PregnancyListViewModel extends AndroidViewModel {

  private final PregnancyRepo mPregnancyRepo;
  private final CycleRepo mCycleRepo;

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
          SparseArray<Integer> cycleIndex = new SparseArray<>();
          for (int i=0; i<cycles.size(); i++) {
            Cycle cycle = cycles.get(i);
            if (cycle.pregnancyId != null) {
              cycleIndex.put(cycle.pregnancyId.intValue(), i);
            }
          }
          List<PregnancyViewModel> out = new ArrayList<>(pregnancies.size());
          for (int i=0; i<pregnancies.size(); i++) {
            Pregnancy pregnancy = pregnancies.get(i);
            out.add(new PregnancyViewModel(
                pregnancy,
                i+1,
                Optional.ofNullable(cycleIndex.get((int) pregnancy.id)).orElse(-1)));
          }
          return out;
        })
        .map(ViewState::new));
  }

  public static class ViewState {
    final List<PregnancyViewModel> viewModels;

    private ViewState(List<PregnancyViewModel> viewModels) {
      this.viewModels = viewModels;
    }
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
      return String.format("#%d test date %s", pregnancyNum, DateUtil.toUiStr(pregnancy.positiveTestDate));
    }
  }
}
