package com.roamingroths.cmcc.ui.drive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.common.base.Optional;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.repos.ChartRepo;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;
import com.roamingroths.cmcc.logic.print.ChartPrinter;
import com.roamingroths.cmcc.logic.print.PageRenderer;
import com.roamingroths.cmcc.utils.SimpleArrayAdapter;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class DriveActivity extends AppCompatActivity {

  private static final int REQUEST_CODE_SIGN_IN = 1;

  ListView mFilesView;
  TextView mInfoTextView;
  private final CompositeDisposable d = new CompositeDisposable();
  private ChartRepo mChartRepo;
  private SimpleArrayAdapter<File, FileViewHolder> fileAdapater;

  private void hideFiles(String message) {
    Timber.d("Hiding files");
    infoMessages.onNext(message);
    mFilesView.setVisibility(View.GONE);
    mInfoTextView.setVisibility(View.VISIBLE);
  }

  private void showFiles(List<File> files) {
    Timber.d("Showing files");
    mInfoTextView.setVisibility(View.GONE);
    mFilesView.setVisibility(View.VISIBLE);
    fileAdapater.updateData(files);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_drive);

    mInfoTextView = findViewById(R.id.tv_drive_info);
    mFilesView = findViewById(R.id.lv_drive_files);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Charts");

    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
    if (account == null) {
      requestSignIn();
    } else {
      run(account);
    }

    d.add(infoMessages.subscribe(message -> mInfoTextView.setText(message)));

    fileAdapater = new SimpleArrayAdapter<>(
        this, R.layout.list_item_drive_file,
        FileViewHolder::new, t -> {});
    mFilesView.setAdapter(fileAdapater);

    hideFiles("Initializing");
  }

  void run(GoogleSignInAccount googleAccount) {
    Timber.d("Signed in as %s", googleAccount.getEmail());

    // Use the authenticated account to sign in to the Drive service.
    GoogleAccountCredential credential =
        GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_FILE));
    credential.setSelectedAccount(googleAccount.getAccount());
    Drive googleDriveService =
        new Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            new GsonFactory(),
            credential)
            .setApplicationName("Drive API Migration")
            .build();

    mChartRepo = new ChartRepo(googleDriveService);


    d.add(mChartRepo.getOrCreateFolder("My Charts")
        .flatMap(folder -> mChartRepo.getFilesInFolder(folder).toList())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(files -> {
          Timber.i("Found %d files", files.size());
          if (files.isEmpty()) {
            hideFiles("No files to display, click sync");
          } else {
            showFiles(files);
          }
        }, Timber::e));
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_SIGN_IN:
        if (resultCode == Activity.RESULT_OK && data != null) {
          handleSignInResult(data);
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_drive, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    MyApplication myApp = MyApplication.cast(getApplication());

    if (id == R.id.action_sync) {
      updateProgress("Gathering your data", "Starting Sync");
      d.add(myApp.instructionsRepo().getAll().firstOrError().map(instructions -> myApp.cycleRepo()
          .getStream()
          .firstOrError()
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
          .flatMapSingle(cycle -> myApp.entryRepo()
              .getStreamForCycle(Flowable.just(cycle))
              .firstOrError()
              .map(entries -> new CycleRenderer(cycle, entries, instructions))))
          .map(PageRenderer::new)
          .map(pageRenderer -> new ChartPrinter(pageRenderer, null, DriveActivity.this))
          .doOnSuccess(p -> updateProgress("Rendering your charts", "Starting render"))
          .flatMap(ChartPrinter::savePDFs)
          .doOnSuccess(f -> updateProgress("Uploading charts to Drive", String.format("Wrote %d files to cache", f.size())))
          .flatMap(files -> mChartRepo.getOrCreateFolder("My Charts").flatMap(folder -> mChartRepo
              .clearFolder(folder).andThen(Observable.fromIterable(files)
                  .flatMap(localFile -> {
                    int fileIndex = files.indexOf(localFile) + 1;
                    File file = new File();
                    file.setName(String.format("chart_number_%d.pdf", fileIndex));
                    FileContent mediaContent = new FileContent("application/pdf", localFile);
                    return mChartRepo
                        .addFileToFolder(folder, file, mediaContent)
                        .doOnSuccess(f -> localFile.delete())
                        .toObservable();
                  })
                  .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
                  .toList()))
          )
          .doOnSubscribe(d -> hideFiles("Starting sync to Drive"))
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(this::showFiles));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void updateProgress(@Nullable String uiMessage, String logMessage) {
    if (uiMessage != null) {
      infoMessages.onNext(uiMessage);
    }
    Timber.d(logMessage);
  }

  BehaviorSubject<String> infoMessages = BehaviorSubject.createDefault("No message...");

  /**
   * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
   */
  private void requestSignIn() {
    Timber.d("Requesting sign-in");

    GoogleSignInOptions signInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
            .build();
    GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

    // The result of the sign-in Intent is handled in onActivityResult.
    startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
  }

  /**
   * Handles the {@code result} of a completed sign-in activity initiated from {@link
   * #requestSignIn()}.
   */
  private void handleSignInResult(Intent result) {
    GoogleSignIn.getSignedInAccountFromIntent(result)
        .addOnSuccessListener(this::run)
        .addOnFailureListener(exception -> Timber.e(exception, "Unable to sign in."));
  }

  private static class FileViewHolder extends SimpleArrayAdapter.SimpleViewHolder<File> {

    TextView fileNameTextView;

    public FileViewHolder(View view) {
      super(view);
      fileNameTextView = view.findViewById(R.id.tv_file_name);
    }

    @Override
    protected void updateUI(File data) {
      fileNameTextView.setText(data.getName());
    }
  }
}
