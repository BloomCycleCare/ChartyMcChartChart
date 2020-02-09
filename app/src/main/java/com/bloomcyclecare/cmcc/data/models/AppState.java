package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.logic.profile.Profile;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class holding application state for writing to JSON in plain text.
 */

public class AppState {
  public final List<CycleData> cycles;
  public final Profile profile;
  public final List<Instructions> instructions;

  public AppState(List<CycleData> cycles, Profile profile, List<Instructions> instructions) {
    this.cycles = cycles;
    this.profile = profile;
    this.instructions = instructions;
  }

  public static class CycleData {
    public final Cycle cycle;
    public final Set<ChartEntry> entries;

    public CycleData(Cycle cycle, Collection<ChartEntry> entries) {
      this.cycle = cycle;
      this.entries = ImmutableSet.copyOf(entries);
    }
  }
}
