package com.roamingroths.cmcc.data;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.R;

import java.util.List;
import java.util.Map;

/**
 * Created by parkeroth on 4/18/17.
 */

public class WellnessAdapter extends RecyclerView.Adapter<WellnessAdapter.WellnessAdapterViewHolder> {

  private List<WellnessItem> wellnessItems = ImmutableList.of();
  private final Context mContext;

  public WellnessAdapter(Context context) {
    mContext = context;
  }

  public synchronized void updateData(Map<String, String> preferences) {
    Log.v("WellnessAdapter", "Updating with new preferences: " + preferences.size());
    ImmutableList.Builder<WellnessItem> builder = ImmutableList.builder();
    for (Map.Entry<String, String> entry : preferences.entrySet()) {
      builder.add(new WellnessItem(entry.getKey(), entry.getValue()));
    }
    wellnessItems = builder.build();
    notifyDataSetChanged();
  }

  /**
   * This gets called when each new ViewHolder is created. This happens when the RecyclerView
   * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
   *
   * @param parent   The ViewGroup that these ViewHolders are contained within.
   * @param viewType If your RecyclerView has more than one type of item (which ours doesn't) you
   *                 can use this viewType integer to provide a different layout. See
   *                 {@link RecyclerView.Adapter#getItemViewType(int)}
   *                 for more details.
   * @return A new EntryAdapterViewHolder that holds the View for each list item
   */
  @Override
  public WellnessAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.wellness_list_item;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new WellnessAdapterViewHolder(view);
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
  public void onBindViewHolder(WellnessAdapterViewHolder holder, int position) {
    WellnessItem item = wellnessItems.get(position);
    holder.mTitleTextView.setText(item.displayText);
    holder.mValueSwitch.setChecked(item.value);
  }

  @Override
  public int getItemCount() {
    return wellnessItems.size();
  }

  public class WellnessAdapterViewHolder extends RecyclerView.ViewHolder {
    public final TextView mTitleTextView;
    public final Switch mValueSwitch;

    public WellnessAdapterViewHolder(View itemView) {
      super(itemView);
      mTitleTextView = (TextView) itemView.findViewById(R.id.tv_wellness_item);
      mValueSwitch = (Switch) itemView.findViewById(R.id.switch_wellness_item);
    }
  }

  public static class WellnessItem {
    public final String key;
    public final String displayText;
    public boolean value = false;

    public WellnessItem(String key, String displayText) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
      Preconditions.checkArgument(!Strings.isNullOrEmpty(displayText));
      this.key = key;
      this.displayText = displayText;
    }
  }
}
