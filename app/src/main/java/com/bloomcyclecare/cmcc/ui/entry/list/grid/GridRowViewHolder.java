package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import androidx.recyclerview.widget.RecyclerView;

class GridRowViewHolder extends RecyclerView.ViewHolder {

  private static final List<Integer> SECTION_IDS = ImmutableList.of(
      R.id.grid_section_1, R.id.grid_section_2, R.id.grid_section_3, R.id.grid_section_4,
      R.id.grid_section_5);

  private static final List<Integer> CELL_IDS = ImmutableList.of(
      R.id.grid_cell_1, R.id.grid_cell_2, R.id.grid_cell_3, R.id.grid_cell_4,
      R.id.grid_cell_5, R.id.grid_cell_6, R.id.grid_cell_7);

  private final Context mContext;
  private final List<GridCell> mCells = new ArrayList<>();
  private final List<LinearLayout> mSections = new ArrayList<>();

  GridRowViewHolder(View v) {
    super(v);
    mContext = v.getContext();
    LinearLayout linearLayout = (LinearLayout) v;
    for (Integer sectionId : SECTION_IDS) {
      LinearLayout sectionLayout = linearLayout.findViewById(sectionId);
      mSections.add(sectionLayout);
      for (Integer cellId : CELL_IDS) {
        mCells.add(new GridCell(sectionLayout.findViewById(cellId)));
      }
    }
  }

  void updateRow(List<Optional<CycleRenderer.RenderableEntry>> renderableEntries) {
    Preconditions.checkArgument(renderableEntries.size() == 35);
    for (int i=0; i<renderableEntries.size(); i++) {
      mCells.get(i).update(renderableEntries.get(i));
    }
    for (LinearLayout sectionLayout : mSections) {
      sectionLayout.setBackground(mContext.getDrawable(R.drawable.section_border));
    }
  }

  private static class GridCell {
    private final Context context;
    private final TextView textView;
    private final ImageView imageView;

    private GridCell(LinearLayout cellLayout) {
      context = cellLayout.getContext();
      textView = cellLayout.findViewById(R.id.cell_text_view);
      imageView = cellLayout.findViewById(R.id.cell_image_view);
    }

    public void update(Optional<CycleRenderer.RenderableEntry> renderableEntry) {
      if (renderableEntry.isPresent()) {
        fillCell(renderableEntry.get());
      } else {
        clearCell();
      }
    }

    private void fillCell(CycleRenderer.RenderableEntry renderableEntry) {
      textView.setText(renderableEntry.entrySummary());
      if (renderableEntry.showBaby()) {
        switch (renderableEntry.backgroundColor()) {
          case WHITE:
            imageView.setBackground(context.getDrawable(R.drawable.sticker_white_baby));
            break;
          case GREEN:
            imageView.setBackground(context.getDrawable(R.drawable.sticker_green_baby));
            break;
          case YELLOW:
            imageView.setBackground(context.getDrawable(R.drawable.sticker_yellow_baby));
            break;
          default:
            throw new IllegalStateException();
        }
      } else {
        imageView.setBackground(context.getDrawable(R.drawable.border));
        setColor(renderableEntry.backgroundColor().resourceId);
      }
    }

    private void clearCell() {
      textView.setText("");
      setColor(R.color.entryGrey);
    }

    private void setColor(int resourceId) {
      Drawable drawable = imageView.getBackground().mutate();
      drawable.setTintMode(PorterDuff.Mode.SRC_OUT);
      drawable.setTint(context.getColor(resourceId));
    }
  }
}
