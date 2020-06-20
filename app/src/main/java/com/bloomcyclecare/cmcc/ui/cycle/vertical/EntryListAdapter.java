package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;
import com.bloomcyclecare.cmcc.ui.showcase.ShowcaseManager;
import com.google.common.collect.Iterables;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
  private final Consumer<RenderedEntry> mTextClickConsumer;
  private final Consumer<RenderedEntry> mStickerClickConsumer;
  private final CompositeDisposable mDisposables;
  private final ShowcaseManager mShowcaseManager;

  private String mLayerKey;
  private ViewMode viewMode;
  private List<RenderedEntry> mRenderedEntries = new ArrayList<>();
  private Optional<LocalDate> mDateToShowcase;
  private boolean mShowcaseStickerSelection;

  EntryListAdapter(
      Context context,
      String layerKey,
      Consumer<RenderedEntry> textClickConsumer,
      Consumer<RenderedEntry> stickerClickConsumer,
      ShowcaseManager showcaseManager) {
    mTextClickConsumer = textClickConsumer;
    mStickerClickConsumer = stickerClickConsumer;
    mContext = context;
    mLayerKey = layerKey;
    mDisposables = new CompositeDisposable();
    mShowcaseManager = showcaseManager;
  }

  void updateLayerKey(String key) {
    mLayerKey = key;
    notifyDataSetChanged();
  }

  void update(
      @NonNull Cycle cycle,
      @NonNull List<RenderedEntry> renderedEntries,
      @NonNull ViewMode viewMode,
      @NonNull Optional<LocalDate> entryDateToShowcase,
      boolean showcaseStickerSelection) {
    mDateToShowcase = entryDateToShowcase;
    mShowcaseStickerSelection = showcaseStickerSelection;
    this.viewMode = viewMode;
    if (!Iterables.elementsEqual(mRenderedEntries, renderedEntries)) {
      Timber.v("Updating entries for cycle %s", cycle.startDate);
      mRenderedEntries = renderedEntries;
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
    return new ChartEntryViewHolder.Impl(mContext, view, mTextClickConsumer, mStickerClickConsumer, mShowcaseManager);
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
    RenderedEntry re = mRenderedEntries.get(mRenderedEntries.size() - position - 1);
    boolean showcaseEntry = mDateToShowcase.isPresent() && re.entryDate().equals(mDateToShowcase.get());
    boolean showcaseStickerSelection = showcaseEntry && mShowcaseStickerSelection;
    holder.bind(re, viewMode, showcaseEntry, showcaseStickerSelection);
  }

  @Override
  public int getItemCount() {
    return mRenderedEntries.size();
  }
}
