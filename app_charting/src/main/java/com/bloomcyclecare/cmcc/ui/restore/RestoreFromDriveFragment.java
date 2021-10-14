package com.bloomcyclecare.cmcc.ui.restore;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.File;

import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class RestoreFromDriveFragment extends Fragment {

  CompositeDisposable mDisposables = new CompositeDisposable();
  RestoreFromDriveViewModel mViewModel;

  TextView mProgressTextView;

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    Task<GoogleSignInAccount> a = GoogleSignIn.getSignedInAccountFromIntent(data);
    try {
      GoogleSignInAccount aa = a.getResult();
      Timber.i("Got %s", aa.getDisplayName());
      mViewModel.checkAccount();
    } catch (Exception e) {
      Timber.e(e);
      mProgressTextView.setText("Error signing ino Google account");
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_drive_restore, container, false);

    RestoreFromDriveViewModel.Factory factory = new RestoreFromDriveViewModel.Factory(requireActivity());
    mViewModel = new ViewModelProvider(this, factory).get(RestoreFromDriveViewModel.class);

    mProgressTextView = view.findViewById(R.id.tv_progress);
    ProgressBar mProgressBar = view.findViewById(R.id.progressBar);
    mProgressBar.setVisibility(View.GONE);

    mProgressTextView.setText("Checking for Google account");
    mViewModel.checkAccount();
    mViewModel.viewState().observeForever(viewState -> {
      if (viewState.noneFound()) {
        mProgressBar.setVisibility(View.GONE);
        mProgressTextView.setText("No backup found in Drive");
        Timber.i("No backup file found");
        if (!viewState.account().isPresent()) {
          Timber.w("Account missing for NOT_FOUND restore from backup");
        }
        String accountEmail = viewState.account()
            .map(GoogleSignInAccount::getEmail)
            .orElse("IDK, this is a bug");
        new AlertDialog.Builder(requireContext())
            .setTitle("No File Found")
            .setMessage("We were not able to find any previous backups for " + accountEmail)
            .setPositiveButton("OK", (dialog, which) -> {
              Navigation.findNavController(requireView()).popBackStack();
              dialog.dismiss();
            })
            .setNegativeButton("Switch Accounts", ((dialog, which) -> {
              mProgressTextView.setText("Switching Google accounts");
              mDisposables.add(mViewModel.switchAccount()
                  .subscribe(intent -> {
                    startActivityForResult(intent, 1);
                  }, t -> {
                    dialog.dismiss();
                    Timber.e(t, "Error switching accounts for restore from drive");
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Failed to switch accounts")
                        .setMessage("Something went wrong")
                        .setPositiveButton("OK", (d, w) -> {
                          Navigation.findNavController(requireView()).popBackStack();
                          d.dismiss();
                        })
                        .show();
                  }));
            }))
            .show();
        return;
      }
      mProgressBar.setVisibility(View.VISIBLE);
      if (!viewState.account().isPresent()) {
        mProgressTextView.setText("Prompting for Google account sign in");
        Timber.d("Prompting for sign in");
        startActivityForResult(GoogleAuthHelper.getPromptIntent(requireContext()), 1);
        return;
      }
      if (viewState.backupFile().isPresent()) {
        mProgressTextView.setText("Found file in Drive");
        mDisposables.add(promptRestoreFromDrive(viewState.backupFile().get())
            .flatMap(doRestore -> {
              if (!doRestore) {
                mProgressTextView.setText("Restore cancelled");
                return Single.just(false);
              }
              mProgressTextView.setText("Restoring data");
              return mViewModel.restore(viewState.backupFile().get()).andThen(Single.just(true))
                  .observeOn(AndroidSchedulers.mainThread())
                  .doOnTerminate(() -> mProgressBar.setVisibility(View.GONE));
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(restored -> {
              if (restored) {
                mProgressTextView.setText("Opening chart");
                Navigation.findNavController(requireView()).navigate(
                    RestoreFromDriveFragmentDirections.actionShowChart()
                        .setViewMode(ViewMode.CHARTING));
              } else {
                Navigation.findNavController(requireView()).popBackStack();
              }
            }));
        return;
      }
    });
    return view;
  }

  @Override
  public void onResume() {
    mViewModel.checkAccount();
    super.onResume();
  }

  @Override
  public void onDestroy() {
    mDisposables.dispose();
    super.onDestroy();
  }

  private Single<Boolean> promptRestoreFromDrive(File file) {
    StringBuilder content = new StringBuilder();
    content.append("Would you like to restore your data from Google Drive?<br/><br/>");
    content.append(String.format("<b>Last backup was taken on:</b> %s", file.getModifiedTime()));
    if (file.getProperties() != null) {
      content.append("<br/><br/>");
      content.append("<b>Properties:</b>");
      for (Map.Entry<String, String> entry : file.getProperties().entrySet()) {
        content.append(String.format("<br/>%s: %s", entry.getKey(), entry.getValue()));
      }
    }
    return Single.create(e -> {
      new AlertDialog.Builder(getActivity())
          //set message, title, and icon
          .setTitle("Restore from Drive?")
          .setMessage(Html.fromHtml(content.toString()))
          .setPositiveButton("Yes", (dialog, whichButton) -> {
            e.onSuccess(true);
            dialog.dismiss();
          })
          .setNegativeButton("No", (dialog, which) -> {
            e.onSuccess(false);
            dialog.dismiss();
          })
          .create().show();
    });
  }
}
