package com.bloomcyclecare.cmcc.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class MainFragment extends Fragment {

  private CompositeDisposable mDisposables = new CompositeDisposable();
  private MainViewModel mMainViewModel;

  private ProgressBar mProgressBar;
  private TextView mErrorView;
  private TextView mStatusView;

  public MainFragment() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_splash, container, false);

    mProgressBar = view.findViewById(R.id.splash_progress);
    mErrorView = view.findViewById(R.id.splash_error_tv);
    mStatusView = view.findViewById(R.id.splash_status_tv);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    mMainViewModel.updateSubtitle("");

    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    Intent intent = requireActivity().getIntent();
    boolean importingData =
        intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW);
    NavController navController = Navigation.findNavController(view);
    if (importingData) {
      switch (intent.getType()) {
        case "application/json":
          navController.navigate(R.id.action_import_app_state);
          break;
        case "application/octet-stream":
          navController.navigate(R.id.import_from_baby_daybook);
          break;
        default:
          new AlertDialog.Builder(requireContext())
              .setTitle("File Not Supported")
              .setMessage("This filetype is not supported by CMCC: " + intent.getType())
              .setPositiveButton("Exit", (d,w) -> requireActivity().finish())
              .show();
      }
    } else {
      mDisposables.add(mMainViewModel.appInitialized()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(initialized -> {
            if (initialized) {
              mDisposables.add(mMainViewModel.initialState().subscribe(viewState -> {
                navController.navigate(MainFragmentDirections.actionShowChart()
                    .setViewMode(viewState.defaultViewMode()));
              }));
            } else {
              navController.navigate(MainFragmentDirections.actionInitApp());
            }
          }));
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  public void showProgress(final String message) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(() -> {
      mProgressBar.setVisibility(View.VISIBLE);
      updateStatus(message);
      mStatusView.setVisibility(View.VISIBLE);
      mErrorView.setText("");
      mErrorView.setVisibility(View.INVISIBLE);
      Timber.i(message);
    });
  }

  public void updateStatus(final String status) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    getActivity().runOnUiThread(() -> mStatusView.setText(status));
  }

  public void showError(final String errorText) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(() -> {
      mProgressBar.setVisibility(View.INVISIBLE);
      mStatusView.setVisibility(View.INVISIBLE);
      mErrorView.setText(errorText);
      mErrorView.setVisibility(View.VISIBLE);
    });
  }

}
