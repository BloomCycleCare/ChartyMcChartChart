package com.roamingroths.cmcc.ui.entry.list;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.roamingroths.cmcc.R;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by parkeroth on 11/19/17.
 */

public class LayerDialogFragment extends DialogFragment {

  private LayerAdapter mAdapter;
  private RecyclerView mRecyclerView;
  private EntryListView mEntryListView;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mEntryListView = (EntryListView) getActivity();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();

    View view = inflater.inflate(R.layout.dialog_layer, null, false);
    mRecyclerView = (RecyclerView) view.findViewById(R.id.layer_recycler_view);

    String[] values = getActivity().getResources().getStringArray(R.array.pref_wellness_option_values);
    String[] keys = getActivity().getResources().getStringArray(R.array.pref_wellness_option_keys);
    Map<String, String> wellnessItems = new HashMap<>();
    for (int i=0; i < keys.length; i++) {
      wellnessItems.put(keys[i], values[i]);
    }
    mAdapter = new LayerAdapter(wellnessItems, getActivity());
    mAdapter.clickStream().subscribe(new Consumer<String>() {
      @Override
      public void accept(String s) throws Exception {
        mEntryListView.setOverlay(s);
        dismiss();
      }
    });

    mRecyclerView.setLayoutManager(
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    mRecyclerView.setAdapter(mAdapter);

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setView(view);
    builder.setTitle("Select an item");
    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        dialogInterface.dismiss();
      }
    });
    builder.setPositiveButton("Clear", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        mEntryListView.clearOverlay();
        dialogInterface.dismiss();
      }
    });

    return builder.create();
  }

  private static class LayerAdapter extends RecyclerView.Adapter<LayerOptionViewHolder> {
    private final Map<Integer, String> wellnessItemIndex = new HashMap<>();
    private final Map<String, String> wellnessItems = new LinkedHashMap<>();
    private final Context mContex;
    private final PublishSubject<String> mClickPublisher = PublishSubject.create();

    public LayerAdapter(Map<String, String> wellnessItems, Context context) {
      mContex = context;
      int index = 0;
      for (Map.Entry<String, String> entry : wellnessItems.entrySet()) {
        this.wellnessItemIndex.put(index++, entry.getKey());
        this.wellnessItems.put(entry.getKey(), entry.getValue());
      }
    }

    public PublishSubject<String> clickStream() {
      return mClickPublisher;
    }

    @Override
    public LayerOptionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      int layoutIdForListItem = R.layout.layer_list_item;
      LayoutInflater inflater = LayoutInflater.from(mContex);
      boolean shouldAttachToParentImmediately = false;
      View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);

      return new LayerOptionViewHolder(view, mClickPublisher);
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

  private static class LayerOptionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView mOptionTitle;
    private PublishSubject<String> mPublisher;

    private String key;

    public LayerOptionViewHolder(View itemView, PublishSubject<String> publisher) {
      super(itemView);
      itemView.setOnClickListener(this);
      mPublisher = publisher;
      mOptionTitle = (TextView) itemView.findViewById(R.id.tv_layer_item);
    }

    public void bind(String key, String value) {
      this.key = key;
      mOptionTitle.setText(value);
    }

    public String getKey() {
      return key;
    }

    @Override
    public void onClick(View view) {
      mPublisher.onNext(key);
    }
  }
}
