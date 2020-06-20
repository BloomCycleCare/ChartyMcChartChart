package com.bloomcyclecare.cmcc.data.models;

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
