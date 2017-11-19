package com.roamingroths.cmcc.ui.entry.list;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by parkeroth on 11/19/17.
 */

public class LayerDialogFragment extends DialogFragment {

  private RecyclerView mRecyclerView;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_layer, container, false);
    mRecyclerView = (RecyclerView) view.findViewById(R.id.layer_recycler_view);

    String[] values = getActivity().getResources().getStringArray(R.array.pref_wellness_option_values);
    String[] keys = getActivity().getResources().getStringArray(R.array.pref_wellness_option_keys);
    Map<String, String> wellnessItems = new HashMap<>();
    for (int i=0; i < keys.length; i++) {
      wellnessItems.put(keys[i], values[i]);
    }
    mRecyclerView.setAdapter(new LayerAdapter(wellnessItems));

    return view;
  }

  private static class LayerAdapter extends RecyclerView.Adapter<LayerOptionViewHolder> {
    private final Map<Integer, String> wellnessItemIndex = new HashMap<>();
    private final Map<String, String> wellnessItems = new LinkedHashMap<>();

    public LayerAdapter(Map<String, String> wellnessItems) {
      int index = 0;
      for (Map.Entry<String, String> entry : wellnessItems.entrySet()) {
        this.wellnessItemIndex.put(index++, entry.getKey());
        this.wellnessItems.put(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public LayerOptionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new LayerOptionViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(LayerOptionViewHolder holder, int position) {
      String key = wellnessItemIndex.get(position);
      holder.bind(key, wellnessItems.get(key));
    }

    @Override
    public int getItemCount() {
      return wellnessItems.size();
    }
  }

  private static class LayerOptionViewHolder extends RecyclerView.ViewHolder {
    private TextView mOptionTitle;
    private RadioButton mRadioButton;

    private String key;

    public LayerOptionViewHolder(View itemView) {
      super(itemView);

      mOptionTitle = (TextView) itemView.findViewById(R.id.tv_layer_item);
      mRadioButton = (RadioButton) itemView.findViewById(R.id.radio_button_layer);
    }

    public void bind(String key, String value) {
      this.key = key;
      mOptionTitle.setText(value);
    }

    public String getKey() {
      return key;
    }
  }
}
