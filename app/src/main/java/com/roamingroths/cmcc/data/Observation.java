package com.roamingroths.cmcc.data;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by parkeroth on 4/20/17.
 */

public class Observation {

  private static final Joiner ON_SPACE = Joiner.on(' ');
  private static final Joiner ON_NEW_LINE = Joiner.on('\n');

  public final Flow flow;
  public final DischargeSummary mucusSummary;
  public final Occurrences occurrences;

  public Observation(ObservationBuilder builder) {
    flow = builder.flow;
    mucusSummary = builder.mucusSummary;
    occurrences = builder.occurrences;
  }

  @Override
  public String toString() {
    List<String> strs = new ArrayList<>();
    if (flow != null) {
      strs.add(flow.name());
    }
    if (mucusSummary != null) {
      strs.add(mucusSummary.getCode().toUpperCase());
    }
    if (occurrences != null) {
      strs.add(occurrences.name());
    }
    return ON_SPACE.join(strs);
  }

  public String getDescription() {
    return getDescription(ON_SPACE);
  }

  public String getMultiLineDescription() {
    return getDescription(ON_NEW_LINE);
  }

  private String getDescription(Joiner joiner) {
    List<String> strs = new ArrayList<>();
    if (flow != null) {
      strs.add(flow.getDescription());
    }
    if (mucusSummary != null) {
      // TODO: Get unit system preference
      strs.add(mucusSummary.getMetricDesciption(joiner));
    }
    if (occurrences != null) {
      strs.add(occurrences.getDescription());
    }
    return joiner.join(strs);
  }

  public enum Occurrences {
    X1("Seen only once"),
    X2("Seen twice"),
    X3("Seen three times"),
    AD("Seen all day");

    private final String description;

    Occurrences(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  public enum Flow {
    H("Heavy flow"),
    M("Medium flow"),
    L("Light flow"),
    VL("Very light flow");

    private final String description;

    Flow(String description) {
      this.description = description;
    }

    public String getDescription() {
      return this.description;
    }
  }

  public static class DischargeSummary {
    public final DischargeType mType;
    public final Set<MucusModifier> mModifiers;

    public DischargeSummary(DischargeType type) {
      this(type, new HashSet<MucusModifier>());
    }

    public DischargeSummary(DischargeType type, Set<MucusModifier> modifiers) {
      mType = type;
      mModifiers = modifiers;
    }

    public String getCode() {
      StringBuilder code = new StringBuilder().append(mType.getCode());
      switch (mType) {
        case STICKY:
        case TACKY:
        case STRETCHY:
          for (MucusModifier modifier : mModifiers) {
            code.append(modifier.name());
          }
          break;

      }
      return code.toString();
    }

    public String getImperialDesciption(Joiner joiner) {
      return getDescription(joiner, mType.getImperialDescription());
    }

    public String getMetricDesciption(Joiner joiner) {
      return getDescription(joiner, mType.getMetricDescription());
    }

    private String getDescription(Joiner joiner, String typeDescription) {
      StringBuilder description = new StringBuilder().append(typeDescription);

      List<String> modifierStrs = new ArrayList<>();
      switch (mType) {
        case STICKY:
        case TACKY:
        case STRETCHY:
          for (MucusModifier modifier : mModifiers) {
            modifierStrs.add(modifier.getDescription());
          }
          break;
      }
      return joiner.join(typeDescription, ON_SPACE.join(modifierStrs));
    }
  }

  public enum MucusModifier {
    CK("Cloudy/clear"), // NOTE: this is listed first to match before C or K
    C("Cloudy (white)"),
    K("Clear"),
    B("Brown (or black) bleeding"),
    G("Gummy (gluey)"),
    L("Lubricative"),
    P("Pasty (creamy)"),
    Y("Yellow (even pale yellow)");

    private String mDescription;

    MucusModifier(String description) {
      mDescription = description;
    }

    public String getDescription() {
      return mDescription;
    }
  }

  public enum DischargeType { // NOTE: enum values ordered to support inorder prefix matching
    DRY("0", false, "Dry"),
    WET_WO_LUB("2W", false, "Wet without lubrication"),
    DAMP_WO_LUB("2", false, "Damp without lubrication"),
    SHINY_WO_LUB("4", false, "Shiny without lubrication"),
    STICKY("6", true, "Sticky", "1/4 inch", "0.5 cm"),
    TACKY("8", true, "Tacky", "1/2 - 3/4 inch", "1.0 - 2.0 cm"),
    DAMP_W_LUB("10DL", true, "Damp with lubrication"),
    WET_W_LUB("10SL", true, "Wet with lubrication"),
    SHINY_W_LUB("10WL", true, "Shiny with lubrication"),
    STRETCHY("10", true, "Stretchy", "1 inch +", "2.5 cm or more");

    private String mCode;
    private String mDescriptionMetric;
    private String mDescriptionImperial;
    private boolean mHasMucus;

    DischargeType(String code, boolean hasMucus, String description) {
      this(code, hasMucus, description, "", "");
    }

    DischargeType(String code, boolean hasMucus, String description, String metricExtraDescription, String imperialExtraDescription) {
      mCode = code;
      mHasMucus = hasMucus;
      if (metricExtraDescription.isEmpty()) {
        mDescriptionMetric = description;
      } else {
        mDescriptionMetric = String.format("%s (%s)", description, metricExtraDescription);
      }
      if (imperialExtraDescription.isEmpty()) {
        mDescriptionImperial = description;
      } else {
        mDescriptionImperial = String.format("%s (%s)", description, imperialExtraDescription);
      }
    }

    public boolean hasMucus() {
      return mHasMucus;
    }

    public String getCode() {
      return mCode;
    }

    public String getImperialDescription() {
      return mDescriptionImperial;
    }

    public String getMetricDescription() {
      return mDescriptionMetric;
    }
  }
}
