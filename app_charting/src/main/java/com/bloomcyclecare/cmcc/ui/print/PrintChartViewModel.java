package com.bloomcyclecare.cmcc.ui.print;

import android.app.Application;
import android.view.View;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.ROChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class PrintChartViewModel extends AndroidViewModel {

  private final ROCycleRepo mCycleRepo;
  private final ROChartEntryRepo mEntryRepo;
  private final ROInstructionsRepo mInstructionsRepo;

  public PrintChartViewModel(@NonNull Application application, @NonNull ViewMode viewMode) {
    super(application);

    DataRepos repos = DataRepos.fromApp(application);
    mCycleRepo = repos.cycleRepo(viewMode);
    mEntryRepo = repos.entryRepo(viewMode);
    mInstructionsRepo = repos.instructionsRepo(viewMode);

  }

  public Single<List<CycleWithEntries>> getCycles() {
    return mCycleRepo.getStream().firstOrError()
        .flatMapObservable(Observable::fromIterable)
        .flatMap(cycle -> mEntryRepo.getStreamForCycle(Flowable.just(cycle))
            .firstOrError()
            .map(entries -> new CycleWithEntries(cycle, entries))
            .toObservable())
        .toList();
  }

  public Observable<CycleRenderer> getRenderers(List<Cycle> cycles) {
    return Observable.fromIterable(cycles)
        .sorted()
        .flatMap(cycle -> Single.zip(
            mEntryRepo.getStreamForCycle(Flowable.just(cycle)).firstOrError(),
            mInstructionsRepo.getAll().firstOrError(),
            (entries, instructions) -> new CycleRenderer(cycle, Optional.empty(), entries, instructions))
            .toObservable());
  }

  public static class Factory implements ViewModelProvider.Factory {

    private final Application application;
    private final ViewMode viewMode;

    public Factory(Application application, ViewMode viewMode) {
      this.application = application;
      this.viewMode = viewMode;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new PrintChartViewModel(application, viewMode);
    }
  }
}
