package com.roamingroths.cmcc.ui.entry.list;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.ChartEntryAdapter;
import com.roamingroths.cmcc.data.EntryContainerList;
import com.roamingroths.cmcc.logic.EntryContainer;

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

  class Impl extends RecyclerView.ViewHolder implements ChartEntryViewHolder {
    private final ChartEntryAdapter.OnClickHandler mClickHandler;
    private final EntryContainerList mContainerList;
    private final TextView mEntryNumTextView;
    private final TextView mEntryDateTextView;
    private final TextView mEntryDataTextView;
    private final TextView mEntryPeakTextView;
    private final ImageView mBabyImageView;
    private final View mEntryBackgroundView;

    public Impl(
        View itemView, EntryContainerList containerList, ChartEntryAdapter.OnClickHandler clickHandler) {
      super(itemView);
      itemView.setOnClickListener(this);
      mContainerList = containerList;
      mClickHandler = clickHandler;
      mEntryNumTextView = (TextView) itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = (TextView) itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = (TextView) itemView.findViewById(R.id.tv_entry_data);
      mEntryPeakTextView = (TextView) itemView.findViewById(R.id.tv_peak_day);
      mBabyImageView = (ImageView) itemView.findViewById(R.id.baby_image_view);
      mEntryBackgroundView = itemView.findViewById(R.id.entry_item_layout);
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
    public void onClick(View v) {
      int index = getAdapterPosition();
      EntryContainer container = mContainerList.get(index);
      mClickHandler.onClick(container, index);
    }
  }
}
