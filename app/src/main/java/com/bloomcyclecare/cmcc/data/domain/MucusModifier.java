package com.bloomcyclecare.cmcc.data.domain;

import com.google.common.collect.ImmutableSet;

import org.parceler.Parcel;

@Parcel
public enum MucusModifier {
  CK("Cloudy/clear"), // NOTE: this is listed first to match before C or K
  C("Cloudy (white)"),
  K("Clear"),
  B("Brown (or black) bleeding"),
  G("Gummy (gluey)"),
  L("Lubricative"),
  P("Pasty (creamy)"),
  Y("Yellow (even pale yellow)"),
  R("Red bleeding");

  public static final ImmutableSet<MucusModifier> VALUES_INDICATING_BLEEDING = ImmutableSet.of(B, R);
  public static final ImmutableSet<MucusModifier> VALUES_REQUIRING_COLOR = ImmutableSet.of(P, G);
  public static final ImmutableSet<MucusModifier> VALUES_ALLOWING_SEPARATE_OCCURRENCES = ImmutableSet.of(B);
  public static final ImmutableSet<MucusModifier> COLOR_VALUES = ImmutableSet.of(B, R, CK, C, K, Y);

  private String mDescription;

  MucusModifier() {} // Only for @Parcel

  MucusModifier(String description) {
    mDescription = description;
  }

  public String getDescription() {
    return mDescription;
  }

  public boolean isColor() {
    return COLOR_VALUES.contains(this);
  }

  public boolean indicatesBleeding() {
    return VALUES_INDICATING_BLEEDING.contains(this);
  }
}
