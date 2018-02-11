package com.roamingroths.cmcc.ui.profile;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by parkeroth on 2/11/18.
 */

public class PeriodicGoalSuggestion extends GoalSuggestion {

  public final Period period;
  public final Instances instances;
  public final SpecialInstances specialInstances;

  public PeriodicGoalSuggestion(Action action, int instances, Period period) {
    super(action);
    this.period = period;
    if (instances == 0) {
      this.instances = null;
      this.specialInstances = null;
    } else {
      this.instances = Instances.valueOf(instances);
      this.specialInstances = SpecialInstances.valueOf(instances);
    }
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

    if (!isValidInstances(parts.get(1))) {
      return false;
    }

    // Skip over "time" or "times"
    int nextIndex = isValidSpecialInstance(parts.get(1)) ? 2 : 3;

    if (nextIndex >= parts.size()) {
      return true;
    }

    if (!"per".startsWith(parts.get(nextIndex))) {
      return false;
    }

    return isValidPeriod(parts.get(nextIndex + 1));
  }

  private boolean isValidPeriod(String part) {
    return period.name().startsWith(part);
  }

  private boolean isValidSpecialInstance(String part) {
    return specialInstances != null && specialInstances.name().startsWith(part);
  }

  private boolean isValidInstances(String part) {
    if (isValidSpecialInstance(part)) {
      return true;
    }
    if (instances == null) {
      return false;
    }
    if (instances.name().startsWith(part)) {
      return true;
    }
    try {
      return Integer.valueOf(part) == instances.num();
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder()
        .append(action.name().toLowerCase()).append(" ");
    if (instances == null) {
      builder.append("...");
    } else {
      builder.append(getInstanceStr()).append(" ").append(getPeriodStr());
    }
    return builder.toString();
  }

  private String getInstanceStr() {
    if (specialInstances != null) {
      return specialInstances.name();
    }
    return String.valueOf(instances.num()) + " times";
  }

  private String getPeriodStr() {
    switch (period) {
      case EMPTY:
        return "...";
      case PER_DAY:
        return "per day";
      case PER_WEEK:
        return "per week";
      case PER_MONTH:
        return "per month";
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PeriodicGoalSuggestion) {
      PeriodicGoalSuggestion that = (PeriodicGoalSuggestion) obj;
      return Objects.equal(this.action, that.action) &&
          Objects.equal(this.instances, that.instances) &&
          Objects.equal(this.period, that.period);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(action, instances, period);
  }

  public enum Period {
    EMPTY, PER_DAY, PER_WEEK, PER_MONTH
  }

  private enum SpecialInstances {
    once, twice;

    public static SpecialInstances valueOf(int val) {
      switch (val) {
        case 1:
          return once;
        case 2:
          return twice;
        default:
          return null;
      }
    }
  }

  public enum Instances {
    one(1), two(2), three(3), four(4), five(5), six(6), seven(7), eight(8), nine(9), ten(10),
    eleven(11), twelve(12), thirteen(13), fourteen(14), fifteen(15), sixteen(16), seventeen(17);

    private int val;

    Instances(int val) {
      this.val = val;
    }

    public int num() {
      return val;
    }

    public static Instances valueOf(int val) {
      switch (val) {
        case 1:
          return one;
        case 2:
          return two;
        case 3:
          return three;
        case 4:
          return four;
        case 5:
          return five;
        case 6:
          return six;
        case 7:
          return seven;
        case 8:
          return eight;
        case 9:
          return nine;
        case 10:
          return ten;
        case 11:
          return eleven;
        case 12:
          return twelve;
        case 13:
          return thirteen;
        case 14:
          return fourteen;
        case 15:
          return fifteen;
        case 16:
          return sixteen;
        case 17:
          return seventeen;
        default:
          return null;
      }
    }
  }
}
