package com.roamingroths.cmcc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter extends RecyclerView.Adapter<ChartEntryViewHolder.Impl> {

  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final ChartEntryListener mListener;
  private final AtomicBoolean mEntryListenerAttached;
  private final DatabaseReference mEntriesDbRef;
  private final Preferences mPreferences;
  private ChartEntryList mChartEntryList;

  public ChartEntryAdapter(
      Context context,
      Cycle cycle,
      OnClickHandler clickHandler,
      FirebaseDatabase db,
      ChartEntryProvider chartEntryProvider,
      Callbacks.Callback<Void> initializationCompleteCallback) {
    mEntriesDbRef = db.getReference("entries").child(cycle.id);
    mEntriesDbRef.keepSynced(true);
    mEntryListenerAttached = new AtomicBoolean(false);
    mContext = context;
    mClickHandler = clickHandler;
    mPreferences = Preferences.fromShared(mContext);
    mChartEntryList = ChartEntryList.builder(cycle, mPreferences).withAdapter(this).build();
    mListener = new ChartEntryListener(context, mChartEntryList);
    mChartEntryList.initialize(chartEntryProvider, initializationCompleteCallback);

    PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(
        new SharedPreferences.OnSharedPreferenceChangeListener() {
          @Override
          public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            mPreferences.update(sharedPreferences);
            notifyDataSetChanged();
          }
        }
    );
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

  public Intent getIntentForModification(ChartEntry entry, int index) {
    Intent intent = new Intent(mContext, ChartEntryModifyActivity.class);
    intent.putExtra(Extras.ENTRY_DATE_STR, entry.getDateStr());
    intent.putExtra(Extras.EXPECT_UNUSUAL_BLEEDING, mChartEntryList.expectUnusualBleeding(index));
    intent.putExtra(Cycle.class.getName(), mChartEntryList.mCycle);
    return intent;
  }
}
