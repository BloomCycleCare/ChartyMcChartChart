package com.roamingroths.cmcc.data;

import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;

import java.util.List;

/**
 * Class holding application state for writing to JSON in plain text.
 */

public class AppState {
  public final List<CycleData> cycles;

  public AppState(List<CycleData> cycles) {
    this.cycles = cycles;
  }

  public static class CycleData {
    public final Cycle cycle;
    public final List<ChartEntry> entries;

    public CycleData(Cycle cycle, List<ChartEntry> entries) {
      this.cycle = cycle;
      this.entries = entries;
    }
  }
}
