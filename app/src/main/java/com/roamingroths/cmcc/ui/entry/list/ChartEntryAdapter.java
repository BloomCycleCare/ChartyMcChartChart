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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.perf.metrics.AddTrace;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.chart.ChartEntry;
import com.roamingroths.cmcc.logic.chart.ChartEntryList;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.providers.ChartEntryProvider;
import com.roamingroths.cmcc.providers.CycleProvider;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;

import java.util.List;

import durdinapps.rxfirebase2.RxFirebaseChildEvent;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter extends RecyclerView.Adapter<ChartEntryViewHolder.Impl> {

  private static final boolean DEBUG = false;
  private static final String TAG = ChartEntryAdapter.class.getSimpleName();

  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final Preferences mPreferences;
  private final ChartEntryProvider mChartEntryProvider;
  private final CycleProvider mCycleProvider;
  private final CompositeDisposable mDisposables;
  private ChartEntryList mContainerList;
  private String mLayerKey;

  public ChartEntryAdapter(
      Context context,
      Cycle currentCycle,
      OnClickHandler clickHandler,
      ChartEntryProvider chartEntryProvider,
      CycleProvider cycleProvider,
      String layerKey) {
    mContext = context;
    mClickHandler = clickHandler;
    mLayerKey = layerKey;
    mChartEntryProvider = chartEntryProvider;
    mCycleProvider = cycleProvider;
    mDisposables = new CompositeDisposable();
    mPreferences = Preferences.fromShared(mContext);
    mContainerList = ChartEntryList.builder(currentCycle, mPreferences).withAdapter(this).build();

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

  public void updateLayerKey(String key) {
    mLayerKey = key;
    notifyDataSetChanged();
  }

  public void start() {
    mDisposables.add(mChartEntryProvider.entryStream(mContainerList.mCurrentCycle)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<RxFirebaseChildEvent<ChartEntry>>() {
          @Override
          public void accept(RxFirebaseChildEvent<ChartEntry> childEvent) throws Exception {
            switch (childEvent.getEventType()) {
              case ADDED:
                if (DEBUG)
                  Log.v(TAG, "Add " + childEvent.getValue().entryDate + " to " + mContainerList.mCurrentCycle.id);
                mContainerList.addEntry(childEvent.getValue());
                break;
              case CHANGED:
                if (DEBUG)
                  Log.v(TAG, "Change " + childEvent.getValue().entryDate + " for " + mContainerList.mCurrentCycle.id);
                mContainerList.changeEntry(childEvent.getValue());
                break;
              case MOVED:
                throw new UnsupportedOperationException();
              case REMOVED:
                if (DEBUG)
                  Log.v(TAG, "Remove " + childEvent.getValue().entryDate + " from " + mContainerList.mCurrentCycle.id);
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
    return mContainerList.mCurrentCycle;
  }

  @Override
  @AddTrace(name = "onCreateViewHolder")
  public ChartEntryViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_observation_entry;
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
    mContainerList.bindViewHolder(holder, position, mLayerKey);
  }

  @Override
  public int getItemCount() {
    return mContainerList.size();
  }

  public interface OnClickHandler {
    void onClick(ChartEntry container, int index);
  }

  public Single<Intent> getIntentForModification(final ChartEntry chartEntry, final int index) {
    return mCycleProvider.hasPreviousCycle(FirebaseAuth.getInstance().getCurrentUser(), mContainerList.mCurrentCycle)
        .map(new Function<Boolean, Intent>() {
          @Override
          public Intent apply(Boolean hasPreviousCycle) throws Exception {
            Intent intent = new Intent(mContext, EntryDetailActivity.class);
            intent.putExtra(
                EntryDetailActivity.Extras.CHART_ENTRY.name(), chartEntry);
            intent.putExtra(
                EntryDetailActivity.Extras.EXPECT_UNUSUAL_BLEEDING.name(),
                mContainerList.expectUnusualBleeding(index));
            intent.putExtra(
                EntryDetailActivity.Extras.CURRENT_CYCLE.name(), mContainerList.mCurrentCycle);
            intent.putExtra(
                EntryDetailActivity.Extras.HAS_PREVIOUS_CYCLE.name(), hasPreviousCycle);
            intent.putExtra(
                EntryDetailActivity.Extras.IS_FIRST_ENTRY.name(), index == getItemCount() - 1);
            return intent;
          }
        });
  }
}
