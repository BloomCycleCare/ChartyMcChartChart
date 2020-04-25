package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.ui.entry.detail.EntryDetailActivity;
import com.bloomcyclecare.cmcc.ui.entry.list.EntryListViewModel;
import com.google.common.base.Strings;

import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class EntryGridPageFragment extends Fragment {


  private EntryListViewModel mViewModel;
  private GridRowAdapter mGridRowAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = ViewModelProviders.of(getActivity()).get(EntryListViewModel.class);
    mGridRowAdapter = new GridRowAdapter(re -> {// Sticker Click
      if (mViewModel.currentViewMode() != ViewMode.TRAINING || Strings.isNullOrEmpty(re.trainingMarker())) {
        Timber.d("Not prompting for ViewMode: %s", mViewModel.currentViewMode().name());
        return;
      }
      StickerDialogFragment fragment = new StickerDialogFragment(selection -> {
        mViewModel.updateSticker(re.entry().entryDate, selection).subscribe();
      });

      Bundle args = new Bundle();
      args.putParcelable(StickerSelection.class.getCanonicalName(), Parcels.wrap(StickerSelection.fromRenderableEntry(re)));
      fragment.setArguments(args);
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
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_entry_grid_page, container, false);

    RecyclerView rowRecyclerView = view.findViewById(R.id.rv_grid_rows);
    rowRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    rowRecyclerView.setAdapter(mGridRowAdapter);

    mViewModel.viewStates().observe(this, viewState -> {
      mGridRowAdapter.updateData(viewState.renderableCycles, viewState.viewMode);
    });

    return view;
  }
}
