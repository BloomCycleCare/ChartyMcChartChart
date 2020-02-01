package com.roamingroths.cmcc.ui.drive;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.services.drive.model.File;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.utils.GoogleAuthActivity;
import com.roamingroths.cmcc.utils.SimpleArrayAdapter;

import java.util.List;

import androidx.lifecycle.ViewModelProviders;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class DriveActivity extends GoogleAuthActivity {

  ListView mFilesView;
  TextView mInfoTextView;
  private final CompositeDisposable d = new CompositeDisposable();
  private SimpleArrayAdapter<File, FileViewHolder> fileAdapater;
  private final Subject<Object> mSyncClicks = BehaviorSubject.create();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_drive);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Charts");

    mInfoTextView = findViewById(R.id.tv_drive_info);
    mFilesView = findViewById(R.id.lv_drive_files);

    fileAdapater = new SimpleArrayAdapter<>(
        this, R.layout.list_item_drive_file,
        FileViewHolder::new, t -> {});
    mFilesView.setAdapter(fileAdapater);

    DriveViewModel viewModel = ViewModelProviders.of(this).get(DriveViewModel.class);
    viewModel.init(mSyncClicks);
    viewModel.viewState().observe(this, viewState -> {
      if (viewState.infoMessage.isPresent()) {
        hideFiles(viewState.infoMessage.get());
      } else {
        showFiles(viewState.files);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_drive, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.action_sync:
        mSyncClicks.onNext(new Object());
        return true;
      default:
        Timber.w("Unsupported menu item!");
    }
    return super.onOptionsItemSelected(item);
  }

  private void hideFiles(String message) {
    Timber.d("Hiding files");
    mFilesView.setVisibility(View.GONE);
    mInfoTextView.setText(message);
    mInfoTextView.setVisibility(View.VISIBLE);
  }

  private void showFiles(List<File> files) {
    Timber.d("Showing files");
    mInfoTextView.setVisibility(View.GONE);
    mFilesView.setVisibility(View.VISIBLE);
    fileAdapater.updateData(files);
  }

  private static class FileViewHolder extends SimpleArrayAdapter.SimpleViewHolder<File> {

    final TextView fileNameTextView;

    FileViewHolder(View view) {
      super(view);
      fileNameTextView = view.findViewById(R.id.tv_file_name);
    }

    @Override
    protected void updateUI(File data) {
      fileNameTextView.setText(data.getName());
    }
  }
}
