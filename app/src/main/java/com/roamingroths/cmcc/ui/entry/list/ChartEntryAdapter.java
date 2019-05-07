package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.models.ChartEntryList;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.providers.CycleProvider;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter extends RecyclerView.Adapter<ChartEntryViewHolder.Impl> {

  private static final boolean DEBUG = false;
  private static final String TAG = ChartEntryAdapter.class.getSimpleName();

  private final Context mContext;
  private final OnClickHandler mClickHandler;
  private final Preferences mPreferences;
  private final CycleProvider mCycleProvider;
  private final CompositeDisposable mDisposables;
  private ChartEntryList mContainerList;
  private String mLayerKey;

  ChartEntryAdapter(
      Context context,
      Cycle currentCycle,
      OnClickHandler clickHandler,
      CycleProvider cycleProvider,
      String layerKey) {
    mContext = context;
    mClickHandler = clickHandler;
    mLayerKey = layerKey;
    mCycleProvider = cycleProvider;
    mDisposables = new CompositeDisposable();
    mPreferences = Preferences.fromShared(mContext);
    mContainerList = ChartEntryList.builder(currentCycle, mPreferences).withAdapter(this).build();

    PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(
        (sharedPreferences, key) -> {
          mPreferences.update(sharedPreferences);
          notifyDataSetChanged();
        }
    );
  }

  void updateLayerKey(String key) {
    mLayerKey = key;
    notifyDataSetChanged();
  }

  void shutdown() {
    mDisposables.clear();
  }

  void initialize(List<ChartEntry> containers) {
    for (ChartEntry container : containers) {
      mContainerList.addEntry(container);
    }
    notifyDataSetChanged();
  }

  public Cycle getCycle() {
    return mContainerList.mCurrentCycle;
  }

  @NonNull
  @Override
  public ChartEntryViewHolder.Impl onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_observation_entry;
    LayoutInflater inflater = LayoutInflater.from(mContext);

    View view = inflater.inflate(layoutIdForListItem, parent, false);
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
  public void onBindViewHolder(@NonNull ChartEntryViewHolder.Impl holder, int position) {
    mContainerList.bindViewHolder(holder, position, mLayerKey);
  }

  @Override
  public int getItemCount() {
    return mContainerList.size();
  }

  public interface OnClickHandler {
    void onClick(ChartEntry container, int index);
  }

  Single<Intent> getIntentForModification(final ChartEntry chartEntry, final int index) {
    return mCycleProvider.hasPreviousCycle(FirebaseAuth.getInstance().getCurrentUser(), mContainerList.mCurrentCycle)
        .map(hasPreviousCycle -> {
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
          intent.putExtra(
              EntryDetailActivity.Extras.ASK_ESSENTIAL_SAMENESS_QUESTION.name(),
              mContainerList.askEssentialSamenessQuestion(index));
          return intent;
        });
  }
}
