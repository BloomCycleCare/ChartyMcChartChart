package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;

import org.parceler.Parcels;

import java.util.ArrayList;
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
  private final CompositeDisposable mDisposables;
  private final boolean mHasPreviousCycle;
  private String mLayerKey;
  private List<CycleRenderer.RenderableEntry> mRenderableEntries = new ArrayList<>();

  ChartEntryAdapter(
      Context context,
      boolean hasPreviousCycle,
      OnClickHandler clickHandler,
      String layerKey) {
    mContext = context;
    mClickHandler = clickHandler;
    mLayerKey = layerKey;
    mDisposables = new CompositeDisposable();
    mHasPreviousCycle = hasPreviousCycle;
  }

  void updateLayerKey(String key) {
    mLayerKey = key;
    notifyDataSetChanged();
  }

  void updateRenderer(CycleRenderer renderer) {
    mRenderableEntries = renderer.render();
    notifyDataSetChanged();
  }

  void shutdown() {
    mDisposables.clear();
  }

  @NonNull
  @Override
  public ChartEntryViewHolder.Impl onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_observation_entry;
    LayoutInflater inflater = LayoutInflater.from(mContext);

    View view = inflater.inflate(layoutIdForListItem, parent, false);
    return new ChartEntryViewHolder.Impl(view, mClickHandler);
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
    holder.bind(mRenderableEntries.get(mRenderableEntries.size() - position - 1));
  }

  @Override
  public int getItemCount() {
    return mRenderableEntries.size();
  }

  public interface OnClickHandler {
    void onClick(CycleRenderer.EntryModificationContext modificationContext, int index);
  }

  Intent getIntentForModification(final CycleRenderer.EntryModificationContext modificationContext, final int index) {
    modificationContext.hasPreviousCycle = mHasPreviousCycle;
    Intent intent = new Intent(mContext, EntryDetailActivity.class);
    intent.putExtra(CycleRenderer.EntryModificationContext.class.getCanonicalName(), Parcels.wrap(modificationContext));
    return intent;
  }
}
