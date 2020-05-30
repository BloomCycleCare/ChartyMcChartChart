package com.bloomcyclecare.cmcc.data.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by parkeroth on 4/20/17.
 */
@Parcel
public class Observation {

  private static final Joiner ON_SPACE = Joiner.on(' ');
  private static final Joiner ON_NEW_LINE = Joiner.on('\n');

  public Flow flow;
  public DischargeSummary dischargeSummary;
  public Occurrences occurrences;
  public Map<MucusModifier, Occurrences> additionalOccurrences = new LinkedHashMap<>();

  public Observation() {}  // Only for @Parcel

  public Observation(Flow flow, DischargeSummary dischargeSummary, Occurrences occurrences, Map<MucusModifier, Occurrences> additionalOccurrences) {
    this.flow = flow;
    this.dischargeSummary = dischargeSummary;
    this.occurrences = occurrences;
    this.additionalOccurrences.putAll(additionalOccurrences);
  }

  public boolean hasBlood() {
    return flow != null
        || (dischargeSummary != null && dischargeSummary.hasBlood())
        || additionalOccurrences.containsKey(MucusModifier.B);
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
      strs.add(dischargeSummary.getCode().or("").toUpperCase());
    }
    if (occurrences != null) {
      strs.add(occurrences.name());
    }
    for (Map.Entry<MucusModifier, Occurrences> e : additionalOccurrences.entrySet()) {
      strs.add(e.getKey().name() + e.getValue().name());
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
      return Objects.equal(this.dischargeSummary, that.dischargeSummary)
          && Objects.equal(this.flow, that.flow)
          && Objects.equal(this.occurrences, that.occurrences)
          && Iterables.elementsEqual(this.additionalOccurrences.entrySet(), that.additionalOccurrences.entrySet());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(flow, dischargeSummary, occurrences, additionalOccurrences);
  }
}
