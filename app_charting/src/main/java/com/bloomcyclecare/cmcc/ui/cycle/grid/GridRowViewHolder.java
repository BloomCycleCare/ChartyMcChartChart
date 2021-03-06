package com.bloomcyclecare.cmcc.ui.cycle.grid;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import androidx.constraintlayout.widget.ConstraintLayout;
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

  GridRowViewHolder(View v, Consumer<RenderedEntry> stickerClickConsumer, Consumer<RenderedEntry> textClickConsumer) {
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

  void updateRow(List<Optional<RenderedEntry>> renderableEntries, RowRenderContext rowRenderContext) {
    Preconditions.checkArgument(renderableEntries.size() == 35);
    for (int i=0; i<renderableEntries.size(); i++) {
      mCells.get(i).update(CellRenderContext.create(rowRenderContext, renderableEntries.get(i)));
    }
    for (LinearLayout sectionLayout : mSections) {
      sectionLayout.setBackground(mContext.getDrawable(R.drawable.section_border));
    }
  }

  private static class GridCell {
    private final Context context;
    private final TextView textView;
    private final TextView stickerView;
    private final TextView measurementView;
    private final View strikeThroughView;
    private final Consumer<RenderedEntry> stickerClickListener;

    private RenderedEntry boundEntry;

    private GridCell(ConstraintLayout cellLayout, Consumer<RenderedEntry> stickerClickListener, Consumer<RenderedEntry> textClickListener) {
      this.stickerClickListener = stickerClickListener;
      context = cellLayout.getContext();
      textView = cellLayout.findViewById(R.id.cell_text_view);
      textView.setOnClickListener(v -> textClickListener.accept(boundEntry));
      stickerView = cellLayout.findViewById(R.id.cell_sticker_view);
      strikeThroughView = cellLayout.findViewById(R.id.cell_sticker_strike_through);
      measurementView = cellLayout.findViewById(R.id.cell_measurement_indicator);
    }

    public void update(CellRenderContext renderContext) {
      boundEntry = renderContext.renderedEntry().orElse(null);
      if (renderContext.renderedEntry().map(RenderedEntry::canPromptForStickerSelection).orElse(false)) {
        stickerView.setOnClickListener(v -> stickerClickListener.accept(boundEntry));
      } else {
        stickerView.setOnClickListener(v -> {});
      }
      if (boundEntry != null) {
        fillCell(renderContext);
      } else {
        clearCell();
      }
    }

    private void fillCell(CellRenderContext renderContext) {
      Optional<RenderedEntry> renderedEntry = renderContext.renderedEntry();
      if (!renderedEntry.isPresent()) {
        clearCell();
        return;
      }
      ViewMode viewMode = renderContext.rowRenderContext().viewMode();
      renderedEntry.ifPresent(entry -> fillCell(viewMode, renderedEntry.get()));
    }

    private void fillCell(ViewMode viewMode, RenderedEntry renderedEntry) {
      List<String> parts = new ArrayList<>();
      if (viewMode != ViewMode.TRAINING) {
        parts.add(renderedEntry.entryDateShortStr());
      }
      parts.add(renderedEntry.observationSummary().orElse("---"));
      textView.setText(ON_NEW_LINE.join(parts));

      stickerView.setBackground(context.getDrawable(renderedEntry.stickerBackgroundResource()));
      stickerView.setText(renderedEntry.stickerText());

      strikeThroughView.setVisibility(
          renderedEntry.showStickerStrike() ? View.VISIBLE : View.GONE);

      measurementView.setText(renderedEntry.measurementSummary().orElse(""));
    }

    private void clearCell() {
      textView.setText("");
      stickerView.setText("");
      stickerView.setBackground(context.getDrawable(R.drawable.sticker_grey));
      strikeThroughView.setVisibility(View.GONE);
    }
  }

  @AutoValue
  public static abstract class RowRenderContext {

    abstract ViewMode viewMode();

    public static RowRenderContext create(ViewMode viewMode) {
      return new AutoValue_GridRowViewHolder_RowRenderContext(viewMode);
    }
  }

  @AutoValue
  public static abstract class CellRenderContext {

    abstract RowRenderContext rowRenderContext();
    abstract Optional<RenderedEntry> renderedEntry();

    public static CellRenderContext create(RowRenderContext rowRenderContext, Optional<RenderedEntry> renderedEntry) {
      return new AutoValue_GridRowViewHolder_CellRenderContext(rowRenderContext,renderedEntry);
    }
  }
}
