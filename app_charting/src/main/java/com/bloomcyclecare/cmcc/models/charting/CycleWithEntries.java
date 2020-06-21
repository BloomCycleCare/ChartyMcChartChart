package com.bloomcyclecare.cmcc.models.charting;

import com.bloomcyclecare.cmcc.data.entities.Cycle;

import java.util.List;

public class CycleWithEntries {
  public final Cycle cycle;
  public final List<ChartEntry> entries;

  public CycleWithEntries(Cycle cycle, List<ChartEntry> entries) {
    this.cycle = cycle;
    this.entries = entries;
  }
}
