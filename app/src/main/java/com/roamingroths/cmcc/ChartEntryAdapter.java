package com.roamingroths.cmcc;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by parkeroth on 4/18/17.
 */

public class ChartEntryAdapter extends RecyclerView.Adapter<ChartEntryAdapter.EntryAdapterViewHolder> {

  private int mNumEntries;
  private final Context mContext;
  private final ChartEntryAdapterOnClickHandler mClickHandler;

  public ChartEntryAdapter(Context context, ChartEntryAdapterOnClickHandler clickHandler) {
    mNumEntries = 20;
    mContext = context;
    mClickHandler = clickHandler;
  }

  public int addEntry() {
    return mNumEntries++;
  }

  /**
   * This gets called when each new ViewHolder is created. This happens when the RecyclerView
   * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
   *
   * @param parent   The ViewGroup that these ViewHolders are contained within.
   * @param viewType If your RecyclerView has more than one type of item (which ours doesn't) you
   *                 can use this viewType integer to provide a different layout. See
   *                 {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
   *                 for more details.
   * @return A new EntryAdapterViewHolder that holds the View for each list item
   */
  @Override
  public EntryAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.observation_list_item;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new EntryAdapterViewHolder(view);
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
  public void onBindViewHolder(EntryAdapterViewHolder holder, int position) {
    // TODO: Bind real text to view
    holder.mEntryDataTextView.setText("ChartEntry #" + position);
  }

  @Override
  public int getItemCount() {
    return mNumEntries;
  }

  public interface ChartEntryAdapterOnClickHandler {
    void onClick(int itemNum);
  }

  public class EntryAdapterViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
    public final TextView mEntryDataTextView;

    public EntryAdapterViewHolder(View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
      mEntryDataTextView = (TextView) itemView.findViewById(R.id.tv_entry_data);
    }

    @Override
    public void onClick(View v) {
      int adapterPosition = getAdapterPosition();
      mClickHandler.onClick(adapterPosition);
    }
  }
}
