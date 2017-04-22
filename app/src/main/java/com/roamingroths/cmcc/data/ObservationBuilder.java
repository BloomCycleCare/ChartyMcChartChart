package com.roamingroths.cmcc.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.data.Observation.Flow;
import com.roamingroths.cmcc.data.Observation.MucusModifier;
import com.roamingroths.cmcc.data.Observation.MucusSummary;
import com.roamingroths.cmcc.data.Observation.MucusType;
import com.roamingroths.cmcc.data.Observation.Occurrences;
import com.roamingroths.cmcc.utils.StringUtil;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by parkeroth on 4/20/17.
 */

public class ObservationBuilder {
  public Flow flow;
  public MucusSummary mucusSummary;
  public Occurrences occurrences;
  public String note = "";

  static final Set<MucusType> TYPES_ALLOWING_MODIFIERS =
      ImmutableSet.of(MucusType.STICKY, MucusType.STRETCHY, MucusType.TACKY);

  private ObservationBuilder(Flow flow, MucusSummary mucusSummary, Occurrences occurrences) {
    this.flow = flow;
    this.mucusSummary = mucusSummary;
    this.occurrences = occurrences;
  }

  public ObservationBuilder withNote(String note) {
    Preconditions.checkNotNull(note);
    note = note;
    return this;
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
        throw new InvalidObservationException("Extra info after flow.");
      }
      return new ObservationBuilder(flow, null, null);
    }

    MucusType mucusType = null;
    for (MucusType t : MucusType.values()) {
      if (observationWithoutFlow.startsWith(t.getCode())) {
        mucusType = t;
        break;
      }
    }
    if (mucusType == null) {
      throw new InvalidObservationException("Could not determine MucusType for: " + observation);
    }
    String observationWithoutMucusType =
        StringUtil.consumePrefix(observationWithoutFlow, mucusType.getCode());

    Set<MucusModifier> mucusModifiers = new LinkedHashSet<>();
    String observationWithoutModifiers =
        consumeMucusModifiers(observationWithoutMucusType, mucusModifiers);
    if (!mucusModifiers.isEmpty() && !TYPES_ALLOWING_MODIFIERS.contains(mucusType)) {
      throw new InvalidObservationException(mucusType.name() + " does not allow modifiers");
    }
    MucusSummary mucusSummary = new MucusSummary(mucusType, mucusModifiers);

    Occurrences occurrences = null;
    for (Occurrences o : Occurrences.values()) {
      if (observationWithoutModifiers.startsWith(o.name())) {
        occurrences = o;
        break;
      }
    }
    if (occurrences == null) {
      throw new InvalidObservationException(
          "Could not determine number of occurrences for: " + observation);
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
    public InvalidObservationException(String reason) {
      super(reason);
    }
  }
}
