package com.roamingroths.cmcc.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by parkeroth on 4/20/17.
 */

public class Observation {

  public final Flow flow;
  public final MucusSummary mucusSummary;
  public final Occurrences occurrences;
  public final String note;

  public Observation(ObservationBuilder builder) {
    flow = builder.flow;
    mucusSummary = builder.mucusSummary;
    occurrences = builder.occurrences;
    note = builder.note;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    if (flow != null) {
      str.append(flow.name());
    }
    if (mucusSummary != null) {
      str.append(mucusSummary.getCode());
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

  public static class MucusSummary {
    public final MucusType mType;
    public final Set<MucusModifier> mModifiers;

    public MucusSummary(MucusType type) {
      this(type, new HashSet<MucusModifier>());
    }

    public MucusSummary(MucusType type, Set<MucusModifier> modifiers) {
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

  public enum MucusType {
    DRY("0", "Dry"),
    DAMP_WO_LUB("2", "Damp without lubrication"),
    WET_WO_LUB("2W", "Wet without lubrication"),
    SHINY_WO_LUB("4", "Shiny without lubrication"),
    STICKY("6", "Sticky 1/4 inch", "Sticky 0.5 cm"),
    TACKY("8", "Tacky 1/2 - 3/4 inch", "Tacky 1.0 - 2.0 cm"),
    STRETCHY("10", "Stretchy 1 inch +", "Stretchy 2.5 cm or more"),
    DAMP_W_LUB("10DL", "Damp with lubrication"),
    WET_W_LUB("10SL", "Wet with lubrication"),
    SHINY_W_LUB("10WL", "Shiny with lubrication");

    private String mCode;
    private String mDescriptionMetric;
    private String mDescriptionImperial;

    MucusType(String code, String description) {
      this(code, description, description);
    }

    MucusType(String code, String metricDescription, String imperialDescription) {
      mCode = code;
      mDescriptionMetric = metricDescription;
      mDescriptionImperial = imperialDescription;
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
