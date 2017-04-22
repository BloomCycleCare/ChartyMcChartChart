package com.roamingroths.cmcc.data;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.data.Observation.DischargeSummary;
import com.roamingroths.cmcc.data.Observation.DischargeType;
import com.roamingroths.cmcc.data.Observation.Flow;
import com.roamingroths.cmcc.data.Observation.MucusModifier;
import com.roamingroths.cmcc.data.Observation.Occurrences;
import com.roamingroths.cmcc.utils.StringUtil;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by parkeroth on 4/20/17.
 */

public class ObservationBuilder {
  Flow flow;
  DischargeSummary mucusSummary;
  Occurrences occurrences;

  private static final Joiner ON_SPACE = Joiner.on(", ");
  static final String VALID_OCCURRENCES_STR = ON_SPACE.join(Occurrences.values());
  static final Set<DischargeType> TYPES_ALLOWING_MODIFIERS =
      ImmutableSet.of(DischargeType.STICKY, DischargeType.STRETCHY, DischargeType.TACKY);

  private ObservationBuilder(Flow flow, DischargeSummary mucusSummary, Occurrences occurrences) {
    this.flow = flow;
    this.mucusSummary = mucusSummary;
    this.occurrences = occurrences;
  }

  public Observation build() {
    return new Observation(this);
  }

  public static ObservationBuilder fromString(String observation)
      throws InvalidObservationException {
    String sanitizedObservation = observation.toUpperCase();

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
      return new ObservationBuilder(flow, null, null);
    }

    DischargeType dischargeType = null;
    for (DischargeType t : DischargeType.values()) {
      if (observationWithoutFlow.startsWith(t.getCode())) {
        dischargeType = t;
        break;
      }
    }
    if (dischargeType == null) {
      throw new InvalidObservationException("Could not determine DischargeType for: " + observation);
    }
    if (flow != null && !dischargeType.hasMucus()) {
      throw new InvalidObservationException(
          "Only discharge types with mucus are valid for L and VL flows.");
    }
    String observationWithoutDischargeType =
        StringUtil.consumePrefix(observationWithoutFlow, dischargeType.getCode());

    Set<MucusModifier> mucusModifiers = new LinkedHashSet<>();
    String observationWithoutModifiers =
        consumeMucusModifiers(observationWithoutDischargeType, mucusModifiers);
    if (!mucusModifiers.isEmpty() && !TYPES_ALLOWING_MODIFIERS.contains(dischargeType)) {
      throw new InvalidObservationException(dischargeType.getCode() + " does not allow modifiers");
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
    return new ObservationBuilder(flow, mucusSummary, occurrences);
  }

  private static String consumeMucusModifiers(String str, Set<MucusModifier> modifiers) {
    for (MucusModifier m : MucusModifier.values()) {
      if (str.startsWith(m.name())) {
        modifiers.add(m);
        return consumeMucusModifiers(str.substring(m.name().length()), modifiers);
      }
    }
    return str;
  }

  public static class InvalidObservationException extends Exception {
    InvalidObservationException(String reason) {
      super(reason);
    }
  }
}
