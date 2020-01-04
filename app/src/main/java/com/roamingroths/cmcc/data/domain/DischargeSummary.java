package com.roamingroths.cmcc.data.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by parkeroth on 4/24/17.
 */
@Parcel
public class DischargeSummary {
  public DischargeType mType;
  public final Set<MucusModifier> mModifiers = new HashSet<>();

  private static final Joiner ON_SPACE = Joiner.on(' ');

  public DischargeSummary() {}

  public DischargeSummary(DischargeType type) {
    mType = type;
  }

  public DischargeSummary(DischargeType type, Set<MucusModifier> modifiers) {
    this(type);
    mModifiers.addAll(modifiers);
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

  public boolean hasMucus() {
    return mType.hasMucus();
  }

  public boolean hasBlood() {
    return mModifiers.contains(MucusModifier.B);
  }

  private static final ImmutableSet<MucusModifier> PEAK_TYPE_MODIFIERS = ImmutableSet.of(
      MucusModifier.K, MucusModifier.CK, MucusModifier.L);
  private static final ImmutableSet<DischargeType> SPECIAL_PEAK_TYPES = ImmutableSet.of(
      DischargeType.DAMP_W_LUB, DischargeType.WET_W_LUB, DischargeType.SHINY_W_LUB, DischargeType.STRETCHY);

  public boolean isPeakType() {
    for (MucusModifier modifier : PEAK_TYPE_MODIFIERS) {
      if (mModifiers.contains(modifier)) {
        return true;
      }
    }
    return SPECIAL_PEAK_TYPES.contains(mType);
  }

  public List<String> getSummaryLinesMetric() {
    List<String> lines = new ArrayList<>();
    lines.add(mType.getMetricDescription());

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
    if (!modifierStrs.isEmpty()) {
      lines.add(ON_SPACE.join(modifierStrs));
    }
    return lines;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DischargeSummary) {
      DischargeSummary that = (DischargeSummary) o;
      if (!this.mType.equals(that.mType)) {
        return false;
      }
      return this.mModifiers.equals(that.mModifiers);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mType, mModifiers);
  }

  @Parcel
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

    MucusModifier() {} // Only for @Parcel

    MucusModifier(String description) {
      mDescription = description;
    }

    public String getDescription() {
      return mDescription;
    }
  }

  @Parcel
  public enum DischargeType {
    DRY("0", false, "Dry"),
    WET_WO_LUB("2W", false, "Wet without lubrication"),
    DAMP_WO_LUB("2", false, "Damp without lubrication"),
    SHINY_WO_LUB("4", false, "Shiny without lubrication"),
    STICKY("6", true, "Sticky", "1/4 inch", "0.5 cm"),
    TACKY("8", true, "Tacky", "1/2 - 3/4 inch", "1.0 - 2.0 cm"),
    DAMP_W_LUB("10DL", true, "Damp with lubrication"),
    WET_W_LUB("10WL", true, "Wet with lubrication"),
    SHINY_W_LUB("10SL", true, "Shiny with lubrication"),
    STRETCHY("10", true, "Stretchy", "1 inch +", "2.5 cm or more");

    private String mCode;
    private String mDescriptionMetric;
    private String mDescriptionImperial;
    private boolean mHasMucus;
    private boolean mAlwaysPeakType;

    DischargeType() {} // Only for @Parcel

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

    public boolean isLubricative() {
      return mCode.contains("L");
    }

    public boolean isStretchy() {
      return mCode.startsWith("10");
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
