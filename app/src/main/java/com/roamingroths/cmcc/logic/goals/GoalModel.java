package com.roamingroths.cmcc.logic.goals;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 2/20/18.
 */

public class GoalModel implements Comparable<GoalModel> {

  public static Builder builder(String action, Type type) {
    return new Builder(action, type);
  }

  private static Builder builder(GoalModel goalModel) {
    return new Builder(goalModel);
  }

  private static final Joiner ON_SPACE = Joiner.on(" ");

  private transient SecretKey key;

  public final String action;
  public final String object;
  public final RelativeChange relativeChange;
  public final Type type;
  public final Instances instances;
  public final boolean numericInstances;
  public final SpecialInstances specialInstances;
  public final Period period;

  private GoalModel(Builder builder) {
    this.action = builder.action;
    this.object = builder.object;
    this.relativeChange = builder.relativeChange;
    this.type = builder.type;
    this.instances = builder.instances;
    this.numericInstances = builder.numericInstances;
    this.specialInstances = builder.specialInstances;
    this.period = builder.period;
  }

  public GoalModel withoutObject() {
    return builder(this).withoutObject().build();
  }

  public Integer getInstanceVal() {
    if (specialInstances != null) {
      return specialInstances.num();
    }
    if (instances != null) {
      return instances.num();
    }
    return null;
  }

  @Override
  public String toString() {
    List<String> parts = new ArrayList<>();
    parts.add(action);
    if (relativeChange == RelativeChange.NA) {
      if (!object.isEmpty()) {
        parts.add(object);
      }
      if (instances != null) {
        if (numericInstances) {
          parts.add(String.valueOf(instances.num()));
        } else {
          parts.add(instances.name());
        }
        if (instances.num() == 1) {
          parts.add("time");
        } else {
          parts.add("times");
        }
      }
      if (specialInstances != null) {
        parts.add(specialInstances.name());
      }
      if (period == null && (instances != null || specialInstances != null)) {
        parts.add("per ...");
      } else if (parts.size() == 1) {
        parts.add("...");
      }
      if (period != null) {
        parts.add("per");
        parts.add(period.interval);
      }
    } else {
      parts.add(relativeChange.name().toLowerCase());
      parts.add(object.isEmpty() ? "..." : object);
    }
    return ON_SPACE.join(parts);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof GoalModel) {
      GoalModel that = (GoalModel) o;
      return Objects.equal(this.action, that.action) &&
          Objects.equal(this.object, that.object) &&
          Objects.equal(this.relativeChange, that.relativeChange) &&
          Objects.equal(this.type, that.type);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(action, object, relativeChange, type);
  }

  @Override
  public int compareTo(GoalModel that) {
    int actionVal = this.action.compareTo(that.action);
    if (actionVal != 0) {
      return actionVal;
    }
    int relChangeVal = this.relativeChange.name().compareTo(that.relativeChange.name());
    if (relChangeVal != 0) {
      return relChangeVal;
    }
    int objVal = this.object.compareTo(that.object);
    if (objVal != 0) {
      return objVal;
    }
    Integer thisInstancesVal = this.getInstanceVal();
    Integer thatInstancesVal = that.getInstanceVal();
    if (thisInstancesVal != null && thatInstancesVal != null) {
      int instanceVal = thisInstancesVal.compareTo(thatInstancesVal);
      if (instanceVal != 0) {
        return instanceVal;
      }
    }
    if (this.period != null && that.period != null) {
      Integer thisPeriodRank = this.period.rank;
      Integer thatPeriodRank = that.period.rank;
      int periodVal = thisPeriodRank.compareTo(thatPeriodRank);
      if (periodVal != 0) {
        return periodVal;
      }
    }
    return this.toString().compareTo(that.toString());
  }

  public enum Type {
    EAT, DRINK, GENERAL;
  }

  public enum RelativeChange {
    MORE, LESS, NA;
  }

  public enum Period {
    PER_DAY("day", 1), PER_WEEK("week", 2), PER_MONTH("month", 3);

    public String interval;
    public int rank;

    Period(String interval, int rank) {
      this.interval = interval;
      this.rank = rank;
    }

    // Get valid periods for parts.
    public static Set<Period> matches(String lastPart) {
      Set<Period> periods = new HashSet<>();
      for (Period period : Period.values()) {
        if (period.interval.startsWith(lastPart)) {
          periods.add(period);
          continue;
        }
        if ("per".startsWith(lastPart)) {
          periods.add(period);
          continue;
        }
      }
      return periods;
    }
  }

  public enum SpecialInstances {
    once(1), twice(1);

    private int val;

    SpecialInstances(int val) {
      this.val = val;
    }

    public int num() {
      return val;
    }

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

  public static class Builder {
    final String action;
    final Type type;
    String object = "";
    RelativeChange relativeChange = RelativeChange.NA;
    Instances instances = null;
    boolean numericInstances = false;
    SpecialInstances specialInstances = null;
    Period period = null;

    private Builder(GoalModel template) {
      this(template.action, template.type);
      this.object = template.object;
      this.relativeChange = template.relativeChange;
      this.instances = template.instances;
      this.numericInstances = template.numericInstances;
      this.specialInstances = template.specialInstances;
      this.period = template.period;
    }

    private Builder(String action, Type type) {
      this.action = action;
      this.type = type;
    }

    public Builder withRelativeChange(RelativeChange relativeChange) {
      this.relativeChange = relativeChange;
      return this;
    }

    public Builder withObject(String object) {
      this.object = object;
      return this;
    }

    public Builder withoutObject() {
      this.object = "";
      return this;
    }

    public Builder withSpecialInstances(SpecialInstances specialInstances) {
      this.specialInstances = specialInstances;
      return this;
    }

    public Builder withInstances(Instances instances, boolean numericInstances) {
      this.instances = instances;
      this.numericInstances = numericInstances;
      return this;
    }

    public Builder withPeriod(Period period) {
      this.period = period;
      return this;
    }

    public Builder copyOf() {
      return new Builder(build());
    }

    public GoalModel build() {
      return new GoalModel(this);
    }
  }
}
