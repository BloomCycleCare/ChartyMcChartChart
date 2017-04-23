package com.roamingroths.cmcc.data;

import com.google.common.base.Preconditions;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ChartEntry {
  public final Observation observation;
  public final boolean peakDay;
  public final boolean intercourse;

  public ChartEntry(Observation observation, boolean peakDay, boolean intercourse) {
    this.observation = Preconditions.checkNotNull(observation);
    this.peakDay = peakDay;
    this.intercourse = intercourse;
  }
}
