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
import com.roamingroths.cmcc.data.CycleKeyProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 4/18/17.
 */

public class CycleAdapter extends RecyclerView.Adapter<CycleAdapter.CycleAdapterViewHolder>
    implements ChildEventListener {

  private final Context mContext;
  private final String mUserId;
  private final OnClickHandler mClickHandler;
  private final Map<String, Cycle> mCycleIndex;
  private final SortedList<Cycle> mCycles;
  private final CycleKeyProvider mCycleKeyProvider;

  public CycleAdapter(Context context, OnClickHandler clickHandler, String userId, CycleKeyProvider cycleKeyProvider) {
    mUserId = userId;
    mCycleIndex = new HashMap<>();
    mContext = context;
    mClickHandler = clickHandler;
    mCycleKeyProvider = cycleKeyProvider;
    mCycles = new SortedList<>(Cycle.class, new SortedList.Callback<Cycle>() {
      @Override
      public int compare(Cycle c1, Cycle c2) {
        return c2.startDate.compareTo(c1.startDate);
      }

      @Override
      public void onChanged(int position, int count) {
        notifyItemRangeChanged(position, count);
      }

      @Override
      public boolean areContentsTheSame(Cycle oldItem, Cycle newItem) {
        return oldItem.equals(newItem);
      }

      @Override
      public boolean areItemsTheSame(Cycle item1, Cycle item2) {
        return item1 == item2;
      }

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
    });
  }

  public Calendar[] getChartedDays() {
    ArrayList<Calendar> days = new ArrayList<>();
    for (int i=0; i<mCycles.size(); i++) {
      Cycle cycle = mCycles.get(i);
      if (cycle.endDate == null) {
      }
    }
    return (Calendar[]) days.toArray();
  }

  /**
   * This gets called when each new ViewHolder is created. This happens when the RecyclerView
   * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
   *
   * @param parent   The ViewGroup that these ViewHolders are contained within.
   * @param viewType If your RecyclerView has more than one type of item (which ours doesn't) you
   *                 can use this viewType integer to provide a different layout. See
   *                 {@link RecyclerView.Adapter#getItemViewType(int)}
   *                 for more details.
   * @return A new EntryAdapterViewHolder that holds the View for each list item
   */
  @Override
  public CycleAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.cycle_list_item;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new CycleAdapterViewHolder(view);
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
  public void onBindViewHolder(CycleAdapterViewHolder holder, int position) {
    // TODO: Bind real text to view
    Cycle cycle = mCycles.get(position);
    holder.mCycleDataTextView.setText("Cycle Starting: " + cycle.startDateStr);
  }

  @Override
  public int getItemCount() {
    return mCycles.size();
  }

  private int findCycle(String cycleId) {
    for (int i = 0; i < mCycles.size(); i++) {
      Cycle curCycle = mCycles.get(i);
      if (curCycle.id.equals(cycleId)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
    mCycleKeyProvider.forCycle(dataSnapshot.getKey()).getKey(mUserId, new Callbacks.HaltingCallback<SecretKey>() {
      @Override
      public void acceptData(SecretKey key) {
        Cycle cycle = Cycle.fromSnapshot(dataSnapshot, key);
        if (mCycleIndex.containsKey(cycle.id)) {
          maybeChangeCycle(cycle);
          return;
        }
        mCycleIndex.put(cycle.id, cycle);
        mCycles.add(cycle);
      }
    });
  }

  private void maybeChangeCycle(Cycle cycle) {
    Preconditions.checkArgument(mCycleIndex.containsKey(cycle.id));
    if (mCycleIndex.get(cycle.id).equals(cycle)) {
      return;
    }
    int index = getCycleIndex(cycle.id);
    if (index < 0) {
      throw new IllegalStateException();
    }
    mCycleIndex.put(cycle.id, cycle);
    mCycles.updateItemAt(index, cycle);
  }

  private int getCycleIndex(String cycleId) {
    for (int i = 0; i < mCycles.size(); i++) {
      Cycle cycle = mCycles.get(i);
      if (cycle.id.equals(cycleId)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void onChildChanged(final DataSnapshot dataSnapshot, String s) {
    mCycleKeyProvider.forCycle(dataSnapshot.getKey()).getKey(mUserId, new Callbacks.HaltingCallback<SecretKey>() {
      @Override
      public void acceptData(SecretKey key) {
        maybeChangeCycle(Cycle.fromSnapshot(dataSnapshot, key));
      }
    });
  }

  @Override
  public void onChildRemoved(DataSnapshot dataSnapshot) {
    int index = findCycle(dataSnapshot.getKey());
    if (index < 0) {
      throw new IllegalStateException("Couldn't find cycle for update!");
    }
    mCycleIndex.remove(mCycles.get(index).id);
    mCycles.removeItemAt(index);
  }

  @Override
  public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    throw new IllegalStateException("NOT IMPLEMENTED");
  }

  @Override
  public void onCancelled(DatabaseError databaseError) {
    databaseError.toException().printStackTrace();
  }

  public interface OnClickHandler {
    void onClick(Cycle cycle, int itemNum);
  }

  public class CycleAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
    public final TextView mCycleDataTextView;

    public CycleAdapterViewHolder(View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
      mCycleDataTextView = (TextView) itemView.findViewById(R.id.tv_cycle_data);
    }

    @Override
    public void onClick(View v) {
      int adapterPosition = getAdapterPosition();
      mClickHandler.onClick(mCycles.get(adapterPosition), adapterPosition);
    }
  }
}
