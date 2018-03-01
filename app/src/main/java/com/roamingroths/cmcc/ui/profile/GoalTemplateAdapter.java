package com.roamingroths.cmcc.ui.profile;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.goals.GoalModel;
import com.roamingroths.cmcc.logic.goals.GoalModelFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parkeroth on 1/24/18.
 */

public class GoalTemplateAdapter extends RecyclerView.Adapter<GoalTemplateViewHolder.Impl> {

  private final OnClickHandler mClickHandler;
  private final Context mContext;
  private List<GoalModel> mActiveTemplates = new ArrayList<>();
  private GoalModelFactory mFactory = GoalModelFactory.withDefaultTemplates();

  public GoalTemplateAdapter(Context context, OnClickHandler clickHandler) {
    mContext = context;
    mClickHandler = clickHandler;
    updateItems("");
  }

  public void updateItems(String input) {
    mActiveTemplates = mFactory.fromInput(input, 3);
    notifyDataSetChanged();
  }

  public GoalModel getTemplate(int position) {
    return mActiveTemplates.get(position);
  }

  @Override
  public GoalTemplateViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_goal_suggestion;
    LayoutInflater inflater = LayoutInflater.from(mContext);

    View view = inflater.inflate(layoutIdForListItem, parent, false);
    return new GoalTemplateViewHolder.Impl(view, mClickHandler);
  }

  @Override
  public void onBindViewHolder(GoalTemplateViewHolder.Impl holder, int position) {
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
