package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.logic.profile.Profile;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.joda.time.LocalDate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class holding application state for writing to JSON in plain text.
 */

public class AppState {
  public final List<CycleData> cycles;
  public final Profile profile;
  public final List<Instructions> instructions;

  private enum Property {
    NUM_ENTRIES,
    LAST_ENTRY,
    NUM_CYCLES,
    NUM_INSTRUCTIONS,
    LAST_INSTRUCTION_START,
  }

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

  public Map<String, String> properties() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    int numEntries = 0;
    LocalDate lastEntry = null;
    for (CycleData cd : cycles) {
      for (ChartEntry entry : cd.entries) {
        numEntries++;
        if (entry.observationEntry.hasObservation() && (
            lastEntry == null || entry.entryDate.isAfter(lastEntry))) {
          lastEntry = entry.entryDate;
        }
      }
    }
    LocalDate lastInstructionStart = null;
    for (Instructions i : instructions) {
      if (lastInstructionStart == null || i.startDate.isAfter(lastInstructionStart)) {
        lastInstructionStart = i.startDate;
      }
    }
    builder.put(Property.NUM_CYCLES.name(), String.valueOf(cycles.size()));
    builder.put(Property.NUM_ENTRIES.name(), String.valueOf(numEntries));
    builder.put(Property.LAST_ENTRY.name(), DateUtil.toWireStr(lastEntry));
    builder.put(Property.NUM_INSTRUCTIONS.name(), String.valueOf(instructions.size()));
    builder.put(Property.LAST_INSTRUCTION_START.name(), DateUtil.toWireStr(lastInstructionStart));
    return builder.build();
  }
}
