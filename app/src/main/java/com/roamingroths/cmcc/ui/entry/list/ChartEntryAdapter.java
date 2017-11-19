package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roamingroths.cmcc.Extras;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.ChartEntryList;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;
import com.roamingroths.cmcc.utils.DateUtil;

import java.util.List;

import durdinapps.rxfirebase2.RxFirebaseChildEvent;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter extends RecyclerView.Adapter<ChartEntryViewHolder.Impl> {

  private static final boolean DEBUG = false;
  private static final String TAG = ChartEntryAdapter.class.getSimpleName();

  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final Cycle mCycle;
  private final Preferences mPreferences;
  private final ChartEntryProvider mChartEntryProvider;
  private final CompositeDisposable mDisposables;
  private ChartEntryList mContainerList;

  public ChartEntryAdapter(
      Context context,
      Cycle cycle,
      OnClickHandler clickHandler,
      ChartEntryProvider chartEntryProvider) {
    mContext = context;
    mCycle = cycle;
    mClickHandler = clickHandler;
    mChartEntryProvider = chartEntryProvider;
    mDisposables = new CompositeDisposable();
    mPreferences = Preferences.fromShared(mContext);
    mContainerList = ChartEntryList.builder(mCycle, mPreferences).withAdapter(this).build();

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

  public void start() {
    mDisposables.add(mChartEntryProvider.entryStream(mCycle)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<RxFirebaseChildEvent<ChartEntry>>() {
          @Override
          public void accept(RxFirebaseChildEvent<ChartEntry> childEvent) throws Exception {
            switch (childEvent.getEventType()) {
              case ADDED:
                if (DEBUG)
                  Log.v(TAG, "Add " + childEvent.getValue().entryDate + " to " + mContainerList.mCycle.id);
                mContainerList.addEntry(childEvent.getValue());
                break;
              case CHANGED:
                if (DEBUG)
                  Log.v(TAG, "Change " + childEvent.getValue().entryDate + " for " + mContainerList.mCycle.id);
                mContainerList.changeEntry(childEvent.getValue());
                break;
              case MOVED:
                throw new UnsupportedOperationException();
              case REMOVED:
                if (DEBUG)
                  Log.v(TAG, "Remove " + childEvent.getValue().entryDate + " from " + mContainerList.mCycle.id);
                mContainerList.removeEntry(childEvent.getValue());
                break;
            }
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            Log.e(TAG, "Error on child changed.", throwable);
          }
        }));
  }

  public void shutdown() {
    mDisposables.clear();
  }

  public void initialize(List<ChartEntry> containers) {
    for (ChartEntry container : containers) {
      mContainerList.addEntry(container);
    }
    notifyDataSetChanged();
  }

  public void updateContainer(ChartEntry container) {
    mContainerList.changeEntry(container);
  }

  public Cycle getCycle() {
    return mContainerList.mCycle;
  }

  @Override
  public ChartEntryViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.observation_list_item;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new ChartEntryViewHolder.Impl(view, mContainerList, mClickHandler);
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
    mContainerList.bindViewHolder(holder, position);
  }

  @Override
  public int getItemCount() {
    return mContainerList.size();
  }

  public interface OnClickHandler {
    void onClick(ChartEntry container, int index);
  }

  public Intent getIntentForModification(ChartEntry container, int index) {
    Intent intent = new Intent(mContext, EntryDetailActivity.class);
    intent.putExtra(Extras.ENTRY_DATE_STR, DateUtil.toWireStr(container.entryDate));
    intent.putExtra(Extras.EXPECT_UNUSUAL_BLEEDING, mContainerList.expectUnusualBleeding(index));
    intent.putExtra(Cycle.class.getName(), mContainerList.mCycle);
    intent.putExtra(ChartEntry.class.getName(), container);
    return intent;
  }
}
