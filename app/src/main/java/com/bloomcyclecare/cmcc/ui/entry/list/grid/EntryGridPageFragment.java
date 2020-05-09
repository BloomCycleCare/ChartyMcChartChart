package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.ui.entry.detail.EntryDetailActivity;
import com.bloomcyclecare.cmcc.ui.entry.list.BaseCycleListFragment;
import com.bloomcyclecare.cmcc.ui.entry.list.CycleListViewModel;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.google.common.base.Strings;

import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class EntryGridPageFragment extends BaseCycleListFragment {

  private EntryGridPageViewModel mViewModel;
  private GridRowAdapter mGridRowAdapter;

  private MainViewModel mMainViewModel;

  @NonNull
  protected ViewMode initialViewModeFromArgs(Bundle args) {
    return EntryGridPageFragmentArgs.fromBundle(args).getViewMode();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @SuppressLint("SourceLockedOrientationActivity")
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

    EntryGridPageFragmentArgs args= EntryGridPageFragmentArgs.fromBundle(requireArguments());
    CycleListViewModel cycleListViewModel = CycleListViewModel.forFragment(this, args.getViewMode());

    EntryGridPageViewModel.Factory factory = new EntryGridPageViewModel.Factory(
        requireActivity().getApplication(),
        cycleListViewModel,
        EntryGridPageFragmentArgs.fromBundle(requireArguments()));

    mViewModel = new ViewModelProvider(this, factory).get(EntryGridPageViewModel.class);
    mGridRowAdapter = new GridRowAdapter(re -> {// Sticker Click
      if (mViewModel.currentViewMode() != ViewMode.TRAINING || Strings.isNullOrEmpty(re.trainingMarker())) {
        Timber.d("Not prompting for ViewMode: %s", mViewModel.currentViewMode().name());
        return;
      }
      StickerDialogFragment fragment = new StickerDialogFragment(selection -> {
        mViewModel.updateSticker(re.entry().entryDate, selection).subscribe();
      });

      Bundle dialogArgs = new Bundle();
      dialogArgs.putParcelable(StickerSelection.class.getCanonicalName(), Parcels.wrap(StickerSelection.fromRenderableEntry(re)));
      fragment.setArguments(dialogArgs);
      fragment.show(getFragmentManager(), "tag");
    }, re -> {// Text click
      if (mViewModel.currentViewMode() != ViewMode.CHARTING) {
        Timber.d("Not navigating to detail activity for ViewMode: %s", mViewModel.currentViewMode().name());
        return;
      }
      Timber.d("Navigating to detail activity");
      startActivity(EntryDetailActivity.createIntent(getContext(), re.modificationContext()));
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
      mGridRowAdapter.updateData(viewState.renderableCycles(), viewState.viewMode());
      mMainViewModel.updateSubtitle(viewState.subtitle());
      mMainViewModel.updateTitle("Some title");
    });

    /*Timber.d("Setting orientation to LANDSCAPE");
    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);*/

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
