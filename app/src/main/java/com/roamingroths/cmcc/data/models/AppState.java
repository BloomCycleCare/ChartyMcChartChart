package com.roamingroths.cmcc.data.models;

import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.logic.profile.Profile;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class holding application state for writing to JSON in plain text.
 */

public class AppState {
  public final List<CycleData> cycles;
  public final Profile profile;

  public AppState(List<CycleData> cycles, Profile profile) {
    this.cycles = cycles;
    this.profile = profile;
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
