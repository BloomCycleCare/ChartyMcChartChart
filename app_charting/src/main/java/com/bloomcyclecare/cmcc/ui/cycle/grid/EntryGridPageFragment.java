package com.bloomcyclecare.cmcc.ui.cycle.grid;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.models.training.Exercise;
import com.bloomcyclecare.cmcc.ui.cycle.BaseCycleListFragment;
import com.bloomcyclecare.cmcc.ui.cycle.StickerDialogFragment;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailActivity;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class EntryGridPageFragment extends BaseCycleListFragment {

  private MainViewModel mMainViewModel;
  private EntryGridPageViewModel mViewModel;
  private GridRowAdapter mGridRowAdapter;

  @NonNull
  @Override
  protected ViewMode initialViewModeFromArgs(Bundle args) {
    return EntryGridPageFragmentArgs.fromBundle(args).getViewMode();
  }

  @NonNull
  @Override
  protected Optional<Exercise.ID> exerciseIdFromArgs(Bundle bundle) {
    int ordinal = EntryGridPageFragmentArgs.fromBundle(bundle).getExerciseIdOrdinal();
    if (ordinal < 0) {
      return Optional.empty();
    }
    return Optional.of(Exercise.ID.values()[ordinal]);
  }

  @NonNull
  @Override
  protected NavDirections toggleLayoutAction(ViewMode viewMode) {
    return EntryGridPageFragmentDirections.actionToggleLayout().setViewMode(viewMode);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

    EntryGridPageViewModel.Factory factory = new EntryGridPageViewModel.Factory(
        requireActivity().getApplication(),
        cycleListViewModel(),
        EntryGridPageFragmentArgs.fromBundle(requireArguments()));

    mViewModel = new ViewModelProvider(this, factory).get(EntryGridPageViewModel.class);
    mGridRowAdapter = new GridRowAdapter(re -> {// Sticker Click
      /*if (mViewModel.currentViewMode() != ViewMode.TRAINING || Strings.isNullOrEmpty(re.trainingMarker())) {
        Timber.d("Not prompting for ViewMode: %s", mViewModel.currentViewMode().name());
        return;
      }*/
      StickerDialogFragment fragment = new StickerDialogFragment(result -> {
        mViewModel.updateSticker(re.entryDate(), result.selection).subscribe();
        if (!result.ok()) {
          Toast.makeText(requireContext(), "Incorrect selection", Toast.LENGTH_SHORT).show();
        }
      });
      fragment.setArguments(StickerDialogFragment.fillArgs(
          new Bundle(), re.expectedStickerSelection().orElse(null), re.manualStickerSelection(), mViewModel.currentViewMode()));
      fragment.show(getChildFragmentManager(), "tag");
    }, re -> {// Text click
      if (mViewModel.currentViewMode() != ViewMode.CHARTING) {
        Timber.d("Not navigating to detail activity for ViewMode: %s", mViewModel.currentViewMode().name());
        return;
      }
      Timber.d("Navigating to detail activity");
      startActivity(EntryDetailActivity.createIntent(getContext(), re.entryModificationContext()));
    });
  }

  @Nullable
  @Override
  @SuppressLint("SourceLockedOrientationActivity")
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_entry_grid_page, container, false);

    EntryGridPageFragmentArgs inputArgs = EntryGridPageFragmentArgs.fromBundle(requireArguments());
    if (inputArgs.getLandscapeMode() && getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
      requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    RecyclerView rowRecyclerView = view.findViewById(R.id.rv_grid_rows);
    rowRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    rowRecyclerView.setAdapter(mGridRowAdapter);

    mViewModel.viewStates().observe(getViewLifecycleOwner(), viewState -> {
      mGridRowAdapter.updateData(
          viewState.renderedEntries(),
          viewState.viewMode());
      mMainViewModel.updateSubtitle(viewState.subtitle());
      mMainViewModel.updateTitle("Your Chart");
    });

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (requireActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
      requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
  }
}
