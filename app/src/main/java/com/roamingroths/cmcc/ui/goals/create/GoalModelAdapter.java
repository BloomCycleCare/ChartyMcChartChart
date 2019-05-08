package com.roamingroths.cmcc.ui.goals.create;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.goals.GoalModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parkeroth on 1/24/18.
 */

public class GoalModelAdapter extends RecyclerView.Adapter<GoalModelViewHolder.Impl> {

  private final OnClickHandler mClickHandler;
  private final Context mContext;
  private List<GoalModel> mActiveTemplates = new ArrayList<>();

  public GoalModelAdapter(Context context, OnClickHandler clickHandler) {
    mContext = context;
    mClickHandler = clickHandler;
  }

  public void updateItems(List<GoalModel> models) {
    mActiveTemplates = models;
    notifyDataSetChanged();
  }

  public GoalModel getTemplate(int position) {
    return mActiveTemplates.get(position);
  }

  @Override
  public GoalModelViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_goal_suggestion;
    LayoutInflater inflater = LayoutInflater.from(mContext);

    View view = inflater.inflate(layoutIdForListItem, parent, false);
    return new GoalModelViewHolder.Impl(view, mClickHandler);
  }

  @Override
  public void onBindViewHolder(GoalModelViewHolder.Impl holder, int position) {
    GoalModel template = mActiveTemplates.get(position);
    holder.bind(template);
  }

  @Override
  public int getItemCount() {
    return mActiveTemplates.size();
  }

  public interface OnClickHandler {
    void onClick(int index);
  }
}
