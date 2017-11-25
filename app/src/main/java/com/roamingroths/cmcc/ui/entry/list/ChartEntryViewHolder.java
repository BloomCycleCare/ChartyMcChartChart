package com.roamingroths.cmcc.ui.entry.list;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.ChartEntryList;
import com.roamingroths.cmcc.logic.ChartEntry;

/**
 * Created by parkeroth on 7/1/17.
 */

public interface ChartEntryViewHolder extends View.OnClickListener {
  void setEntrySummary(String summary);

  void setBackgroundColor(int colorResourceId);

  void setEntryNum(int num);

  void setDate(String dateStr);

  void setPeakDayText(String text);

  void setIntercourse(boolean val);

  void setShowBaby(boolean val);

  void setOverlay(boolean val);

  void setWeekTransition(boolean val);

  class Impl extends RecyclerView.ViewHolder implements ChartEntryViewHolder {
    private final ChartEntryAdapter.OnClickHandler mClickHandler;
    private final ChartEntryList mContainerList;
    private final TextView mEntryNumTextView;
    private final TextView mEntryDateTextView;
    private final TextView mEntryDataTextView;
    private final TextView mEntryPeakTextView;
    private final ImageView mBabyImageView;
    private final ImageView mStarImageView;
    private final View mEntryBackgroundView;
    private final View mSeparator;
    private final View mWeekSeparator;

    public Impl(
        View itemView, ChartEntryList containerList, ChartEntryAdapter.OnClickHandler clickHandler) {
      super(itemView);
      itemView.setOnClickListener(this);
      mContainerList = containerList;
      mClickHandler = clickHandler;
      mEntryNumTextView = (TextView) itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = (TextView) itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = (TextView) itemView.findViewById(R.id.tv_entry_data);
      mEntryPeakTextView = (TextView) itemView.findViewById(R.id.tv_peak_day);
      mBabyImageView = (ImageView) itemView.findViewById(R.id.baby_image_view);
      mStarImageView = (ImageView) itemView.findViewById(R.id.star_image_view);
      mEntryBackgroundView = itemView.findViewById(R.id.entry_item_layout);
      mSeparator = itemView.findViewById(R.id.observation_list_separator);
      mWeekSeparator = itemView.findViewById(R.id.week_separator);
    }

    @Override
    public void setEntrySummary(String summary) {
      mEntryDataTextView.setText(summary);
    }

    @Override
    public void setBackgroundColor(int colorResourceId) {
      mEntryBackgroundView.setBackgroundResource(colorResourceId);
    }

    @Override
    public void setEntryNum(int num) {
      mEntryNumTextView.setText(String.valueOf(num));
    }

    @Override
    public void setDate(String dateStr) {
      mEntryDateTextView.setText(dateStr);
    }

    @Override
    public void setPeakDayText(String text) {
      mEntryPeakTextView.setText(text);
    }

    @Override
    public void setIntercourse(boolean val) {
      if (val) {
        mEntryDataTextView.setTypeface(null, Typeface.BOLD);
      } else {
        mEntryDateTextView.setTypeface(null, Typeface.NORMAL);
      }
    }

    @Override
    public void setShowBaby(boolean val) {
      if (val) {
        mBabyImageView.setVisibility(View.VISIBLE);
      } else {
        mBabyImageView.setVisibility(View.INVISIBLE);
      }
    }

    @Override
    public void setOverlay(boolean val) {
      if (val) {
        mStarImageView.setVisibility(View.VISIBLE);
      } else {
        mStarImageView.setVisibility(View.INVISIBLE);
      }
    }

    @Override
    public void onClick(View v) {
      int index = getAdapterPosition();
      ChartEntry container = mContainerList.get(index);
      mClickHandler.onClick(container, index);
    }

    @Override
    public void setWeekTransition(boolean val) {
      mWeekSeparator.setVisibility(val ? View.VISIBLE : View.GONE);
    }
  }
}
