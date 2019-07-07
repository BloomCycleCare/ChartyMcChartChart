package com.roamingroths.cmcc.ui.entry.list;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;

/**
 * Created by parkeroth on 7/1/17.
 */

public interface ChartEntryViewHolder extends View.OnClickListener {

  class Impl extends RecyclerView.ViewHolder implements ChartEntryViewHolder {
    private final ChartEntryAdapter.OnClickHandler mClickHandler;
    private final TextView mEntryNumTextView;
    private final TextView mEntryDateTextView;
    private final TextView mEntryDataTextView;
    private final TextView mEntryPeakTextView;
    private final TextView mSymptomGoalSummaryView;
    private final ImageView mBabyImageView;
    private final ImageView mStarImageView;
    private final View mEntryBackgroundView;
    private final View mSeparator;
    private final View mWeekSeparator;

    private CycleRenderer.EntryModificationContext mBoundModificationContext;

    public Impl(
        View itemView,
        ChartEntryAdapter.OnClickHandler clickHandler) {
      super(itemView);
      itemView.setOnClickListener(this);
      mClickHandler = clickHandler;
      mEntryNumTextView = itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = itemView.findViewById(R.id.tv_entry_data);
      mEntryPeakTextView = itemView.findViewById(R.id.tv_peak_day);
      mSymptomGoalSummaryView = itemView.findViewById(R.id.tv_goal_symptom_summary);
      mBabyImageView = itemView.findViewById(R.id.baby_image_view);
      mStarImageView = itemView.findViewById(R.id.star_image_view);
      mEntryBackgroundView = itemView.findViewById(R.id.entry_item_layout);
      mSeparator = itemView.findViewById(R.id.observation_list_separator);
      mWeekSeparator = itemView.findViewById(R.id.week_separator);
    }

    public void bind(CycleRenderer.RenderableEntry renderableEntry) {
      mEntryDataTextView.setText(renderableEntry.entrySummary);
      mEntryBackgroundView.setBackgroundResource(renderableEntry.backgroundColor.resourceId);
      mEntryNumTextView.setText(String.valueOf(renderableEntry.entryNum));
      mEntryDateTextView.setText(renderableEntry.dateSummary);
      mEntryPeakTextView.setText(renderableEntry.peakDayText);
      mBabyImageView.setVisibility(renderableEntry.showBaby ? View.VISIBLE : View.INVISIBLE);

      ChartEntry entry = renderableEntry.modificationContext.entry;
      mSymptomGoalSummaryView.setText(String.format("S: %d", entry.symptomEntry.getNumSymptoms()));

      boolean showOverlay = entry.wellnessEntry.hasItem(null) || entry.symptomEntry.hasItem(null);
      if (showOverlay) {
        mStarImageView.setVisibility(View.VISIBLE);
        mSymptomGoalSummaryView.setVisibility(View.INVISIBLE);
      } else {
        mStarImageView.setVisibility(View.INVISIBLE);
        mSymptomGoalSummaryView.setVisibility(View.VISIBLE);
      }

      boolean showTransition = entry.observationEntry.getDate().dayOfWeek().getAsString().equals("1");
      mWeekSeparator.setVisibility(showTransition ? View.VISIBLE : View.GONE);
      mSeparator.setVisibility(showTransition ? View.GONE : View.VISIBLE);

      mBoundModificationContext = renderableEntry.modificationContext;
    }

    @Override
    public void onClick(View v) {
      int index = getAdapterPosition();
      mClickHandler.onClick(mBoundModificationContext, index);
    }
  }
}
