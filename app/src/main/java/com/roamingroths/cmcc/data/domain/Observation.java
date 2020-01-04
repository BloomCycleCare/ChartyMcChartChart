package com.roamingroths.cmcc.data.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.utils.StringUtil;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;

/**
 * Created by parkeroth on 4/20/17.
 */
@Parcel
public class Observation {

  private static final Joiner ON_SPACE = Joiner.on(' ');
  private static final Joiner ON_NEW_LINE = Joiner.on('\n');
  static final String VALID_OCCURRENCES_STR = ON_SPACE.join(Occurrences.values());
  static final Set<DischargeSummary.DischargeType> TYPES_ALLOWING_MODIFIERS =
      ImmutableSet.of(DischargeSummary.DischargeType.STICKY, DischargeSummary.DischargeType.STRETCHY, DischargeSummary.DischargeType.TACKY);

  public Flow flow;
  public DischargeSummary dischargeSummary;
  public Occurrences occurrences;

  public Observation() {}  // Only for @Parcel

  public Observation(Flow flow, DischargeSummary dischargeSummary, Occurrences occurrences) {
    this.flow = flow;
    this.dischargeSummary = dischargeSummary;
    this.occurrences = occurrences;
  }

  public boolean hasBlood() {
    return flow != null || (dischargeSummary != null && dischargeSummary.hasBlood());
  }

  public boolean hasMucus() {
    return dischargeSummary != null && dischargeSummary.mType.hasMucus();
  }
  @Override
  public String toString() {
    List<String> strs = new ArrayList<>();
    if (flow != null) {
      strs.add(flow.name());
    }
    if (dischargeSummary != null) {
      strs.add(dischargeSummary.getCode().toUpperCase());
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

  public List<String> getSummaryLines() {
    List<String> strs = new ArrayList<>();
    if (flow != null) {
      strs.add(flow.getDescription());
    }
    if (dischargeSummary != null) {
      // TODO: Get unit system preference
      strs.addAll(dischargeSummary.getSummaryLinesMetric());
    }
    if (occurrences != null) {
      strs.add(occurrences.getDescription());
    }
    return strs;
  }

  private String getDescription(Joiner joiner) {
    return joiner.join(getSummaryLines());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Observation) {
      Observation that = (Observation) o;
      if (this.flow != that.flow) {
        return false;
      }
      if (this.occurrences != that.occurrences) {
        return false;
      }
      return Objects.equal(this.dischargeSummary, that.dischargeSummary);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(flow, dischargeSummary, occurrences);
  }

  public static class InvalidObservationException extends Exception {
    InvalidObservationException(String reason) {
      super(reason);
    }
  }

  @Nullable
  public static Observation fromString(String observation) throws InvalidObservationException {
    if (Strings.isNullOrEmpty(observation)) {
      return null;
    }

    String sanitizedObservation = observation.toUpperCase().replace(" ", "");

    Flow flow = null;
    for (Flow f : Flow.values()) {
      if (sanitizedObservation.startsWith(f.name())) {
        flow = f;
        break;
      }
    }
    String observationWithoutFlow = (flow == null)
        ? sanitizedObservation : StringUtil.consumePrefix(sanitizedObservation, flow.name());
    if (flow == Flow.H || flow == Flow.M) {
      if (!observationWithoutFlow.isEmpty()) {
        throw new InvalidObservationException("H or M do not require extra info.");
      }
      return new Observation(flow, null, null);
    }

    DischargeSummary.DischargeType dischargeType = null;
    for (DischargeSummary.DischargeType t : DischargeSummary.DischargeType.values()) {
      if (observationWithoutFlow.startsWith(t.getCode())) {
        dischargeType = t;
        break;
      }
    }
    if (dischargeType == null) {
      throw new InvalidObservationException("Could not determine DischargeType for: " + observation);
    }
    String observationWithoutDischargeType =
        StringUtil.consumePrefix(observationWithoutFlow, dischargeType.getCode());

    Set<DischargeSummary.MucusModifier> mucusModifiers = new LinkedHashSet<>();
    String observationWithoutModifiers = StringUtil.consumeEnum(
        observationWithoutDischargeType, mucusModifiers, DischargeSummary.MucusModifier.class);
    if (!mucusModifiers.isEmpty() && !TYPES_ALLOWING_MODIFIERS.contains(dischargeType)) {
      throw new InvalidObservationException(dischargeType.getCode() + " does not allow modifiers");
    }
    // Add "implicit" modifiers
    if (dischargeType.isLubricative()) {
      mucusModifiers.add(DischargeSummary.MucusModifier.L);
    }
    DischargeSummary mucusSummary = new DischargeSummary(dischargeType, mucusModifiers);

    Occurrences occurrences = null;
    for (Occurrences o : Occurrences.values()) {
      if (observationWithoutModifiers.startsWith(o.name())) {
        occurrences = o;
        break;
      }
    }
    if (occurrences == null) {
      if (observationWithoutModifiers.isEmpty()) {
        throw new InvalidObservationException("Missing one of: " + VALID_OCCURRENCES_STR);
      } else {
        throw new InvalidObservationException(
            String.format("Occurrence %s is not one of: %s",
                observationWithoutModifiers, VALID_OCCURRENCES_STR));
      }
    }
    String shouldBeEmptyString =
        StringUtil.consumePrefix(observationWithoutModifiers, occurrences.name());
    if (!shouldBeEmptyString.isEmpty()) {
      throw new InvalidObservationException("Extra info after occurrences.");
    }
    return new Observation(flow, mucusSummary, occurrences);
  }
}
