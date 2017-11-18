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
import com.roamingroths.cmcc.application.FirebaseApplication;
import com.roamingroths.cmcc.data.ChartEntryAdapter;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;

import java.util.ArrayList;

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
  private ArrayList<ChartEntry> mChartEntries;
  private Cycle mCycle;

  public void updateContainer(ChartEntry chartEntry) {
    mChartEntryAdapter.updateContainer(chartEntry);
  }

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
    mChartEntries = arguments.getParcelableArrayList(ChartEntry.class.getName());
    mCycle = arguments.getParcelable(Cycle.class.getName());

    mChartEntryAdapter = new ChartEntryAdapter(
        getActivity().getApplicationContext(), mCycle, this, new ChartEntryProvider(mDb, FirebaseApplication.getCryptoUtil()));
    mChartEntryAdapter.initialize(mChartEntries);

    if (DEBUG)
      Log.v(TAG, "onCreate() cycle:" + mCycle.id + ", mChartEntries:" + mChartEntries.size());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_list, container, false);

    mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_chart_list);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    mRecyclerView.setAdapter(mChartEntryAdapter);

    mChartEntryAdapter.notifyDataSetChanged();

    if (!mChartEntries.isEmpty()) {
      mView.showList();
    }

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    mRecyclerView.scrollToPosition(0);

  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  public void onClick(ChartEntry container, int index) {
    startActivityForResult(
        mChartEntryAdapter.getIntentForModification(container, index),
        EntryDetailActivity.MODIFY_REQUEST);
  }

  public Cycle getCycle() {
    return mCycle;
  }
}
