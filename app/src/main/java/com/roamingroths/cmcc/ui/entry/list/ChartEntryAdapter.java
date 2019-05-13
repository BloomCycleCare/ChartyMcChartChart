package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.models.ChartEntryList;
import com.roamingroths.cmcc.ui.entry.detail.EntryContext;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;

import org.parceler.Parcels;

import java.util.List;

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
  private final CompositeDisposable mDisposables;
  private final boolean mHasPreviousCycle;
  private ChartEntryList mContainerList;
  private String mLayerKey;

  ChartEntryAdapter(
      Context context,
      Cycle currentCycle,
      boolean hasPreviousCycle,
      OnClickHandler clickHandler,
      String layerKey) {
    mContext = context;
    mClickHandler = clickHandler;
    mLayerKey = layerKey;
    mDisposables = new CompositeDisposable();
    mPreferences = Preferences.fromShared(mContext);
    mContainerList = ChartEntryList.builder(currentCycle, mPreferences).withAdapter(this).build();
    mHasPreviousCycle = hasPreviousCycle;

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

  Intent getIntentForModification(final ChartEntry chartEntry, final int index) {
    EntryContext entryContext = mContainerList.getEntryContext(index);
    entryContext.hasPreviousCycle = mHasPreviousCycle;
    Intent intent = new Intent(mContext, EntryDetailActivity.class);
    intent.putExtra(EntryContext.class.getCanonicalName(), Parcels.wrap(entryContext));
    return intent;
  }
}
