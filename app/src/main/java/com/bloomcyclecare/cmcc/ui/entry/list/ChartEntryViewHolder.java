package com.bloomcyclecare.cmcc.ui.entry.list;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by parkeroth on 7/1/17.
 */

public interface ChartEntryViewHolder extends View.OnClickListener, View.OnLongClickListener {

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
    private final Context mContext;

    private CycleRenderer.RenderableEntry mBoundEntry;

    public Impl(
        Context context,
        View itemView,
        ChartEntryAdapter.OnClickHandler clickHandler) {
      super(itemView);
      mContext = context;
      itemView.setOnClickListener(this);
      itemView.setOnLongClickListener(this);
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
      mSymptomGoalSummaryView.setText(renderableEntry.essentialSamenessSummary);

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

      mBoundEntry = renderableEntry;
    }

    @Override
    public void onClick(View v) {
      int index = getAdapterPosition();
      mClickHandler.onClick(mBoundEntry.modificationContext, index);
    }

    @Override
    public boolean onLongClick(View v) {
      new AlertDialog.Builder(mContext)
          .setTitle("Instruction Summary")
          .setMessage(mBoundEntry.instructionSummary)
          .setPositiveButton("Edit", (dialogInterface, i) -> {
            onClick(v);
            dialogInterface.dismiss();
          })
          .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss())
          .create()
          .show();
      return true;
    }
  }
}
