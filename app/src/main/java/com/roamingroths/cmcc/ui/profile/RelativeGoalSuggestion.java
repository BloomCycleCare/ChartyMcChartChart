package com.roamingroths.cmcc.ui.profile;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by parkeroth on 2/11/18.
 */

public class RelativeGoalSuggestion extends GoalSuggestion {

  public enum Intent {
    MORE, LESS, FOR, ONCE_PER
  }

  public final Intent intent;
  public final String object;

  public RelativeGoalSuggestion(Action action, Intent intent, String object) {
    super(action);
    this.intent = intent;
    this.object = object;
  }

  @Override
  public boolean isActive(String input) {
    String lowerInput = input.toLowerCase();
    List<String> parts = Lists.newArrayList(lowerInput.split(" "));

    // Include all actions each with only one object
    if (parts.isEmpty()) {
      return true;
    }

    // Only include options for matching actions
    if (!action.name().toLowerCase().startsWith(parts.get(0))) {
      return false;
    }

    if (parts.size() == 1) {
      return true;
    }

    if (!intent.name().toLowerCase().startsWith(parts.get(1))) {
      return false;
    }

    if (parts.size() == 2) {
      return true;
    }

    return object.toLowerCase().startsWith(parts.get(2));
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append(action.name().toLowerCase()).append(" ")
        .append(intent.name().toLowerCase()).append(" ")
        .append(object.toLowerCase())
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RelativeGoalSuggestion) {
      RelativeGoalSuggestion that = (RelativeGoalSuggestion) obj;
      return this.action.equals(that.action) &&
          this.intent.equals(that.intent) &&
          this.object.equals(that.object);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(action, intent, object);
  }
}
