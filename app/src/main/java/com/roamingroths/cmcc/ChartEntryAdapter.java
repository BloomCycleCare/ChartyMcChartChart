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

  private final SortedSet<LocalDate> mPeakDays = new TreeSet<>();
  private final Set<LocalDate> mSeenDates = new HashSet<>();
  private final SortedList<ChartEntry> mEntries;
  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final OnItemAddedHandler mAddedHandler;
  private Cycle mCycle;
  private ChartEntryListener mEntryListener;

  public ChartEntryAdapter(Context context, OnClickHandler clickHandler, OnItemAddedHandler addedHandler) {
    mContext = context;
    mClickHandler = clickHandler;
    mAddedHandler = addedHandler;
    mEntries = new SortedList<ChartEntry>(ChartEntry.class, new SortedList.Callback<ChartEntry>() {

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
  }

  public void addEntry(ChartEntry entry) {
    maybeUpdatePeakDays(entry);
    if (mSeenDates.contains(entry.date)) {
      return;
    }
    mSeenDates.add(entry.date);
    int index = mEntries.add(entry);
    mAddedHandler.onItemAdded(entry, index);
  }

  public void changeEntry(String dateStr, String encryptedPayload) {
    final int entryIndex = findEntry(dateStr);
    if (entryIndex < 0) {
      throw new IllegalStateException("Couldn't find entry for update! " + dateStr);
    }
    ChartEntry.fromEncryptedString(
        encryptedPayload, mContext, new Callbacks.HaltingCallback<ChartEntry>() {
          @Override
          public void acceptData(ChartEntry entry) {
            maybeUpdatePeakDays(entry);
            mEntries.updateItemAt(entryIndex, entry);
          }
    });
  }

  public void removeEntry(String dateStr) {
    int entryIndex = findEntry(dateStr);
    if (entryIndex < 0) {
      throw new IllegalStateException("Couldn't find entry for update! " + dateStr);
    }
    ChartEntry entry = mEntries.get(entryIndex);
    maybeUpdatePeakDays(entry);
    mSeenDates.remove(entry.date);
    mEntries.removeItemAt(entryIndex);
  }

  public boolean isAttachedToCycle() {
    return mCycle != null;
  }

  public Cycle getCycle() {
    Preconditions.checkState(isAttachedToCycle());
    return mCycle;
  }

  public LocalDate getNextEntryDate() {
    if (mEntries.size() == 0) {
      return mCycle.startDate;
    }
    return mEntries.get(0).date.plusDays(1);
  }

  public void attachToCycle(final Cycle cycle, final Callbacks.Callback<Void> doneCallback) {
    if (isAttachedToCycle()) {
      if (mCycle.equals(cycle)) {
        doneCallback.acceptData(null);
        return;
      }
      detachFromCycle();
    }
    mEntryListener = new ChartEntryListener(mContext, this);
    mCycle = cycle;
    DataStore.fillCycleEntryAdapter(
        mCycle, mContext, this, new Callbacks.ErrorForwardingCallback<LocalDate>(doneCallback) {
          @Override
          public void acceptData(LocalDate lastEntryDate) {
            if (cycle.endDate == null && lastEntryDate.isBefore(DateUtil.now())) {
              DataStore.createEmptyEntries(mContext, cycle.id, lastEntryDate, null, this);
            }
            Log.v("ChartEntryAdapter", "Attaching ChartEntryListener");
            DataStore.attachCycleEntryListener(mEntryListener, cycle);
            doneCallback.acceptData(null);
          }
        });
  }

  @Nullable
  public Cycle detachFromCycle() {
    if (!isAttachedToCycle()) {
      return null;
    }
    DataStore.detatchCycleEntryListener(mEntryListener, mCycle);
    Cycle cycle = mCycle;
    mCycle = null;
    mEntryListener = null;
    mEntries.clear();
    mSeenDates.clear();
    return cycle;
  }

  private int findEntry(String dateStr) {
    for (int i = 0; i < mEntries.size(); i++) {
      ChartEntry entry = mEntries.get(i);
      if (entry.getDateStr().equals(dateStr)) {
        return i;
      }
    }
    return -1;
  }

  private void maybeUpdatePeakDays(ChartEntry entry) {
    if (entry.peakDay) {
      mPeakDays.add(entry.date);
    } else {
      mPeakDays.remove(entry.date);
    }
  }

  /**
   * This gets called when each new ViewHolder is created. This happens when the RecyclerView
   * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
   *
   * @param parent   The ViewGroup that these ViewHolders are contained within.
   * @param viewType If your RecyclerView has more than one type of item (which ours doesn't) you
   *                 can use this viewType integer to provide a different layout. See
   *                 {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
   *                 for more details.
   * @return A new EntryAdapterViewHolder that holds the View for each list item
   */
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
    ChartEntry entry = mEntries.get(position);
    holder.mEntryDataTextView.setText(entry.getListUiText());
    holder.mEntryBackgroundView.setBackgroundResource(entry.getEntryColorResource(mContext));
    holder.mEntryNumTextView.setText(String.valueOf(mEntries.size() - position));
    holder.mEntryDateTextView.setText(DateUtil.toWireStr(entry.date));
    holder.mEntryPeakTextView.setText(getPeakDayViewText(entry, mPeakDays));
    if (entry.intercourse) {
      holder.mEntryDataTextView.setTypeface(null, Typeface.BOLD);
    } else {
      holder.mEntryDateTextView.setTypeface(null, Typeface.NORMAL);
    }
    if (shouldShowBaby(entry)) {
      holder.mBabyImageView.setVisibility(View.VISIBLE);
    } else {
      holder.mBabyImageView.setVisibility(View.INVISIBLE);
    }
  }

  private static String getPeakDayViewText(ChartEntry entry, SortedSet<LocalDate> peakDays) {
    if (entry == null) {
      return "";
    }
    LocalDate closestPeakDay = null;
    for (LocalDate peakDay : peakDays) {
      if (peakDay.isAfter(entry.date)) {
        continue;
      }
      if (closestPeakDay == null) {
        closestPeakDay = peakDay;
      }
      if (closestPeakDay.isBefore(peakDay)) {

        closestPeakDay = peakDay;
      }
    }
    if (closestPeakDay == null) {
      return "";
    }
    return getPeakDayViewText(entry, closestPeakDay);
  }

  private static String getPeakDayViewText(ChartEntry entry, LocalDate peakDay) {
    if (entry.date.isBefore(peakDay)) {
      return "";
    }
    if (entry.date.equals(peakDay)) {
      return "P";
    }
    int daysAfterPeak = Days.daysBetween(peakDay, entry.date).getDays();
    if (daysAfterPeak < 4) {
      return String.valueOf(daysAfterPeak);
    }
    return "";
  }

  private static boolean shouldShowBaby(ChartEntry entry) {
    if (entry == null) {
      return false;
    }
    return entry.observation != null && entry.observation.hasMucus();
  }

  @Override
  public int getItemCount() {
    return mEntries.size();
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
      ChartEntry currentEntry = mEntries.get(index);
      mClickHandler.onClick(currentEntry, index);
    }
  }
}
