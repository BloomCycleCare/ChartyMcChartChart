package com.bloomcyclecare.cmcc.ui.drive;

import android.app.Application;
import android.content.Context;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.drive.DriveServiceHelper;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.google.api.services.drive.model.File;
import com.google.common.base.Optional;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.MaybeSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class DriveViewModel extends AndroidViewModel {

  private static final String FOLDER_NAME = "My Charts";

  private final Context mContext;
  private final PublishSubject<ViewState> mViewState = PublishSubject.create();
  private final MaybeSubject<DriveServiceHelper> mDriveHelper = MaybeSubject.create();
  private final Subject<Object> mSyncClicks = BehaviorSubject.create();

  private final RWInstructionsRepo mInstructionRepo;
  private final RWCycleRepo mCycleRepo;
  private final RWChartEntryRepo mEntryRepo;

  public DriveViewModel(@NonNull Application application) {
    super(application);
    mContext = application.getApplicationContext();

    MyApplication myApp = MyApplication.cast(application);
    mInstructionRepo = myApp.instructionsRepo();
    mCycleRepo = myApp.cycleRepo();
    mEntryRepo = myApp.entryRepo();

    myApp.driveService().filter(Optional::isPresent).map(Optional::get).subscribe(mDriveHelper);

    mDriveHelper.flatMapObservable(driveHelper -> {
      Observable<ViewState> syncResults = mSyncClicks
          .flatMap(c -> {
            Timber.d("Sync clicked");
            Subject<String> updates = PublishSubject.create();
            Single<ViewState> result = Single.error(new UnsupportedOperationException()); //doSync(driveHelper, updates).cache();
            return updates.map(ViewState::message)
                .takeUntil(result.toObservable())
                .concatWith(result.toObservable());
          });
      return loadInitialFiles(driveHelper).toObservable().concatWith(syncResults);
    }).startWith(ViewState.message("Initializing")).subscribe(mViewState);
  }


  void init(Subject<?> syncClicks) {
    syncClicks.subscribe(mSyncClicks);
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  private Single<ViewState> loadInitialFiles(DriveServiceHelper driveHelper) {
    return driveHelper
        .getOrCreateFolder(FOLDER_NAME)
        .flatMap(folder -> driveHelper.getFilesInFolder(folder).toList())
        .map(ViewState::files)
        ;
  }

  /*private Single<ViewState> doSync(DriveServiceHelper driveHelper, Subject<String> updateSubject) {
    return mInstructionRepo.getAll().firstOrError().map(instructions -> mCycleRepo
        .getStream()
        .firstOrError()
        .doOnSuccess(c -> updateSubject.onNext("Gathering your data"))
        .flatMapObservable(Observable::fromIterable)
        .filter(cycle -> {
          int daysInCycle = Days.daysBetween(cycle.startDate, Optional.fromNullable(cycle.endDate).or(LocalDate.now())).getDays();
          if (PageRenderer.numRows(daysInCycle) >= 6) {
            Timber.w("Skipping long cycle");
            return false;
          }
          return true;
        })
        .sorted((a, b) -> a.startDate.compareTo(b.startDate))
        .flatMapSingle(cycle -> mEntryRepo
            .getStreamForCycle(Flowable.just(cycle))
            .firstOrError()
            .flatMap(entries -> Single.just(new CycleRenderer(cycle, entries, instructions))))
        .toList()
        .map(PageRenderer::new)
        .map(pageRenderer -> new ChartPrinter(pageRenderer, null, mContext))
        .doOnSuccess(p -> updateSubject.onNext("Rendering your charts"))
        .flatMap(ChartPrinter::savePDFs)
        .doOnSuccess(f -> updateSubject.onNext("Uploading charts to Drive"))
        .flatMap(savedCharts -> driveHelper.getOrCreateFolder(FOLDER_NAME).flatMap(folder -> driveHelper
            .clearFolder(folder, "pdf").andThen(Observable.fromIterable(savedCharts)
                .flatMap(savedChart -> {
                  File file = new File();
                  file.setName(String.format("chart_starting_%s.pdf", DateUtil.toFileStr(savedChart.firstCycle.startDate)));
                  FileContent mediaContent = new FileContent("application/pdf", savedChart.file);
                  return driveHelper
                      .addFileToFolder(folder, file, mediaContent)
                      .doOnSuccess(f -> savedChart.file.delete())
                      .toObservable();
                })
                .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
                .toList())))
        .map(ViewState::files)
        .doOnSubscribe(d -> updateSubject.onNext("Initiating sync to Drive"))
        ;
  }*/

  public static class ViewState {
    public final List<File> files;
    public final Optional<String> infoMessage;

    private ViewState(List<File> files, Optional<String> infoMessage) {
      this.files = files;
      this.infoMessage = infoMessage;
    }

    static ViewState files(List<File> files) {
      return new ViewState(files, Optional.absent());
    }

    static ViewState message(String message) {
      return new ViewState(Collections.emptyList(), Optional.of(message));
    }
  }
}
