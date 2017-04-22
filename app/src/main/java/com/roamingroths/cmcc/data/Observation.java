package com.roamingroths.cmcc.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by parkeroth on 4/20/17.
 */

public class Observation {

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
    StringBuilder str = new StringBuilder();
    if (flow != null) {
      str.append(flow.name()).append(" ");
    }
    if (mucusSummary != null) {
      str.append(mucusSummary.getCode().toUpperCase()).append(" ");
    }
    if (occurrences != null) {
      str.append(occurrences.name());
    }
    return str.toString();
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

    public String getImperialDesciption() {
      return getDescription(mType.getImperialDescription());
    }

    public String getMetricDesciption() {
      return getDescription(mType.getMetricDescription());
    }

    private String getDescription(String typeDescription) {
      StringBuilder description = new StringBuilder().append(typeDescription);
      switch (mType) {
        case STICKY:
        case TACKY:
        case STRETCHY:
          for (MucusModifier modifier : mModifiers) {
            description.append(modifier.getDescription()).append(" ");
          }
          break;

      }
      return description.toString();
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
    STICKY("6", true, "Sticky 1/4 inch", "Sticky 0.5 cm"),
    TACKY("8", true, "Tacky 1/2 - 3/4 inch", "Tacky 1.0 - 2.0 cm"),
    DAMP_W_LUB("10DL", true, "Damp with lubrication"),
    WET_W_LUB("10SL", true, "Wet with lubrication"),
    SHINY_W_LUB("10WL", true, "Shiny with lubrication"),
    STRETCHY("10", true, "Stretchy 1 inch +", "Stretchy 2.5 cm or more");

    private String mCode;
    private String mDescriptionMetric;
    private String mDescriptionImperial;
    private boolean mHasMucus;

    DischargeType(String code, boolean hasMucus, String description) {
      this(code, hasMucus, description, description);
    }

    DischargeType(String code, boolean hasMucus, String metricDescription, String imperialDescription) {
      mCode = code;
      mHasMucus = hasMucus;
      mDescriptionMetric = metricDescription;
      mDescriptionImperial = imperialDescription;
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
