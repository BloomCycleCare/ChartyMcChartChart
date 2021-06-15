package com.bloomcyclecare.cmcc.backup;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.backup.models.AppState;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.ROChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.ROPregnancyRepo;
import com.bloomcyclecare.cmcc.data.utils.GsonUtil;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import io.reactivex.Single;

public class AppStateExporter {

  private final ROCycleRepo mCycleRepo;
  private final ROChartEntryRepo mEntryRepo;
  private final ROInstructionsRepo mInstructionsRepo;
  private final ROPregnancyRepo mPregnancyRepo;

  public static AppStateExporter forApp(ChartingApp myApp) {
    return new AppStateExporter(myApp);
  }

  public AppStateExporter(ChartingApp myApp) {
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);
    mEntryRepo = myApp.entryRepo(ViewMode.CHARTING);
    mInstructionsRepo = myApp.instructionsRepo(ViewMode.CHARTING);
    mPregnancyRepo = myApp.pregnancyRepo(ViewMode.CHARTING);
  }

  public Single<AppState> export() {
    return Single.zip(
        mCycleRepo.getStream().firstOrError(),
        mEntryRepo.getAllEntries(),
        mInstructionsRepo.getAll().firstOrError(),
        mPregnancyRepo.getAll().firstOrError(),
        (cycles, entries, instructions, pregnancies) -> new AppState(
            cycles, entries, null, instructions, pregnancies));
  }

  public Single<Intent> getShareIntent(Activity launchingActivity) {
    return export()
        .map(appState -> GsonUtil.getGsonInstance().toJson(appState))
        .map(json -> {
          Application application = launchingActivity.getApplication();
          File path = new File(application.getFilesDir(), "tmp/");
          if (!path.exists()) {
            path.mkdir();
          }
          String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
          File file = new File(path, "cmcc_export_" + date + ".json");

          Files.write(json, file, Charsets.UTF_8);

          Uri uri = FileProvider.getUriForFile(
              application, String.format("%s.fileprovider", application.getPackageName()), file);

          Intent shareIntent = ShareCompat.IntentBuilder.from(launchingActivity)
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
}
