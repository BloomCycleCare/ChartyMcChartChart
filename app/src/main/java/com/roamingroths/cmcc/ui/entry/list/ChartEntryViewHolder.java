package com.roamingroths.cmcc.ui.entry.list;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

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
    private final TextView mPocSummaryTextView;
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
      mPocSummaryTextView = itemView.findViewById(R.id.tv_tv_poc_summary);
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
      mPocSummaryTextView.setText(renderableEntry.pocSummary);

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

      List<String> parts = new ArrayList<>();
      parts.add(String.format("%s: ", renderableEntry.dateSummary));
      if (!renderableEntry.infertilityReasons.isEmpty()) {
        parts.add(String.format("infertility[%s]", Joiner.on(",").join(
            Collections2.transform(renderableEntry.infertilityReasons, i -> String.format("%s.%s", i.section(), i.subsection())))));
      }
      if (!renderableEntry.fertilityReasons.isEmpty()) {
        parts.add(String.format("fertility[%s]", Joiner.on(",").join(
            Collections2.transform(renderableEntry.fertilityReasons, i -> String.format("%s.%s", i.section(), i.subsection())))));
      }
      if (!renderableEntry.suppressedFertilityReasons.isEmpty()) {
        parts.add(String.format("suppressed[%s]", Joiner.on(",").join(
            Collections2.transform(renderableEntry.suppressedFertilityReasons.entrySet(),
                e -> String.format("%s|%s", e.getKey(), e.getValue())))));
      }
      if (!renderableEntry.relaxedInfertilityReasons.isEmpty()) {
        parts.add(String.format("relaxed[%s]", Joiner.on(",").join(
            Collections2.transform(renderableEntry.relaxedInfertilityReasons.entrySet(),
                e -> String.format("%s|%s", e.getKey(), e.getValue())))));
      }
      Timber.d(Joiner.on(", ").join(parts));
    }

    @Override
    public void onClick(View v) {
      int index = getAdapterPosition();
      mClickHandler.onClick(mBoundModificationContext, index);
    }
  }
}
