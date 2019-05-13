package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roamingroths.cmcc.R;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by parkeroth on 11/23/17.
 */

public class LayerListFragment extends Fragment {

  public static final String KEYS_ARG = "keys";
  public static final String VALS_ARG = "vals";

  private int mKeyArrayId;
  private int mValArrayId;

  private LayerAdapter mAdapter;
  private RecyclerView mRecyclerView;
  private EntryListView mEntryListView;
  private DialogFragment mParent;

  static LayerListFragment newInstance(int keyArrayId, int valArrayId) {
    LayerListFragment fragment = new LayerListFragment();
    Bundle args = new Bundle();
    args.putInt(KEYS_ARG, keyArrayId);
    args.putInt(VALS_ARG, valArrayId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mEntryListView = (EntryListView) getActivity();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    mKeyArrayId = args.getInt(KEYS_ARG);
    mValArrayId = args.getInt(VALS_ARG);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_layer_list, null, false);
    mRecyclerView = view.findViewById(R.id.layer_recycler_view);

    mParent = (DialogFragment) getParentFragment();

    String[] keys = getResources().getStringArray(mKeyArrayId);
    String[] vals = getResources().getStringArray(mValArrayId);
    Map<String, String> wellnessItems = new HashMap<>();
    for (int i=0; i < keys.length; i++) {
      wellnessItems.put(keys[i], vals[i]);
    }
    mAdapter = new LayerAdapter(wellnessItems, getActivity());
    mAdapter.clickStream().subscribe(new Consumer<String>() {
      @Override
      public void accept(String s) throws Exception {
        mEntryListView.setOverlay(s);
        mParent.dismiss();
      }
    });

    mRecyclerView.setLayoutManager(
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    mRecyclerView.setAdapter(mAdapter);

    return view;
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
      int layoutIdForListItem = R.layout.list_item_layer;
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
      mOptionTitle = itemView.findViewById(R.id.tv_layer_item);
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
