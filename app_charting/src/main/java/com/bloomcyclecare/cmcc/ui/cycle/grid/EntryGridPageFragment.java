package com.bloomcyclecare.cmcc.ui.cycle.grid;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.ui.cycle.BaseCycleListFragment;
import com.bloomcyclecare.cmcc.ui.cycle.StickerDialogFragment;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailActivity;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

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

  @NonNull
  @Override
  protected NavDirections printAction(ViewMode viewMode) {
    return EntryGridPageFragmentDirections.actionPrint(viewMode);
  }

  @NonNull
  @Override
  protected NavDirections reinitAction() {
    return EntryGridPageFragmentDirections.actionReinitApp();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    cycleListViewModel().showCycleStats(true);

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
          new Bundle(), re.stickerSelectionContext(), re.manualStickerSelection(),
          mViewModel.currentViewMode(), re.canSelectYellowStamps()));
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
    LinearLayoutManager llm = new LinearLayoutManager(requireContext());
    rowRecyclerView.setLayoutManager(llm);
    rowRecyclerView.setAdapter(mGridRowAdapter);

    Map<Integer, View> viewMap = getHeaderCellMap(view);

    mViewModel.viewStates().observe(getViewLifecycleOwner(), viewState -> {
      llm.setStackFromEnd(viewState.viewMode() != ViewMode.TRAINING);
      mGridRowAdapter.updateData(
          viewState.renderedEntries(),
          viewState.viewMode());
      mMainViewModel.updateSubtitle(viewState.subtitle());
      mMainViewModel.updateTitle("Your Chart");
      for (int i=0; i < 35; i++) {
        View cellView = viewMap.get(i+1);
        if (cellView == null) {
          Timber.w("Could not find view for cell %d", i+1);
          continue;
        }
        cellView.setBackgroundColor(Color.parseColor(viewState.headerColors().getColor(i)));
      }
    });

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.action_toggle_stats) {
      cycleListViewModel().toggleStats();
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (requireActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
      requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
  }

  private static ImmutableMap<Integer, View> getHeaderCellMap(View view) {
    return ImmutableMap.<Integer, View>builder()
        .put(1, view.findViewById(R.id.grid_header_cell_1))
        .put(2, view.findViewById(R.id.grid_header_cell_2))
        .put(3, view.findViewById(R.id.grid_header_cell_3))
        .put(4, view.findViewById(R.id.grid_header_cell_4))
        .put(5, view.findViewById(R.id.grid_header_cell_5))
        .put(6, view.findViewById(R.id.grid_header_cell_6))
        .put(7, view.findViewById(R.id.grid_header_cell_7))
        .put(8, view.findViewById(R.id.grid_header_cell_8))
        .put(9, view.findViewById(R.id.grid_header_cell_9))
        .put(10, view.findViewById(R.id.grid_header_cell_10))
        .put(11, view.findViewById(R.id.grid_header_cell_11))
        .put(12, view.findViewById(R.id.grid_header_cell_12))
        .put(13, view.findViewById(R.id.grid_header_cell_13))
        .put(14, view.findViewById(R.id.grid_header_cell_14))
        .put(15, view.findViewById(R.id.grid_header_cell_15))
        .put(16, view.findViewById(R.id.grid_header_cell_16))
        .put(17, view.findViewById(R.id.grid_header_cell_17))
        .put(18, view.findViewById(R.id.grid_header_cell_18))
        .put(19, view.findViewById(R.id.grid_header_cell_19))
        .put(20, view.findViewById(R.id.grid_header_cell_20))
        .put(21, view.findViewById(R.id.grid_header_cell_21))
        .put(22, view.findViewById(R.id.grid_header_cell_22))
        .put(23, view.findViewById(R.id.grid_header_cell_23))
        .put(24, view.findViewById(R.id.grid_header_cell_24))
        .put(25, view.findViewById(R.id.grid_header_cell_25))
        .put(26, view.findViewById(R.id.grid_header_cell_26))
        .put(27, view.findViewById(R.id.grid_header_cell_27))
        .put(28, view.findViewById(R.id.grid_header_cell_28))
        .put(29, view.findViewById(R.id.grid_header_cell_29))
        .put(30, view.findViewById(R.id.grid_header_cell_30))
        .put(31, view.findViewById(R.id.grid_header_cell_31))
        .put(32, view.findViewById(R.id.grid_header_cell_32))
        .put(33, view.findViewById(R.id.grid_header_cell_33))
        .put(34, view.findViewById(R.id.grid_header_cell_34))
        .put(35, view.findViewById(R.id.grid_header_cell_35))
        .build();
  }
}
