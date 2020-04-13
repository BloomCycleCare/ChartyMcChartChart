package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.entry.list.EntryListViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EntryGridPageFragment extends Fragment {


  private EntryListViewModel mViewModel;
  private GridRowAdapter mGridRowAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = ViewModelProviders.of(getActivity()).get(EntryListViewModel.class);
    mGridRowAdapter = new GridRowAdapter(re -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
      builder.setTitle("Sticker Click");
      builder.setMessage(String.format("You clicked on %s.", re.dateSummary()));
      builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
      builder.show();
    }, re -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
      builder.setTitle("Text Click");
      builder.setMessage(String.format("You clicked on %s.", re.dateSummary()));
      builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
      builder.show();
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
