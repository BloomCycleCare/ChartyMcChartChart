package com.bloomcyclecare.cmcc.backup.drive;

import android.content.Context;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.print.ChartPrinter;
import com.bloomcyclecare.cmcc.logic.print.PageRenderer;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import com.google.common.collect.Range;

import org.joda.time.LocalDate;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.RxWorker;
import androidx.work.WorkerParameters;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class PublishWorker extends RxWorker {

  private final Context mContext;

  private enum Params {
    START_DATE,
    END_DATE
  }

  public static Data createInputData(Range<LocalDate> dateRange) {
    if (!dateRange.hasUpperBound() || !dateRange.hasLowerBound()) {
      throw new IllegalArgumentException();
    }
    return new Data.Builder()
        .putString(Params.START_DATE.name(), DateUtil.toWireStr(dateRange.lowerEndpoint()))
        .putString(Params.END_DATE.name(), DateUtil.toWireStr(dateRange.upperEndpoint()))
        .build();
  }

  public PublishWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
    mContext = context;
  }

  private Single<ChartPrinter> chartPrinter() {
    ChartingApp myApp = ChartingApp.getInstance();
    return Single.merge(Single.zip(
        myApp.instructionsRepo().getAll().firstOrError(),
        myApp.cycleRepo().getStream().firstOrError(),
        (instructions, cycles) -> Observable.fromIterable(cycles)
            /*.filter(cycle -> {
              int daysInCycle = Days.daysBetween(cycle.startDate, Optional.fromNullable(cycle.endDate).or(LocalDate.now())).getDays();
              if (PageRenderer.numRows(daysInCycle) >= 6) {
                Timber.w("Skipping long cycle");
                return false;
              }
              return true;
            })*/
            .sorted((a, b) -> a.startDate.compareTo(b.startDate))
            .flatMapSingle(cycle -> myApp.entryRepo()
                .getStreamForCycle(Flowable.just(cycle))
                .doOnSubscribe(d -> Timber.d("Fetching entries for cycle"))
                .firstOrError()
                .map(entries -> new CycleRenderer(cycle, Optional.empty(), entries, instructions)))
            .toList()
            .map(PageRenderer::new)
            .map(pageRenderer -> new ChartPrinter(pageRenderer, null, mContext))));
  }

  @NonNull
  @Override
  public Single<Result> createWork() {
    Timber.i("Doing work from %s to %s",
        getInputData().getString(Params.START_DATE.name()),
        getInputData().getString(Params.END_DATE.name()));

    // TODO: incremental updates

    ChartingApp myApp = ChartingApp.getInstance();
    if (!myApp.driveService().hasValue() || !myApp.driveService().getValue().isPresent()) {
      Timber.w("Drive service not available");
      return Single.just(Result.failure());
    }
    DriveServiceHelper driveService = myApp.driveService().getValue().get();
    return chartPrinter()
        .doOnSuccess(p -> Timber.d("Saving PDFs"))
        .flatMap(ChartPrinter::savePDFs)
        .observeOn(Schedulers.io())
        .doOnSuccess(charts -> Timber.d("Uploading PDFs to Drive"))
        .flatMap(savedCharts -> driveService.getOrCreateFolder("My Charts").flatMap(folder -> driveService
            .clearFolder(folder, "pdf").andThen(Observable.fromIterable(savedCharts)
                .flatMap(savedChart -> {
                  File file = new File();
                  file.setName(String.format("chart_starting_%s.pdf", DateUtil.toFileStr(savedChart.firstCycle.startDate)));
                  FileContent mediaContent = new FileContent("application/pdf", savedChart.file);
                  return driveService
                      .addFileToFolder(folder, file, mediaContent)
                      .doOnSuccess(f -> savedChart.file.delete())
                      .toObservable();
                })
                .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
                .toList())))
        .flatMap(files -> Single.just(Result.success()))
        .doOnError(t -> Timber.w(t, "Problem with pubish"))
        .subscribeOn(Schedulers.computation())
        ;
  }
}
