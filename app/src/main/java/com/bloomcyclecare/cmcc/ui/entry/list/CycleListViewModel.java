package com.bloomcyclecare.cmcc.ui.entry.list;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.backup.AppStateExporter;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.util.List;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class CycleListViewModel extends AndroidViewModel {

  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();
  private final Subject<Boolean> mToggles = PublishSubject.create();

  private final Activity mActivity;
  private final MyApplication mApplication;

  public static CycleListViewModel forFragment(Fragment fragment, ViewMode initialViewMode) {
    Factory factory = new Factory(fragment.requireActivity(), initialViewMode);
    return new ViewModelProvider(fragment, factory).get(CycleListViewModel.class);
  }

  public CycleListViewModel(@NonNull Application application, @NonNull Activity activity, @NonNull ViewMode initialViewMode) {
    super(application);
    mApplication = MyApplication.cast(application);
    mActivity = activity;

    Observable<ViewMode> viewModeStream = mToggles
        .scan(initialViewMode, (previousVideMode, toggle) -> {
          if (previousVideMode == ViewMode.CHARTING) {
            return ViewMode.DEMO;
          } else {
            return ViewMode.CHARTING;
          }
        });

    viewModeStream.flatMap(viewMode -> Flowable.merge(Flowable.combineLatest(
        mApplication.instructionsRepo(viewMode).getAll()
            .distinctUntilChanged(),
        mApplication.cycleRepo(viewMode).getStream()
            .distinctUntilChanged(),
        (instructions, cycles) -> Flowable.merge(Flowable
            .fromIterable(cycles)
            .observeOn(Schedulers.computation())
            .parallel()
            .map(cycle -> Flowable.combineLatest(
                mApplication.entryRepo(viewMode).getStreamForCycle(Flowable.just(cycle)),
                mApplication.cycleRepo(viewMode).getPreviousCycle(cycle)
                    .map(Optional::of).defaultIfEmpty(Optional.empty())
                    .toFlowable(),
                (entries, previousCycle) -> new CycleRenderer(cycle, previousCycle, entries, instructions).render())
            )
            .sequential()
            .toList()
            .toFlowable()
            .map(RxUtil::combineLatest))))
        .map(renderableCycles -> ViewState.create(viewMode, renderableCycles))
        .toObservable())
        .subscribe(mViewStateSubject);
  }

  public Flowable<ViewState> viewStateStream() {
    return mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER);
  }

  public ViewMode currentViewMode() {
    return mViewStateSubject.blockingFirst().viewMode();
  }

  @AutoValue
  public static abstract class ViewState {
    public abstract ViewMode viewMode();
    public abstract List<CycleRenderer.RenderableCycle> renderableCycles();

    public static ViewState create(ViewMode viewMode, List<CycleRenderer.RenderableCycle> renderableCycles) {
      return new AutoValue_CycleListViewModel_ViewState(viewMode, renderableCycles);
    }
  }

  public void toggleViewMode() {
    mToggles.onNext(true);
  }

  Single<Intent> export() {
    AppStateExporter exporter = new AppStateExporter(MyApplication.cast(getApplication()));
    return exporter.export()
        .map(appState -> GsonUtil.getGsonInstance().toJson(appState))
        .map(json -> {
          File path = new File(getApplication().getFilesDir(), "tmp/");
          if (!path.exists()) {
            path.mkdir();
          }
          File file = new File(path, "cmcc_export.chart");

          Files.write(json, file, Charsets.UTF_8);

          Uri uri = FileProvider.getUriForFile(
              getApplication(), String.format("%s.fileprovider", getApplication().getPackageName()), file);

          Intent shareIntent = ShareCompat.IntentBuilder.from(mActivity)
              .setSubject("CMCC Export")
              .setEmailTo(null)
              .setType("application/json")
              .setStream(uri)
              .getIntent();
          shareIntent.setData(uri);
          shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          return shareIntent;
        });
  }

  private static class Factory implements ViewModelProvider.Factory {

    private final Activity mActivity;
    private final ViewMode mInitialViewMode;

    public Factory(Activity activity, ViewMode initialViewMode) {
      mActivity = activity;
      mInitialViewMode = initialViewMode;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new CycleListViewModel(mActivity.getApplication(), mActivity, mInitialViewMode);
    }
  }
}
