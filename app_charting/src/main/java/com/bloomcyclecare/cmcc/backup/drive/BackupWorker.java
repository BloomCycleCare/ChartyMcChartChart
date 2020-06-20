package com.bloomcyclecare.cmcc.backup.drive;

import android.content.Context;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.backup.AppStateExporter;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.api.client.http.FileContent;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.work.RxWorker;
import androidx.work.WorkerParameters;
import io.reactivex.Single;
import timber.log.Timber;


public class BackupWorker extends RxWorker {

  private static final String BACKUP_FILE_NAME_IN_DRIVE = "backup.chart";
  private static final String LOCAL_BACKUP_FILE_NAME = "local_backup.chart";

  private final Context mContext;
  private final ChartingApp mApp;

  public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
    mApp = ChartingApp.getInstance();
    mContext = context;
  }

  @NonNull
  @Override
  public Single<Result> createWork() {
    Timber.d("Creating work");
    if (!mApp.driveService().hasValue()) {
      return Single.error(new IllegalStateException("Drive service not yet initialized"));
    }
    if (!mApp.driveService().getValue().isPresent()) {
      return Single.error(new IllegalStateException("Drive service not available"));
    }
    DriveServiceHelper driveService = mApp.driveService().getValue().get();

    return AppStateExporter.forApp(mApp).export()
        .flatMapCompletable(appState -> {
          String json = GsonUtil.getGsonInstance().toJson(appState);
          Timber.v("Writing local file");
          File path = new File(mContext.getFilesDir(), "backup/");
          if (!path.exists()) {
            path.mkdir();
          }
          File file = new File(path, LOCAL_BACKUP_FILE_NAME);
          Files.write(json, file, Charsets.UTF_8);
          return driveService
              .getOrCreateFolder("My Charts")
              .flatMap(folder -> {
                Timber.v("Uploading file to Drive");
                com.google.api.services.drive.model.File driveFile =
                    new com.google.api.services.drive.model.File();
                driveFile.setName("backup.chart");
                driveFile.setProperties(appState.properties());
                FileContent mediaContent = new FileContent("application/json", file);
                return driveService
                    .deleteFileFromFolder(folder, BACKUP_FILE_NAME_IN_DRIVE)
                    .andThen(Single.defer(() -> driveService.addFileToFolder(folder, driveFile, mediaContent)));
              })
              .doOnSuccess(driveFile -> file.delete())
              .ignoreElement();
        })
        .toSingleDefault(Result.success());
  }
}
