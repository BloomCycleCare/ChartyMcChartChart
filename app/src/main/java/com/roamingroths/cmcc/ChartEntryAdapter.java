package com.roamingroths.cmcc;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter
    extends RecyclerView.Adapter<ChartEntryAdapter.EntryAdapterViewHolder>
    implements ChildEventListener {

  private final Set<String> mSeenDates = new HashSet<>();
  private final SortedList<ChartEntry> mEntries;
  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final OnItemAddedHandler mAddedHandler;
  private Cycle mCycle;

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

  public void attachToCycle(Cycle cycle) {
    if (isAttachedToCycle()) {
      if (mCycle.equals(cycle)) {
        return;
      }
      detachFromCycle();
    }
    DataStore.attachCycleEntryListener(this, cycle);
    mCycle = cycle;
  }

  public void detachFromCycle() {
    if (!isAttachedToCycle()) {
      return;
    }
    DataStore.detatchCycleEntryListener(this, mCycle);
    mCycle = null;
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

  @Override
  public void onChildAdded(DataSnapshot dataSnapshot, String s) {
    try {
      ChartEntry entry = ChartEntry.fromSnapshot(dataSnapshot, mContext);
      if (mSeenDates.contains(entry.getDateStr())) {
        return;
      }
      mSeenDates.add(entry.getDateStr());
      int index = mEntries.add(entry);
      mAddedHandler.onItemAdded(entry, index);
    } catch (CryptoUtil.CryptoException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    String dateStr = dataSnapshot.getKey();
    int entryIndex = findEntry(dateStr);
    if (entryIndex < 0) {
      throw new IllegalStateException("Couldn't find entry for update! " + dateStr);
    }
    try {
      mEntries.updateItemAt(entryIndex, ChartEntry.fromSnapshot(dataSnapshot, mContext));
    } catch (CryptoUtil.CryptoException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onChildRemoved(DataSnapshot dataSnapshot) {
    String dateStr = dataSnapshot.getKey();
    int entryIndex = findEntry(dateStr);
    if (entryIndex < 0) {
      throw new IllegalStateException("Couldn't find entry for update! " + dateStr);
    }
    ChartEntry entry = mEntries.get(entryIndex);
    mSeenDates.remove(entry.getDateStr());
    mEntries.removeItemAt(entryIndex);
  }

  @Override
  public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    throw new IllegalStateException("NOT IMPLEMENTED");
  }

  @Override
  public void onCancelled(DatabaseError databaseError) {
    databaseError.toException().printStackTrace();
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
    holder.mEntryNumTextView.setText(String.valueOf(mEntries.size() - position));
    holder.mEntryDateTextView.setText(DateUtil.toWireStr(entry.date));
    holder.mEntryBackgroundView.setBackgroundResource(entry.getEntryColorResource());
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
    public final View mEntryBackgroundView;

    public EntryAdapterViewHolder(View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
      mEntryNumTextView = (TextView) itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = (TextView) itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = (TextView) itemView.findViewById(R.id.tv_entry_data);
      mEntryBackgroundView = itemView.findViewById(R.id.entry_item_layout);
    }

    @Override
    public void onClick(View v) {
      int index = getAdapterPosition();
      mClickHandler.onClick(mEntries.get(index), index);
    }
  }
}
