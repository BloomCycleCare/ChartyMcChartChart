package com.roamingroths.cmcc.logic.drive;

import android.content.Context;

import com.google.api.client.http.FileContent;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.backup.AppStateExporter;
import com.roamingroths.cmcc.data.drive.DriveServiceHelper;
import com.roamingroths.cmcc.utils.GsonUtil;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.work.RxWorker;
import androidx.work.WorkerParameters;
import io.reactivex.Single;
import timber.log.Timber;


public class BackupWorker extends RxWorker {

  private final Context mContext;
  private final MyApplication mApp;

  public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
    mApp = MyApplication.getInstance();
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
        .map(appState -> GsonUtil.getGsonInstance().toJson(appState))
        .map(json -> {
          Timber.v("Writing local file");
          File path = new File(mContext.getFilesDir(), "backup/");
          if (!path.exists()) {
            path.mkdir();
          }
          File file = new File(path, "cmcc_export.chart");
          Files.write(json, file, Charsets.UTF_8);
          return file;
        })
        .flatMapCompletable(file -> driveService
            .getOrCreateFolder("My Charts")
            .flatMap(folder -> {
              Timber.v("Uploading file to Drive");
              com.google.api.services.drive.model.File driveFile =
                  new com.google.api.services.drive.model.File();
              driveFile.setName("backup.chart");
              FileContent mediaContent = new FileContent("application/json", file);
              return driveService
                  .addFileToFolder(folder, driveFile, mediaContent);
            })
            .ignoreElement())
        .toSingleDefault(Result.success());
  }
}
