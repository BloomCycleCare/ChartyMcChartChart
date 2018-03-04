package com.roamingroths.cmcc.ui.goals.list;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.goals.Goal;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parkeroth on 1/24/18.
 */

public class GoalListAdapter extends RecyclerView.Adapter<GoalViewHolder.Impl> {

  private final Context mContext;
  private List<Goal> mGoals;

  public GoalListAdapter(Context context) {
    mContext = context;
    mGoals = new ArrayList<>();
  }

  public void updateGoals(List<Goal> newGoals) {
    mGoals = newGoals;
    notifyDataSetChanged();
  }

  @Override
  public GoalViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_goal;
    LayoutInflater inflater = LayoutInflater.from(mContext);
    boolean shouldAttachToParentImmediately = false;

    View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
    return new GoalViewHolder.Impl(view);
  }

  @Override
  public void onBindViewHolder(GoalViewHolder.Impl holder, int position) {
    holder.bind(mGoals.get(position));
  }

  @Override
  public int getItemCount() {
    return mGoals.size();
  }

  public interface OnClickHandler {
    void onClick(int index);
  }
}
