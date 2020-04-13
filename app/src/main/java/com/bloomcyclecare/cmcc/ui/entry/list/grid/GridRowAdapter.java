package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

public class GridRowAdapter extends RecyclerView.Adapter<GridRowViewHolder> {

  private ViewMode mViewMode;
  private final List<List<Optional<CycleRenderer.RenderableEntry>>> mEntryLists = new ArrayList<>();
  private final Consumer<CycleRenderer.RenderableEntry> mImageClickConsumer;
  private final Consumer<CycleRenderer.RenderableEntry> mTextClickConsumer;

  public GridRowAdapter(Consumer<CycleRenderer.RenderableEntry> mImageClickConsumer, Consumer<CycleRenderer.RenderableEntry> mTextClickConsumer) {
    this.mImageClickConsumer = mImageClickConsumer;
    this.mTextClickConsumer = mTextClickConsumer;
  }

  public void updateData(List<CycleRenderer.RenderableCycle> renderableCycles, ViewMode viewMode) {
    this.mViewMode = viewMode;
    List<List<Optional<CycleRenderer.RenderableEntry>>> entryLists = new ArrayList<>();
    for (CycleRenderer.RenderableCycle renderableCycle : renderableCycles) {
      append(entryLists, renderableCycle.entries());
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
    if (entries.size() <= 35) {
      listOfLists.add(entries.stream().map(Optional::of).collect(Collectors.toList()));
    } else {
      append(listOfLists, entries.subList(0, 35));
      append(listOfLists, entries.subList(35, entries.size() - 1));
    }
  }

  @NonNull
  @Override
  public GridRowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.list_item_grid_row, parent, false);
    return new GridRowViewHolder(view, mImageClickConsumer, mTextClickConsumer);
  }

  @Override
  public void onBindViewHolder(@NonNull GridRowViewHolder holder, int position) {
    holder.updateRow(mEntryLists.get(position), mViewMode);
  }

  @Override
  public int getItemCount() {
    return mEntryLists.size();
  }
}
