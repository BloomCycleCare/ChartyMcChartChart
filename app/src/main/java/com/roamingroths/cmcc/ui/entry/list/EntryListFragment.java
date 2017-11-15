package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.ChartEntryAdapter;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.EntryContainer;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;

import java.util.List;

/**
 * Created by parkeroth on 11/13/17.
 */

public class EntryListFragment extends Fragment implements ChartEntryAdapter.OnClickHandler {

  private static boolean DEBUG = true;
  private static String TAG = EntryListFragment.class.getSimpleName();

  private RecyclerView mRecyclerView;

  private ChartEntryAdapter mChartEntryAdapter;
  private EntryListView mView;

  private FirebaseDatabase mDb;
  private CycleProvider mCycleProvider;
  private List<EntryContainer> mEntryContainers;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    mView = (ChartEntryListActivity) getActivity();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mDb = FirebaseDatabase.getInstance();
    mCycleProvider = CycleProvider.forDb(mDb);

    Bundle arguments = getArguments();
    mEntryContainers = arguments.getParcelableArrayList(EntryContainer.class.getName());
    Cycle cycle = arguments.getParcelable(Cycle.class.getName());

    mChartEntryAdapter = new ChartEntryAdapter(
        getActivity().getApplicationContext(), cycle, this, mDb, mCycleProvider);
    mChartEntryAdapter.initialize(mEntryContainers);

    if (DEBUG)
      Log.v(TAG, "onCreate() cycle:" + cycle.id + ", mEntryContainers:" + mEntryContainers.size());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_list, container, false);

    mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_chart_list);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    mRecyclerView.setAdapter(mChartEntryAdapter);

    mChartEntryAdapter.notifyDataSetChanged();

    if (!mEntryContainers.isEmpty()) {
      mView.showList();
    }

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    mChartEntryAdapter.attachListener();
    mRecyclerView.scrollToPosition(0);

  }

  @Override
  public void onPause() {
    super.onPause();
    mChartEntryAdapter.detachListener();
  }

  @Override
  public void onClick(EntryContainer container, int index) {
    startActivityForResult(
        mChartEntryAdapter.getIntentForModification(container, index),
        EntryDetailActivity.MODIFY_REQUEST);
  }
}
