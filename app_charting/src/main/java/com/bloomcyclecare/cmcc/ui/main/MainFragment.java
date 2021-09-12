package com.bloomcyclecare.cmcc.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.backup.AppStateExporter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class MainFragment extends Fragment {

  private CompositeDisposable mDisposables = new CompositeDisposable();
  private MainViewModel mMainViewModel;

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

    TextView mStatusView = view.findViewById(R.id.splash_status_tv);
    mStatusView.setText("Initializing App");
    view.findViewById(R.id.splash_progress).setVisibility(View.VISIBLE);

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
      mDisposables.add(mMainViewModel
          .shouldShowCyclePage(promptForExportExistingData()
              .subscribeOn(AndroidSchedulers.mainThread())
              .observeOn(AndroidSchedulers.mainThread()), requireActivity())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(showChart -> {
            if (showChart) {
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

  private Single<Boolean> promptForExportExistingData() {
    return Single.create(e -> {
      new AlertDialog.Builder(requireContext())
          .setTitle("Export Existing Data?")
          .setMessage("Existing data was found but the app cann't be loaded. Would you like to export this data to a file before it is cleared?")
          .setPositiveButton("Yes", (d, i) -> {
            e.onSuccess(true);
            d.dismiss();
          })
          .setNegativeButton("No", (d, i) -> {
            e.onSuccess(false);
            d.dismiss();
          })
          .show();
    });
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }
}
