package com.bloomcyclecare.cmcc.ui.cycle;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bloomcyclecare.cmcc.BuildConfig;
import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import io.reactivex.android.schedulers.AndroidSchedulers;
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

  @NonNull
  protected abstract NavDirections printAction(ViewMode viewMode);

  @NonNull
  protected abstract NavDirections reinitAction();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = CycleListViewModel.forFragment(this, initialViewModeFromArgs(requireArguments()), exerciseIdFromArgs(requireArguments()));

    setHasOptionsMenu(true);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    pruneMenuOptions(menu, mViewModel.currentViewState());
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_entry_list, menu);
  }

  private static void pruneMenuOptions(@NonNull Menu menu, @NonNull CycleListViewModel.ViewState viewState) {
    ViewMode viewMode = viewState.viewMode();
    boolean runningDebugBuild = BuildConfig.BUILD_TYPE.equals("debug");
    if (viewMode != ViewMode.CHARTING) {
      menu.findItem(R.id.action_export).setVisible(false);
      menu.findItem(R.id.action_trigger_sync).setVisible(false);
      menu.findItem(R.id.action_show_monitor_readings).setVisible(false);
    } else {
      menu.findItem(R.id.action_export).setVisible(true);
      menu.findItem(R.id.action_trigger_sync).setVisible(runningDebugBuild);
      menu.findItem(R.id.action_show_monitor_readings).setVisible(viewState.monitorReadingsEnabled());
    }
    menu.findItem(R.id.action_print).setVisible(viewMode != ViewMode.TRAINING);
    menu.findItem(R.id.action_toggle_layout).setVisible(viewMode != ViewMode.TRAINING);
    menu.findItem(R.id.action_toggle_demo).setVisible(viewMode != ViewMode.TRAINING);
    menu.findItem(R.id.action_toggle_demo).setChecked(viewMode == ViewMode.DEMO);
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
              mDisposables.add(mViewModel.clearData()
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(() -> {
                    Navigation.findNavController(requireView()).navigate(reinitAction());
                    dialog.dismiss();
                  }));
            })
            .setNegativeButton("No", ((dialog, which) -> {
              dialog.dismiss();
            }))
            .create().show();
        return true;

      case R.id.action_print:
        NavHostFragment.findNavController(this).navigate(printAction(mViewModel.currentViewMode()));
        return true;

      case R.id.action_show_monitor_readings:
        mViewModel.toggleShowMonitorReadings();
        item.setChecked(!item.isChecked());
        return true;

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
