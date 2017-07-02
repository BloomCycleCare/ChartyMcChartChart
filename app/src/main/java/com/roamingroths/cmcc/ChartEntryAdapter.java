package com.roamingroths.cmcc;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter extends RecyclerView.Adapter<ChartEntryViewHolder.Impl> {

  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final OnItemAddedHandler mAddedHandler;
  private final ChartEntryListener mListener;
  private final AtomicBoolean mEntryListenerAttached;
  private final DatabaseReference mEntriesDbRef;
  private ChartEntryList mChartEntryList;

  public ChartEntryAdapter(
      Context context,
      Cycle cycle,
      OnClickHandler clickHandler,
      OnItemAddedHandler addedHandler,
      FirebaseDatabase db,
      Callbacks.Callback<Void> initializationCompleteCallback) {
    mEntriesDbRef = db.getReference("entries").child(cycle.id);
    mEntriesDbRef.keepSynced(true);
    mEntryListenerAttached = new AtomicBoolean(false);
    mContext = context;
    mClickHandler = clickHandler;
    mAddedHandler = addedHandler;
    mChartEntryList = ChartEntryList.builder(cycle, Preferences.fromShared(mContext)).withAdapter(this).build();
    mListener = new ChartEntryListener(context, mChartEntryList);
    mChartEntryList.initialize(context, initializationCompleteCallback);
  }

  public synchronized void attachListener() {
    if (mEntryListenerAttached.compareAndSet(false, true)) {
      mEntriesDbRef.addChildEventListener(mListener);
    } else {
      Log.w("ChartEntryAdapter", "Already attached!");
    }
  }

  public synchronized void detachListener() {
    if (mEntryListenerAttached.compareAndSet(true, false)) {
      mEntriesDbRef.removeEventListener(mListener);
    } else {
      Log.w("ChartEntryAdapter", "Not attached!");
    }
  }

  public Cycle getCycle() {
    return mChartEntryList.mCycle;
  }

  @Override
  public ChartEntryViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.observation_list_item;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new ChartEntryViewHolder.Impl(view, mChartEntryList, mClickHandler);
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
  public void onBindViewHolder(ChartEntryViewHolder.Impl holder, int position) {
    mChartEntryList.bindViewHolder(holder, position, mContext);
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
}
