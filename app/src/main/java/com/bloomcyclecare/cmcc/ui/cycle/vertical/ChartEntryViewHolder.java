package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;

import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by parkeroth on 7/1/17.
 */

interface ChartEntryViewHolder extends View.OnLongClickListener {

  class Impl extends RecyclerView.ViewHolder {
    private final TextView mEntryNumTextView;
    private final TextView mEntryDateTextView;
    private final TextView mEntryDataTextView;
    private final TextView mEntryPeakTextView;
    private final TextView mSymptomGoalSummaryView;
    private final TextView mPocSummaryTextView;
    private final ImageView mStarImageView;
    private final View mStickerView;
    private final View mSeparator;
    private final View mStrikeView;
    private final Context mContext;

    private CycleRenderer.RenderableEntry mBoundEntry;
    private ViewMode mBoundViewMode;

    public Impl(
        Context context,
        View itemView,
        Consumer<CycleRenderer.RenderableEntry> textClickConsumer,
        Consumer<CycleRenderer.RenderableEntry> stickerClickConsumer) {
      super(itemView);
      mContext = context;
      mEntryNumTextView = itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = itemView.findViewById(R.id.tv_entry_data);
      mEntryPeakTextView = itemView.findViewById(R.id.tv_peak_day);
      mPocSummaryTextView = itemView.findViewById(R.id.tv_tv_poc_summary);
      mSymptomGoalSummaryView = itemView.findViewById(R.id.tv_goal_symptom_summary);
      mStickerView = itemView.findViewById(R.id.baby_image_view);
      mStarImageView = itemView.findViewById(R.id.star_image_view);
      mSeparator = itemView.findViewById(R.id.separator);
      mStrikeView = itemView.findViewById(R.id.strike_through);
      mStickerView.setOnClickListener(v -> stickerClickConsumer.accept(mBoundEntry));
      itemView.setOnClickListener(v -> textClickConsumer.accept(mBoundEntry));
      itemView.setOnLongClickListener(v -> {
        if (mBoundViewMode == ViewMode.TRAINING) {
          return false;
        }
        new AlertDialog.Builder(mContext)
            .setTitle("Instruction Summary")
            .setMessage(mBoundEntry.instructionSummary())
            .setPositiveButton("Edit", (dialogInterface, i) -> {
              textClickConsumer.accept(mBoundEntry);
              dialogInterface.dismiss();
            })
            .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss())
            .create()
            .show();
        return true;
      });
    }

    public void bind(CycleRenderer.RenderableEntry renderableEntry,
                     ViewMode viewMode,
                     boolean autoStickeringEnabled) {
      mStickerView.setVisibility(View.VISIBLE);

      mEntryDataTextView.setText(renderableEntry.entrySummary());
      mEntryNumTextView.setText(String.valueOf(renderableEntry.entryNum()));
      mEntryDateTextView.setText(renderableEntry.dateSummary());

      StickerSelection autoSelection = StickerSelection.fromRenderableEntry(renderableEntry);
      StickerSelection manualSelection = renderableEntry.entry().stickerSelection;

      int resourceId = R.color.entryGrey;
      if (manualSelection != null) {
        resourceId = manualSelection.sticker.resourceId;
      } else if (autoStickeringEnabled) {
        resourceId = autoSelection.sticker.resourceId;
      }
      mStickerView.setBackgroundResource(resourceId);
      if (manualSelection != null && !autoSelection.equals(manualSelection)) {
        mStrikeView.setVisibility(View.VISIBLE);
      } else {
        mStrikeView.setVisibility(View.GONE);
      }

      String text = "";
      if (viewMode == ViewMode.TRAINING) {
        text = renderableEntry.trainingMarker();
      } else if (autoStickeringEnabled) {
        text = autoSelection.text != null ? autoSelection.text.name() : "";
      } else if (manualSelection != null) {
        text = manualSelection.text != null ? manualSelection.text.name() : "";
      } else {
        text = "?";
      }
      mEntryPeakTextView.setText(text);

      mPocSummaryTextView.setText(viewMode == ViewMode.TRAINING
          ? "" : renderableEntry.pocSummary());

      ChartEntry entry = renderableEntry.modificationContext().entry;
      mSymptomGoalSummaryView.setText(renderableEntry.essentialSamenessSummary());

      boolean showOverlay = entry.wellnessEntry.hasItem(null) || entry.symptomEntry.hasItem(null);
      if (showOverlay) {
        mStarImageView.setVisibility(View.VISIBLE);
        mSymptomGoalSummaryView.setVisibility(View.INVISIBLE);
      } else {
        mStarImageView.setVisibility(View.INVISIBLE);
        mSymptomGoalSummaryView.setVisibility(View.VISIBLE);
      }

      boolean showTransition = entry.observationEntry.getDate().dayOfWeek().getAsString().equals("1");
      if (showTransition) {
        mSeparator.setBackgroundColor(mContext.getColor(R.color.week_separator));
      } else {
        mSeparator.setBackgroundColor(mContext.getColor(R.color.entry_separator));
      }

      mBoundEntry = renderableEntry;
      mBoundViewMode = viewMode;
    }
  }
}
