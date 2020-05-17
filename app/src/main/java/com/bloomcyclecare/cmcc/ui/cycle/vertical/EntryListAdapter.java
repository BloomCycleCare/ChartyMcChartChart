package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

/**
 * Created by parkeroth on 4/18/17.
 */

class EntryListAdapter extends RecyclerView.Adapter<ChartEntryViewHolder.Impl> {

  private final Context mContext;
  private final Consumer<CycleRenderer.RenderableEntry> mTextClickConsumer;
  private final Consumer<CycleRenderer.RenderableEntry> mStickerClickConsumer;
  private final CompositeDisposable mDisposables;

  private String mLayerKey;
  private ViewMode viewMode;
  private List<CycleRenderer.RenderableEntry> mRenderableEntries = new ArrayList<>();
  private Map<LocalDate, StickerSelection> mStickerSelections = new HashMap<>();
  private boolean mAutoStickeringEnabled;

  EntryListAdapter(
      Context context,
      String layerKey,
      Consumer<CycleRenderer.RenderableEntry> textClickConsumer,
      Consumer<CycleRenderer.RenderableEntry> stickerClickConsumer) {
    mTextClickConsumer = textClickConsumer;
    mStickerClickConsumer = stickerClickConsumer;
    mContext = context;
    mLayerKey = layerKey;
    mDisposables = new CompositeDisposable();
  }

  void updateLayerKey(String key) {
    mLayerKey = key;
    notifyDataSetChanged();
  }

  void update(
      @NonNull CycleRenderer.RenderableCycle renderableCycle,
      @NonNull ViewMode viewMode,
      boolean autoStickeringEnabled,
      @NonNull Map<LocalDate, StickerSelection> stickerSelections) {
    this.viewMode = viewMode;
    if (!Iterables.elementsEqual(mRenderableEntries, renderableCycle.entries())
        || !Iterables.elementsEqual(mStickerSelections.entrySet(), stickerSelections.entrySet())) {
      Timber.v("Updating entries for cycle %s", renderableCycle.cycle().startDate);
      mRenderableEntries = renderableCycle.entries();
      mStickerSelections = stickerSelections;
      mAutoStickeringEnabled = autoStickeringEnabled;
      notifyDataSetChanged();
    }
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
    return new ChartEntryViewHolder.Impl(mContext, view, re -> {
      if (viewMode == ViewMode.TRAINING) {
        if (!Strings.isNullOrEmpty(re.trainingMarker())) {
          Timber.d("TODO: prompt for entry");
        } else {
          Timber.d("Only prompting on training entries with markers");
        }
        return;
      }
      if (viewMode != ViewMode.CHARTING) {
        Timber.d("Not navigating to detail activity while in mode: %s", viewMode.name());
        return;
      }
      mTextClickConsumer.accept(re);
    }, re -> {
      if (mAutoStickeringEnabled) {
        Timber.d("Not prompting for sticker selection while auto stickering is enabled");
        return;
      }
      if (viewMode != ViewMode.CHARTING) {
        Timber.d("Not prompting for sticker selection in mode: %s", viewMode.name());
        return;
      }
      mStickerClickConsumer.accept(re);
    });
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
    CycleRenderer.RenderableEntry re =
        mRenderableEntries.get(mRenderableEntries.size() - position - 1);
    boolean showSticker = mAutoStickeringEnabled || mStickerSelections.containsKey(re.entry().entryDate);
    holder.bind(re, viewMode, showSticker);
  }

  @Override
  public int getItemCount() {
    return mRenderableEntries.size();
  }
}
