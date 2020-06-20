package com.bloomcyclecare.cmcc.ui.cycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bloomcyclecare.cmcc.BuildConfig;
import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.ui.init.UserInitActivity;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public abstract class BaseCycleListFragment extends Fragment {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private CycleListViewModel mViewModel;

  protected CycleListViewModel cycleListViewModel() {
    return mViewModel;
  }

  @NonNull
  protected abstract ViewMode initialViewModeFromArgs(Bundle bundle);

  @NonNull
  protected abstract Optional<Exercise.ID> exerciseIdFromArgs(Bundle bundle);

  @NonNull
  protected abstract NavDirections toggleLayoutAction(ViewMode viewMode);

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = CycleListViewModel.forFragment(this, initialViewModeFromArgs(requireArguments()), exerciseIdFromArgs(requireArguments()));

    setHasOptionsMenu(true);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    pruneMenuOptions(menu, mViewModel.currentViewMode());
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_entry_list, menu);
  }

  private static void pruneMenuOptions(@NonNull Menu menu, @NonNull ViewMode viewMode) {
    boolean runningDebugBuild = BuildConfig.BUILD_TYPE.equals("debug");
    if (viewMode != ViewMode.CHARTING) {
      menu.findItem(R.id.action_print).setVisible(false);
      menu.findItem(R.id.action_export).setVisible(false);
      menu.findItem(R.id.action_trigger_sync).setVisible(false);
    } else {
      menu.findItem(R.id.action_print).setVisible(true);
      menu.findItem(R.id.action_export).setVisible(true);
      menu.findItem(R.id.action_trigger_sync).setVisible(runningDebugBuild);
    }
    menu.findItem(R.id.action_toggle_layout).setVisible(viewMode != ViewMode.TRAINING);
    menu.findItem(R.id.action_toggle_demo).setVisible(viewMode != ViewMode.TRAINING);
    menu.findItem(R.id.action_clear_data).setVisible(runningDebugBuild);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_toggle_layout:
        mDisposables.add(mViewModel.viewStateStream()
            .firstOrError()
            .map(CycleListViewModel.ViewState::viewMode)
            .doOnSuccess(viewMode -> Timber.d("Toggling view mode with ViewState %s", viewMode.name()))
            .map(this::toggleLayoutAction)
            .subscribe(action -> NavHostFragment.findNavController(this).navigate(action)));
        return true;

      case R.id.action_export:
        mDisposables.add(mViewModel.export().subscribe(this::startActivity, Timber::e));
        return true;

      case R.id.action_trigger_sync:
        ChartingApp.cast(requireActivity().getApplication()).triggerSync();
        return true;

      case R.id.action_toggle_demo:
        mViewModel.toggleViewMode();
        return true;

      case R.id.action_clear_data:
        new AlertDialog.Builder(requireContext())
            .setTitle("Confirm Clear Data")
            .setMessage("Are you sure you want to permanently clear all your data?")
            .setPositiveButton("Yes", (dialog, which) -> {
              mDisposables.add(mViewModel.clearData().subscribe(() -> {
                startActivity(new Intent(requireContext(), UserInitActivity.class));
                requireActivity().finish();
                dialog.dismiss();
              }));
            })
            .setNegativeButton("No", ((dialog, which) -> {
              dialog.dismiss();
            }))
            .create().show();

      default:
        return NavigationUI.onNavDestinationSelected(
            item, NavHostFragment.findNavController(this));
    }
  }

  public interface BaseArgs {
    @NonNull
    ViewMode getViewMode();
  }
}
