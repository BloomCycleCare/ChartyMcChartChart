package com.roamingroths.cmcc.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by parkeroth on 9/17/17.
 */

public class MultiSelectPrefAdapter extends RecyclerView.Adapter<MultiSelectPrefAdapter.MultiSelectPrefViewHolder> {

  private final Context mContext;
  private final int mLayoutIdForListItem;
  private final int mLayoutIdForHolderTitle;
  private final int mLayoutIdForHolderSwitch;

  private final LinkedHashMap<String, String> mItems = new LinkedHashMap<>();
  private ListOrderedMap<String, Boolean> mActiveItems = new ListOrderedMap<>();

  public MultiSelectPrefAdapter(
      Context context,
      int layoutIdForListItem,
      int layoutIdForHolderTitle,
      int layoutIdForHolderSwitch,
      String[] values,
      String[] keys,
      Bundle savedInstanceState) {
    mContext = context;
    mLayoutIdForListItem = layoutIdForListItem;
    mLayoutIdForHolderTitle = layoutIdForHolderTitle;
    mLayoutIdForHolderSwitch = layoutIdForHolderSwitch;

    for (int i = 0; i < keys.length; i++) {
      mItems.put(keys[i], values[i]);
    }

    if (savedInstanceState != null) {
      for (String key : mItems.keySet()) {
        if (savedInstanceState.getBoolean(key, false)) {
          mActiveItems.put(key, true);
        }
      }
    }
  }

  public void updateActiveItems(Set<String> activeKeys) {
    Log.v("MultiSelectPrefAdapter", "Active items: " + activeKeys.size());
    ListOrderedMap<String, Boolean> updatedItems = new ListOrderedMap<>();
    for (Map.Entry<String, String> entry : mItems.entrySet()) {
      String key = entry.getKey();
      if (activeKeys.contains(key)) {
        boolean val = mActiveItems.containsKey(key) ? mActiveItems.get(key) : false;
        updatedItems.put(entry.getKey(), val);
      }
    }
    mActiveItems = updatedItems;
    notifyDataSetChanged();
  }

  public Bundle fillBundle(Bundle bundle) {
    for (Map.Entry<String, Boolean> entry : mActiveItems.entrySet()) {
      bundle.putBoolean(entry.getKey(), entry.getValue());
    }
    return bundle;
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
  public MultiSelectPrefViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;
    View view = inflater.inflate(mLayoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new MultiSelectPrefViewHolder(view);
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
  public void onBindViewHolder(MultiSelectPrefViewHolder holder, int position) {
    final String key = mActiveItems.get(position);
    holder.mTitleTextView.setText(mItems.get(key));
    holder.mValueSwitch.setChecked(mActiveItems.get(key));
    holder.mValueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mActiveItems.put(key, isChecked);
      }
    });
  }

  @Override
  public int getItemCount() {
    return mActiveItems.size();
  }

  public class MultiSelectPrefViewHolder extends RecyclerView.ViewHolder {

    public final TextView mTitleTextView;
    public final Switch mValueSwitch;

    public MultiSelectPrefViewHolder(View itemView) {
      super(itemView);
      mTitleTextView = (TextView) checkNotNull(itemView.findViewById(mLayoutIdForHolderTitle));
      mValueSwitch = (Switch) checkNotNull(itemView.findViewById(mLayoutIdForHolderSwitch));
    }
  }
}
