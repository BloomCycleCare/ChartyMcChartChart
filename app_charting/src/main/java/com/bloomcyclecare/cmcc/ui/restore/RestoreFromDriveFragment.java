package com.bloomcyclecare.cmcc.ui.restore;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.api.services.drive.model.File;

import java.util.Map;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class RestoreFromDriveFragment extends Fragment {

  RestoreFromDriveViewModel mViewModel;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_drive_restore, container, false);

    mViewModel = new ViewModelProvider(this).get(RestoreFromDriveViewModel.class);

    mViewModel.checkAccount();
    mViewModel.viewState().observeForever(viewState -> {
      if (!viewState.account().isPresent()) {
        startActivityForResult(GoogleAuthHelper.getPromptIntent(requireContext()), 1);
      }
      if (viewState.backupFile().isPresent()) {
        promptRestoreFromDrive(viewState.backupFile().get())
            .flatMap(doRestore -> {
              if (!doRestore) {
                return Single.just(false);
              }
              return mViewModel.restore(viewState.backupFile().get()).andThen(Single.just(true));
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(restored -> {
              if (restored) {
                Navigation.findNavController(requireView()).navigate(RestoreFromDriveFragmentDirections.actionShowChart());
              } else {
                Navigation.findNavController(requireView()).popBackStack();
              }
            });
      }
    });
    return view;
  }

  @Override
  public void onResume() {
    mViewModel.checkAccount();
    super.onResume();
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
