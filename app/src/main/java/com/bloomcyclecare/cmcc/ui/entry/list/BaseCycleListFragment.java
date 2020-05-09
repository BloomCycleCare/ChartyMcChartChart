package com.bloomcyclecare.cmcc.ui.entry.list;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public abstract class BaseCycleListFragment extends Fragment {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private CycleListViewModel mViewModel;

  @NonNull
  protected abstract ViewMode initialViewModeFromArgs(Bundle bundle);

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = CycleListViewModel.forFragment(this, initialViewModeFromArgs(requireArguments()));

    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);

    inflater.inflate(R.menu.menu_entry_list, menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_export:
        mDisposables.add(mViewModel.export().subscribe(this::startActivity, Timber::e));
        return true;

      case R.id.action_trigger_sync:
        MyApplication.cast(requireActivity().getApplication()).triggerSync();
        return true;

      case R.id.action_toggle_demo:
        mViewModel.toggleViewMode();
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
