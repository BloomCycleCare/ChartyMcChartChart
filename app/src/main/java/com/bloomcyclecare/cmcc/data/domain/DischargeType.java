package com.bloomcyclecare.cmcc.data.domain;

import org.parceler.Parcel;

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
