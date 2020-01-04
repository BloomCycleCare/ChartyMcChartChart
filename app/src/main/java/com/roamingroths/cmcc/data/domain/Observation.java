package com.roamingroths.cmcc.data.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

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
      strs.add(dischargeSummary.getCode().or("").toUpperCase());
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
}
