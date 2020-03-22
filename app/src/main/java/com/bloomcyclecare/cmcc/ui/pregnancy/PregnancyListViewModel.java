package com.bloomcyclecare.cmcc.ui.pregnancy;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.PregnancyRepo;
import com.bloomcyclecare.cmcc.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

public class PregnancyListViewModel extends AndroidViewModel {

  private final PregnancyRepo mPregnancyRepo;

  public PregnancyListViewModel(@NonNull Application application) {
    super(application);
    mPregnancyRepo = MyApplication.cast(application).pregnancyRepo();
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mPregnancyRepo.getAll()
        .map(pregnancies -> {
          List<PregnancyViewModel> out = new ArrayList<>(pregnancies.size());
          for (int i=0; i<pregnancies.size(); i++) {
            out.add(new PregnancyViewModel(i+1, pregnancies.get(i)));
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
    final Pregnancy pregnancy;

    private PregnancyViewModel(int pregnancyNum, Pregnancy pregnancy) {
      this.pregnancyNum = pregnancyNum;
      this.pregnancy = pregnancy;
    }

    public String getInfo() {
      return String.format("#%d test date %s", pregnancyNum, DateUtil.toUiStr(pregnancy.positiveTestDate));
    }
  }
}
