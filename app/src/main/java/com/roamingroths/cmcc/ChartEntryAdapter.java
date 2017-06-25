package com.roamingroths.cmcc;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter
    extends RecyclerView.Adapter<ChartEntryAdapter.EntryAdapterViewHolder> {

  private final Set<LocalDate> mSeenDates = new HashSet<>();
  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final OnItemAddedHandler mAddedHandler;

  private ChartEntryList mChartEntryList;

  public ChartEntryAdapter(Context context, Cycle cycle, OnClickHandler clickHandler, OnItemAddedHandler addedHandler) {
    mContext = context;
    mClickHandler = clickHandler;
    mAddedHandler = addedHandler;

    SortedList<ChartEntry> entries = new SortedList<ChartEntry>(ChartEntry.class, new SortedList.Callback<ChartEntry>() {
      @Override
      public void onInserted(int position, int count) {
        notifyItemRangeInserted(position, count);
      }

      @Override
      public void onRemoved(int position, int count) {
        notifyItemRangeRemoved(position, count);
      }

      @Override
      public void onMoved(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
      }

      @Override
      public int compare(ChartEntry e1, ChartEntry e2) {
        return e2.date.compareTo(e1.date);
      }

      @Override
      public void onChanged(int position, int count) {
        notifyItemRangeChanged(position, count);
      }

      @Override
      public boolean areContentsTheSame(ChartEntry oldItem, ChartEntry newItem) {
        return oldItem.equals(newItem);
      }

      @Override
      public boolean areItemsTheSame(ChartEntry item1, ChartEntry item2) {
        return item1 == item2;
      }
    });

    mChartEntryList = ChartEntryList.create(cycle, entries, context, new Callbacks.HaltingCallback<ChartEntryList>() {
      @Override
      public void acceptData(ChartEntryList mChartEntryList) {
        // Do nothing
      }
    });
  }

  @Override
  public EntryAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.observation_list_item;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new EntryAdapterViewHolder(view);
  }

  /**
   * OnBindViewHolder is called by the RecyclerView to display the data at the specified
   * position. In this method, we update the contents of the ViewHolder to display the weather
   * details for this particular position, using the "position" argument that is conveniently
   * passed into us.
   *
   * @param holder   The ViewHolder which should be updated to represent the
   *                 contents of the item at the given position in the data set.
   * @param position The position of the item within the adapter's data set.
   */
  @Override
  public void onBindViewHolder(EntryAdapterViewHolder holder, int position) {
    ChartEntry entry = mChartEntryList.get(position);
    holder.mEntryDataTextView.setText(entry.getListUiText());
    holder.mEntryBackgroundView.setBackgroundResource(entry.getEntryColorResource(mContext));
    holder.mEntryNumTextView.setText(String.valueOf(mChartEntryList.size() - position));
    holder.mEntryDateTextView.setText(DateUtil.toWireStr(entry.date));
    holder.mEntryPeakTextView.setText(mChartEntryList.getPeakDayViewText(entry));
    if (entry.intercourse) {
      holder.mEntryDataTextView.setTypeface(null, Typeface.BOLD);
    } else {
      holder.mEntryDateTextView.setTypeface(null, Typeface.NORMAL);
    }
    if (mChartEntryList.shouldShowBaby(entry)) {
      holder.mBabyImageView.setVisibility(View.VISIBLE);
    } else {
      holder.mBabyImageView.setVisibility(View.INVISIBLE);
    }
  }


  @Override
  public int getItemCount() {
    return mChartEntryList.size();
  }

  public interface OnClickHandler {
    void onClick(ChartEntry entry, int index);
  }

  public interface OnItemAddedHandler {
    void onItemAdded(ChartEntry entry, int index);
  }

  public class EntryAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
    public final TextView mEntryNumTextView;
    public final TextView mEntryDateTextView;
    public final TextView mEntryDataTextView;
    public final TextView mEntryPeakTextView;
    public final ImageView mBabyImageView;
    public final View mEntryBackgroundView;

    public EntryAdapterViewHolder(View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
      mEntryNumTextView = (TextView) itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = (TextView) itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = (TextView) itemView.findViewById(R.id.tv_entry_data);
      mEntryPeakTextView = (TextView) itemView.findViewById(R.id.tv_peak_day);
      mBabyImageView = (ImageView) itemView.findViewById(R.id.baby_image_view);
      mEntryBackgroundView = itemView.findViewById(R.id.entry_item_layout);
    }

    @Override
    public void onClick(View v) {
      int index = getAdapterPosition();
      int previousEntryIndex = index + 1;
      ChartEntry currentEntry = mChartEntryList.get(index);
      mClickHandler.onClick(currentEntry, index);
    }
  }
}
