package com.bloomcyclecare.cmcc.backup.drive;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.RxWorker;
import androidx.work.WorkerParameters;

import com.bloomcyclecare.cmcc.BuildConfig;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.backup.AppStateExporter;
import com.bloomcyclecare.cmcc.data.utils.GsonUtil;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.api.client.http.FileContent;
import com.google.common.base.Charsets;
import com.google.common.collect.Range;
import com.google.common.io.Files;

import org.joda.time.LocalDate;

import java.io.File;

import io.reactivex.Single;
import timber.log.Timber;


public class BackupWorker extends RxWorker {

  public static final String BACKUP_DIRECTORY_NAME_IN_DRIVE = "My Charts";
  private static final String LOCAL_BACKUP_FILE_NAME = "local_cmcc_backup.json";

  private final Context mContext;
  private final ChartingApp mApp;

  public static OneTimeWorkRequest forDateRange(Range<LocalDate> dateRange) {
    return new OneTimeWorkRequest.Builder(BackupWorker.class)
        .build();
  }

  public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
    mApp = ChartingApp.getInstance();
    mContext = context;
  }

  public static String filename() {
    return BuildConfig.DEBUG ? "cmcc_debug_backup.json" : "cmcc_backup.json";
  }

  @NonNull
  @Override
  public Single<Result> createWork() {
    Timber.d("Creating work");
    return GoogleAuthHelper.googleAccount(mContext)
        .map(account -> DriveServiceHelper.forAccount(account, mContext))
        .flatMapSingle(driveService -> AppStateExporter.forApp(mApp).export()
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
                  .getOrCreateFolder(BACKUP_DIRECTORY_NAME_IN_DRIVE)
                  .flatMap(folder -> {
                    Timber.v("Uploading file to Drive");
                    com.google.api.services.drive.model.File driveFile =
                        new com.google.api.services.drive.model.File();
                    driveFile.setName(filename());
                    driveFile.setProperties(appState.properties());
                    FileContent mediaContent = new FileContent("application/json", file);
                    return driveService
                        .deleteFileFromFolder(folder, filename())
                        .andThen(Single.defer(() -> driveService.addFileToFolder(folder, driveFile, mediaContent)));
                  })
                  .doOnSuccess(driveFile -> file.delete())
                  .ignoreElement();
            })
            .toSingleDefault(Result.success()));
  }
}
