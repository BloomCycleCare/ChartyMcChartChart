package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

class GridRowViewHolder extends RecyclerView.ViewHolder {

  private static final Joiner ON_NEW_LINE = Joiner.on('\n');

  private static final List<Integer> SECTION_IDS = ImmutableList.of(
      R.id.grid_section_1, R.id.grid_section_2, R.id.grid_section_3, R.id.grid_section_4,
      R.id.grid_section_5);

  private static final List<Integer> CELL_IDS = ImmutableList.of(
      R.id.grid_cell_1, R.id.grid_cell_2, R.id.grid_cell_3, R.id.grid_cell_4,
      R.id.grid_cell_5, R.id.grid_cell_6, R.id.grid_cell_7);

  private final Context mContext;
  private final List<GridCell> mCells = new ArrayList<>();
  private final List<LinearLayout> mSections = new ArrayList<>();

  GridRowViewHolder(View v, Consumer<CycleRenderer.RenderableEntry> stickerClickConsumer, Consumer<CycleRenderer.RenderableEntry> textClickConsumer) {
    super(v);
    mContext = v.getContext();
    LinearLayout linearLayout = (LinearLayout) v;
    for (Integer sectionId : SECTION_IDS) {
      LinearLayout sectionLayout = linearLayout.findViewById(sectionId);
      for (Integer cellId : CELL_IDS) {
        int index = 7 * mSections.size() + mCells.size();
        mCells.add(new GridCell(sectionLayout.findViewById(cellId), stickerClickConsumer, textClickConsumer));
      }
      mSections.add(sectionLayout);
    }
  }

  void updateRow(List<Optional<CycleRenderer.RenderableEntry>> renderableEntries, ViewMode viewMode) {
    Preconditions.checkArgument(renderableEntries.size() == 35);
    for (int i=0; i<renderableEntries.size(); i++) {
      mCells.get(i).update(renderableEntries.get(i), viewMode);
    }
    for (LinearLayout sectionLayout : mSections) {
      sectionLayout.setBackground(mContext.getDrawable(R.drawable.section_border));
    }
  }

  private static class GridCell {
    private final Context context;
    private final TextView textView;
    private final TextView stickerView;

    private CycleRenderer.RenderableEntry boundEntry;

    private GridCell(LinearLayout cellLayout, Consumer<CycleRenderer.RenderableEntry> stickerClickListener, Consumer<CycleRenderer.RenderableEntry> textClickListener) {
      context = cellLayout.getContext();
      textView = cellLayout.findViewById(R.id.cell_text_view);
      textView.setOnClickListener(v -> textClickListener.accept(boundEntry));
      stickerView = cellLayout.findViewById(R.id.cell_sticker_view);
      stickerView.setOnClickListener(v -> stickerClickListener.accept(boundEntry));
    }

    public void update(Optional<CycleRenderer.RenderableEntry> renderableEntry, ViewMode viewMode) {
      boundEntry = renderableEntry.orElse(null);
      if (renderableEntry.isPresent()) {
        fillCell(renderableEntry.get(), viewMode);
      } else {
        clearCell();
      }
    }

    private void fillCell(CycleRenderer.RenderableEntry renderableEntry, ViewMode viewMode) {
      List<String> parts = new ArrayList<>();
      if (viewMode != ViewMode.TRAINING) {
        parts.add(renderableEntry.dateSummaryShort());
      }
      parts.add(renderableEntry.entrySummary());
      textView.setText(ON_NEW_LINE.join(parts));

      stickerView.setBackground(context.getDrawable(getStickerResourceID(renderableEntry, viewMode)));

      if (viewMode != ViewMode.TRAINING) {
        stickerView.setText(renderableEntry.peakDayText());
      } else {
        StickerSelection stickerSelection = renderableEntry.entry().stickerSelection;
        if (stickerSelection == null) {
          stickerView.setText(renderableEntry.trainingMarker());
        } else {
          stickerView.setText(Optional.ofNullable(stickerSelection.text)
              .map(t -> String.valueOf(t.value))
              .orElse(""));
        }
      }
    }

    private int getStickerResourceID(CycleRenderer.RenderableEntry renderableEntry, ViewMode viewMode) {
      if (viewMode == ViewMode.TRAINING) {
        StickerSelection stickerSelection = renderableEntry.entry().stickerSelection;
        if (stickerSelection == null || stickerSelection.sticker == null) {
          return R.drawable.sticker_grey;
        }
        return stickerSelection.sticker.resourceId;
      }
      return StickerSelection.fromRenderableEntry(renderableEntry).sticker.resourceId;
    }

    private void clearCell() {
      textView.setText("");
      stickerView.setText("");
      stickerView.setBackground(context.getDrawable(R.drawable.sticker_grey));
    }
  }
}
