package com.roamingroths.cmcc.ui.profile;

import android.support.v4.util.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.annotations.Nullable;

/**
 * Created by parkeroth on 2/11/18.
 */

public class GoalSuggestionSet {

  public final List<GoalSuggestion> mBaseSuggestions = new ArrayList<>();

  public GoalSuggestionSet addRelative(
      GoalSuggestion.Action action, RelativeGoalSuggestion.Intent intent, List<String> objects) {
    for (String object : objects) {
      mBaseSuggestions.add(new RelativeGoalSuggestion(action, intent, object));
    }
    return this;
  }

  public GoalSuggestionSet addPeriodic(GoalSuggestion.Action action) {
    for (int i=1; i<4; i++) {
      for (PeriodicGoalSuggestion.Period period : PeriodicGoalSuggestion.Period.values()) {
        mBaseSuggestions.add(new PeriodicGoalSuggestion(action, i, period));
      }
    }
    return this;
  }

  public List<GoalSuggestion> getSuggestions(final String input) {
    Predicate<GoalSuggestion> inputFilter = new Predicate<GoalSuggestion>() {
      @Override
      public boolean apply(GoalSuggestion suggestion) {
        return suggestion.isActive(input);
      }
    };
    List<GoalSuggestion> filteredSuggestions = Lists.newArrayList(Iterables.filter(mBaseSuggestions, inputFilter));
    Map<GoalSuggestion.Action, Set<RelativeGoalSuggestion.Intent>> relativeActionIndex = new HashMap<>();
    Map<GoalSuggestion.Action, Set<Integer>> periodicActionIndex = new HashMap<>();
    for (GoalSuggestion suggestion : filteredSuggestions) {
      if (suggestion instanceof RelativeGoalSuggestion) {
        if (!relativeActionIndex.containsKey(suggestion.action)) {
          relativeActionIndex.put(suggestion.action, new HashSet<RelativeGoalSuggestion.Intent>());
        }
        relativeActionIndex.get(suggestion.action).add(((RelativeGoalSuggestion) suggestion).intent);
      }
      if (suggestion instanceof PeriodicGoalSuggestion) {
        if (!periodicActionIndex.containsKey(suggestion.action)) {
          periodicActionIndex.put(suggestion.action, new HashSet<Integer>());
        }
        periodicActionIndex.get(suggestion.action).add(((PeriodicGoalSuggestion) suggestion).instances.num());
      }
    }
    List<GoalSuggestion> suggestions = new ArrayList<>();
    // Add one entry for combination of (action, intent) and (action)
    if (relativeActionIndex.keySet().size() + periodicActionIndex.keySet().size() > 1) {
      for (Map.Entry<GoalSuggestion.Action, Set<RelativeGoalSuggestion.Intent>> entry : relativeActionIndex.entrySet()) {
        for (RelativeGoalSuggestion.Intent intent : entry.getValue()) {
          suggestions.add(new RelativeGoalSuggestion(entry.getKey(), intent, "..."));
        }
      }
      for (Map.Entry<GoalSuggestion.Action, Set<Integer>> entry : periodicActionIndex.entrySet()) {
        suggestions.add(new PeriodicGoalSuggestion(entry.getKey(), 0, null));
      }
      return suggestions;
    }
    // Add one entry for combination of (action, instances)
    if (periodicActionIndex.size() == 1) {
      for (Map.Entry<GoalSuggestion.Action, Set<Integer>> entry : periodicActionIndex.entrySet()) {
        for (Integer i : entry.getValue()) {
          if (entry.getValue().size() == 1) {
            suggestions.add(new PeriodicGoalSuggestion(entry.getKey(), i, PeriodicGoalSuggestion.Period.PER_DAY));
            suggestions.add(new PeriodicGoalSuggestion(entry.getKey(), i, PeriodicGoalSuggestion.Period.PER_WEEK));
            suggestions.add(new PeriodicGoalSuggestion(entry.getKey(), i, PeriodicGoalSuggestion.Period.PER_MONTH));
          } else {
            suggestions.add(new PeriodicGoalSuggestion(entry.getKey(), i, PeriodicGoalSuggestion.Period.EMPTY));
          }
        }
      }
      return Lists.newArrayList(Iterables.filter(suggestions, inputFilter));
    }
    return filteredSuggestions;
  }
}
