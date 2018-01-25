package com.roamingroths.cmcc.ui.profile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.chart.ChartEntry;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryAdapter;

/**
 * Created by parkeroth on 11/13/17.
 */

public class GoalListFragment extends Fragment implements ChartEntryAdapter.OnClickHandler {

  private static boolean DEBUG = true;
  private static String TAG = GoalListFragment.class.getSimpleName();

  private RecyclerView mRecyclerView;
  private TextView mAddGoalsView;
  private GoalAdapter mAdapter;

  public GoalListFragment() {
    if (DEBUG) Log.v(TAG, "Construct");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mAdapter = new GoalAdapter(getContext());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_goal_list, container, false);

    mAddGoalsView = view.findViewById(R.id.tv_add_goals);
    mRecyclerView = view.findViewById(R.id.recyclerview_goal_list);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    mRecyclerView.setAdapter(mAdapter);
    mAdapter.notifyDataSetChanged();

    hideList();

    return view;
  }

  @Override
  public void onClick(ChartEntry container, int index) {

  }

  private void showList() {
    mRecyclerView.setVisibility(View.VISIBLE);
    mAddGoalsView.setVisibility(View.GONE);
  }

  private void hideList() {
    mRecyclerView.setVisibility(View.GONE);
    mAddGoalsView.setVisibility(View.VISIBLE);
  }

}
