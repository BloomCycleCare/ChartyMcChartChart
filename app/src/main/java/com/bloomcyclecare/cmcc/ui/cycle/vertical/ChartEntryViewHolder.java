package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;
import com.bloomcyclecare.cmcc.ui.showcase.ShowcaseManager;

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
    private final View mStickerView;
    private final View mSeparator;
    private final View mStrikeView;
    private final Context mContext;
    private final ShowcaseManager mShowcaseManager;
    private final View mItemView;

    private RenderedEntry mBoundEntry;
    private ViewMode mBoundViewMode;

    public Impl(
        Context context,
        View itemView,
        Consumer<RenderedEntry> textClickConsumer,
        Consumer<RenderedEntry> stickerClickConsumer,
        ShowcaseManager showcaseManager) {
      super(itemView);
      mItemView = itemView;
      mShowcaseManager = showcaseManager;
      mContext = context;
      mEntryNumTextView = itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = itemView.findViewById(R.id.tv_entry_data);
      mEntryPeakTextView = itemView.findViewById(R.id.tv_peak_day);
      mPocSummaryTextView = itemView.findViewById(R.id.tv_tv_poc_summary);
      mSymptomGoalSummaryView = itemView.findViewById(R.id.tv_goal_symptom_summary);
      mStickerView = itemView.findViewById(R.id.baby_image_view);
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

    public void bind(RenderedEntry re, ViewMode viewMode, boolean showcaseEntry, boolean showcaseStickerSelection) {
      mStickerView.setVisibility(View.VISIBLE);

      mEntryDataTextView.setText(re.observationSummary());
      mEntryNumTextView.setText(re.entryNum());
      mEntryDateTextView.setText(re.entryDateStr());
      mStickerView.setBackgroundResource(re.stickerBackgroundResource());
      mStrikeView.setVisibility(re.showStickerStrike() ? View.VISIBLE : View.GONE);
      mEntryPeakTextView.setText(re.stickerText());
      mPocSummaryTextView.setText(re.pocSummaryText());
      mSymptomGoalSummaryView.setText(re.essentialSamenessSummary());
      mSymptomGoalSummaryView.setVisibility(View.VISIBLE);

      if (re.showWeekTransition()) {
        mSeparator.setBackgroundColor(mContext.getColor(R.color.week_separator));
      } else {
        mSeparator.setBackgroundColor(mContext.getColor(R.color.entry_separator));
      }

      mBoundEntry = re;
      mBoundViewMode = viewMode;

      if (re.showStickerStrike()) {
        mShowcaseManager.showShowcase(ShowcaseManager.ShowcaseID.FIRST_INCORRECT_STICKER, mStrikeView);
      }
      if (showcaseEntry) {
        mShowcaseManager.showShowcase(ShowcaseManager.ShowcaseID.OBSERVATION_INPUT, mEntryDataTextView);
      }
      if (showcaseStickerSelection) {
        mShowcaseManager.showShowcase(ShowcaseManager.ShowcaseID.STICKER_SELECTION, mStickerView);
      }
    }
  }
}
