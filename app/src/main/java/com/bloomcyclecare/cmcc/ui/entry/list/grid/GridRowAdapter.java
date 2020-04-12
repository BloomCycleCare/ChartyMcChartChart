package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridRowAdapter extends RecyclerView.Adapter<GridRowViewHolder> {

  private final List<List<Optional<CycleRenderer.RenderableEntry>>> mEntryLists = new ArrayList<>();

  public void updateData(List<CycleRenderer.RenderableCycle> renderableCycles) {
    List<List<Optional<CycleRenderer.RenderableEntry>>> entryLists = new ArrayList<>();
    for (CycleRenderer.RenderableCycle renderableCycle : renderableCycles) {
      if (renderableCycle.entries().size() <= 35) {
        append(entryLists, renderableCycle.entries());
      } else {
        append(entryLists, renderableCycle.entries().subList(0, 35));
        append(entryLists, renderableCycle.entries().subList(35, renderableCycle.entries().size() - 1));
      }
      List<Optional<CycleRenderer.RenderableEntry>> lastList = entryLists.get(entryLists.size() - 1);
      while (lastList.size() < 35) {
        lastList.add(Optional.empty());
      }
    }
    this.mEntryLists.clear();
    this.mEntryLists.addAll(entryLists);
    notifyDataSetChanged();
  }

  private static <T> void append(List<List<Optional<T>>> listOfLists, List<T> entries) {
    listOfLists.add(entries.stream().map(Optional::of).collect(Collectors.toList()));
  }

  @NonNull
  @Override
  public GridRowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.list_item_grid_row, parent, false);
    return new GridRowViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull GridRowViewHolder holder, int position) {
    holder.updateRow(mEntryLists.get(position));
  }

  @Override
  public int getItemCount() {
    return mEntryLists.size();
  }
}
