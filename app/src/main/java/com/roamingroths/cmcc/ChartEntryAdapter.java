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
import com.google.common.base.Strings;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.CryptoUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter
    extends RecyclerView.Adapter<ChartEntryAdapter.EntryAdapterViewHolder>
    implements ChildEventListener {

  private final SortedList<ChartEntry> mEntries;
  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private Date mCycleStartDate;
  private String mAttachedCycleId;

  public ChartEntryAdapter(Context context, OnClickHandler clickHandler) {
    mContext = context;
    mClickHandler = clickHandler;
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
        return e1.date.compareTo(e2.date);
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
    return !Strings.isNullOrEmpty(mAttachedCycleId);
  }

  public String getCycleId() {
    Preconditions.checkState(isAttachedToCycle());
    return mAttachedCycleId;
  }

  public Date getCycleStartDate() {
    Preconditions.checkState(isAttachedToCycle());
    return mCycleStartDate;
  }

  public void attachToCycle(String cycleId, Date cycleStartDate) {
    Preconditions.checkState(!isAttachedToCycle());
    DataStore.attachCycleEntryListener(this, cycleId);
    mAttachedCycleId = cycleId;
    mCycleStartDate = cycleStartDate;
  }

  public String detachFromCycle() {
    Preconditions.checkState(isAttachedToCycle());
    DataStore.detatchCycleEntryListener(this, mAttachedCycleId);
    String cycleId = mAttachedCycleId;
    mCycleStartDate = null;
    mAttachedCycleId = null;
    return cycleId;
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
      mEntries.add(ChartEntry.fromSnapshot(dataSnapshot, mContext));
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
    holder.mEntryDataTextView.setText(entry.observation.toString());
    holder.mEntryNumTextView.setText(String.valueOf(position + 1));
    holder.mEntryDateTextView.setText(DATE_FORMAT.format(entry.date));
  }

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");

  @Override
  public int getItemCount() {
    return mEntries.size();
  }

  public interface OnClickHandler {
    void onClick(ChartEntry entry, int index);
  }

  public class EntryAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
    public final TextView mEntryNumTextView;
    public final TextView mEntryDateTextView;
    public final TextView mEntryDataTextView;

    public EntryAdapterViewHolder(View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
      mEntryNumTextView = (TextView) itemView.findViewById(R.id.tv_entry_num);
      mEntryDateTextView = (TextView) itemView.findViewById(R.id.tv_entry_date);
      mEntryDataTextView = (TextView) itemView.findViewById(R.id.tv_entry_data);
    }

    @Override
    public void onClick(View v) {
      int index = getAdapterPosition();
      mClickHandler.onClick(mEntries.get(index), index);
    }
  }
}
