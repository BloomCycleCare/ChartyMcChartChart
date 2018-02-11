package com.roamingroths.cmcc.ui.profile;

import java.util.List;

/**
 * Created by parkeroth on 2/11/18.
 */

public abstract class GoalSuggestion {

  public enum Action {
    EAT, DRINK, JOURNAL, MEDITATE
  }

  public final Action action;

  protected GoalSuggestion(Action action) {
    this.action = action;
  }

  public abstract boolean isActive(String input);

  public abstract String toString();

  public abstract boolean equals(Object obj);

  public abstract int hashCode();
}
