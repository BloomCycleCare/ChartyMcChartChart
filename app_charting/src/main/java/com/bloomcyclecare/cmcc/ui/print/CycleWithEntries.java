package com.bloomcyclecare.cmcc.ui.print;

import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;

import java.util.List;

public class CycleWithEntries {
  public final Cycle cycle;
  public final List<ChartEntry> entries;

  public CycleWithEntries(Cycle cycle, List<ChartEntry> entries) {
    this.cycle = cycle;
    this.entries = entries;
  }
}
