package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.observation.DischargeSummary;
import com.bloomcyclecare.cmcc.data.models.observation.DischargeType;
import com.bloomcyclecare.cmcc.data.models.observation.Flow;
import com.bloomcyclecare.cmcc.data.models.observation.MucusModifier;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.observation.Occurrences;
import com.bloomcyclecare.cmcc.utils.StringUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import androidx.annotation.NonNull;

public class ObservationParser {

  private static final Joiner ON_SPACE = Joiner.on(' ');
  private static final String VALID_OCCURRENCES_STR = ON_SPACE.join(Occurrences.values());
  private static final Set<DischargeType> TYPES_REQUIRING_MODIFIERS =
      ImmutableSet.of(DischargeType.STICKY, DischargeType.STRETCHY, DischargeType.TACKY);
  private static final Set<MucusModifier> MODIFIERS_NOT_REQUIRING_DISCHARGE_TYPE =
      ImmutableSet.of(MucusModifier.P);

  @NonNull
  public static Optional<Observation> parse(String input) throws InvalidObservationException {
    try {
      if (Strings.isNullOrEmpty(input)) {
        return Optional.empty();
      }

      String sanitizedObservation = input.toUpperCase().replace(" ", "");

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
        DischargeSummary dischargeSummary = null;
        if (!observationWithoutFlow.isEmpty()) {
          try {
            MucusModifier modifier = MucusModifier.valueOf(observationWithoutFlow);
            if (!modifier.equals(MucusModifier.B)) {
              throw new IllegalArgumentException();
            }
            dischargeSummary = new DischargeSummary(ImmutableSet.of(modifier));
          } catch (Exception e) {
            throw new InvalidObservationException("Only B can follow H or M", e);
          }
        }
        return Optional.of(new Observation(flow, dischargeSummary, null, ImmutableMap.of()));
      }

      DischargeType dischargeType = null;
      for (DischargeType t : DischargeType.values()) {
        if (observationWithoutFlow.startsWith(t.getCode())) {
          dischargeType = t;
          break;
        }
      }
      String observationWithoutDischargeType = observationWithoutFlow;
      if (dischargeType != null) {
        observationWithoutDischargeType =
            StringUtil.consumePrefix(observationWithoutFlow, dischargeType.getCode());
      }
      Set<MucusModifier> mucusModifiers = new LinkedHashSet<>();
      String observationWithoutModifiers = StringUtil.consumeEnum(
          observationWithoutDischargeType, mucusModifiers, MucusModifier.class);

      Set<MucusModifier> modifiersRequiringColor = new HashSet<>(mucusModifiers);
      modifiersRequiringColor.retainAll(MucusModifier.VALUES_REQUIRING_COLOR);
      boolean hasColorModifier = !Collections.disjoint(mucusModifiers, MucusModifier.COLOR_VALUES);
      for (MucusModifier m : modifiersRequiringColor) {
        if (!hasColorModifier) {
          throw new InvalidObservationException(m.name() + " must be followed by a color: " + input);
        }
      }

      if (dischargeType == null && !mucusModifiers.contains(MucusModifier.P)) {
        throw new InvalidObservationException("Could not determine DischargeType for: " + input);
      }
      if (dischargeType != null) {
        if (!mucusModifiers.isEmpty() && !TYPES_REQUIRING_MODIFIERS.contains(dischargeType)) {
          throw new InvalidObservationException(dischargeType.getCode() + " does not allow modifiers");
        }
        if (mucusModifiers.isEmpty() && TYPES_REQUIRING_MODIFIERS.contains(dischargeType)) {
          throw new InvalidObservationException(dischargeType.getCode() + " needs a modifier");
        }
        // Add "implicit" modifiers
        if (dischargeType.isLubricative()) {
          mucusModifiers.add(MucusModifier.L);
        }
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
      String remainder = StringUtil.consumePrefix(observationWithoutModifiers, occurrences.name());
      Map<MucusModifier, Occurrences> additionalOccurrences = new LinkedHashMap<>();
      if (!remainder.isEmpty()) {
        Set<MucusModifier> modifiersWithOccurrences = new LinkedHashSet<>();
        remainder = StringUtil.consumeEnum(
            remainder, modifiersWithOccurrences, MucusModifier.class,
            MucusModifier.VALUES_ALLOWING_SEPARATE_OCCURRENCES::contains);
        if (modifiersWithOccurrences.isEmpty()) {
          throw new InvalidObservationException(
              "Occurrance should only be followed with additional modifiers");
        }
        Set<MucusModifier> repeatedModifiers = Sets.intersection(modifiersWithOccurrences, mucusModifiers);
        if (!repeatedModifiers.isEmpty()) {
          throw new InvalidObservationException(
              "Cannot repeat modifier " + repeatedModifiers.iterator().next().name());
        }
        Occurrences additionalOccurrence = null;
        for (Occurrences o : Occurrences.values()) {
          if (remainder.startsWith(o.name())) {
            additionalOccurrence = o;
            break;
          }
        }
        if (additionalOccurrence == null) {
          throw new InvalidObservationException(
              "Additonal modifiers should be followed by an occurrence");
        }
        for (MucusModifier m : modifiersWithOccurrences) {
          additionalOccurrences.put(m, additionalOccurrence);
        }
        remainder = StringUtil.consumePrefix(remainder, additionalOccurrence.name());
        if (!Strings.isNullOrEmpty(remainder)) {
          throw new InvalidObservationException(
              "No additional info should follow extra occurrences");
        }
      }
      if (!remainder.isEmpty()) {
        throw new InvalidObservationException("Extra info after occurrences.");
      }
      return Optional.of(new Observation(flow, mucusSummary, occurrences, additionalOccurrences));
    } catch (InvalidObservationException ioe) {
      throw new InvalidObservationException(ioe.getMessage() + " for input " + input);
    }
  }

  public static class InvalidObservationException extends Exception {
    InvalidObservationException(String reason) {
      super(reason);
    }
    InvalidObservationException(String m, Throwable t) {
      super(m, t);
    }
  }
}
