package com.bloomcyclecare.cmcc.data.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by parkeroth on 4/24/17.
 */
@Parcel
public class DischargeSummary {
  @Nullable
  public DischargeType mType;
  public Set<MucusModifier> mModifiers = new LinkedHashSet<>(); // preserve ordering

  private static final Joiner ON_SPACE = Joiner.on(' ');

  public DischargeSummary() {}

  public DischargeSummary(@NonNull DischargeType type) {
    this(type, ImmutableSet.of());
  }

  public DischargeSummary(@NonNull Collection<MucusModifier> modifiers) {
    this(null, ImmutableSet.copyOf(modifiers));
  }

  public DischargeSummary(@Nullable DischargeType type, Set<MucusModifier> modifiers) {
    mType = type;
    mModifiers.addAll(modifiers);
  }

  public Optional<String> getCode() {
    StringBuilder code = new StringBuilder();
    if (mType != null) {
      code.append(mType.getCode());
    }
    for (MucusModifier modifier : mModifiers) {
      code.append(modifier.name());
    }
    return code.length() > 0 ? Optional.of(code.toString()) : Optional.absent();
  }

  public boolean hasMucus() {
    return mType.hasMucus();
  }

  public boolean hasBlood() {
    return !Collections.disjoint(mModifiers, MucusModifier.VALUES_INDICATING_BLEEDING);
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

}
