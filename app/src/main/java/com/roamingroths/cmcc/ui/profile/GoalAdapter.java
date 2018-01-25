package com.roamingroths.cmcc.ui.profile;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roamingroths.cmcc.R;

/**
 * Created by parkeroth on 1/24/18.
 */

public class GoalAdapter extends RecyclerView.Adapter<GoalViewHolder.Impl> {

  private final Context mContext;

  public GoalAdapter(Context context) {
    mContext = context;
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

  }

  @Override
  public int getItemCount() {
    return 0;
  }

  public interface OnClickHandler {
    void onClick(int index);
  }
}
