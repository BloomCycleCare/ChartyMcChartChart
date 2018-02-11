package com.roamingroths.cmcc.ui.profile;

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.R;

import static com.roamingroths.cmcc.ui.profile.RelativeGoalSuggestion.Action;
import static com.roamingroths.cmcc.ui.profile.RelativeGoalSuggestion.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parkeroth on 1/24/18.
 */

public class GoalSuggestionAdapter extends RecyclerView.Adapter<GoalSuggestionViewHolder.Impl> {

  private static final List<String> DRINKS_GOOD = ImmutableList.of("water");
  private static final List<String> DRINKS_BAD = ImmutableList.of("alcohol", "caffeine");
  private static final List<String> FOOD_BAD = ImmutableList.of("gluten", "dairy", "sugar");

  private static final GoalSuggestionSet SUGGESTIONS = new GoalSuggestionSet()
      .addPeriodic(Action.JOURNAL)
      .addPeriodic(Action.MEDITATE)
      .addRelative(Action.EAT, Intent.LESS, FOOD_BAD)
      .addRelative(Action.DRINK, Intent.MORE, DRINKS_GOOD)
      .addRelative(Action.DRINK, Intent.LESS, DRINKS_BAD);

  private final OnClickHandler mClickHandler;
  private final Context mContext;
  private List<GoalSuggestion> activeSuggestions = new ArrayList<>();

  public GoalSuggestionAdapter(Context context, OnClickHandler clickHandler) {
    mContext = context;
    mClickHandler = clickHandler;
    updateItems("");
  }

  public void updateItems(String input) {
    activeSuggestions = SUGGESTIONS.getSuggestions(input);
    notifyDataSetChanged();
  }

  public GoalSuggestion getSuggestion(int position) {
    return activeSuggestions.get(position);
  }

  @Override
  public GoalSuggestionViewHolder.Impl onCreateViewHolder(ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_goal_suggestion;
    LayoutInflater inflater = LayoutInflater.from(mContext);

    View view = inflater.inflate(layoutIdForListItem, parent, false);
    return new GoalSuggestionViewHolder.Impl(view, mClickHandler);
  }

  @Override
  public void onBindViewHolder(GoalSuggestionViewHolder.Impl holder, int position) {
    GoalSuggestion suggestion = activeSuggestions.get(position);
    holder.bind(suggestion);
  }

  @Override
  public int getItemCount() {
    return activeSuggestions.size();
  }

  public interface OnClickHandler {
    void onClick(int index);
  }
}
