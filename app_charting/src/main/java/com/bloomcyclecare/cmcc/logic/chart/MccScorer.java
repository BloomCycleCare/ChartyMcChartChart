package com.bloomcyclecare.cmcc.logic.chart;

import android.util.Log;

import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.observation.DischargeType;
import com.bloomcyclecare.cmcc.data.models.observation.MucusModifier;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

public class MccScorer {

  private static final int EVALUATION_INTERVAL_DAYS = 6;

  public static Optional<Float> getScore(SortedSet<ChartEntry> unfilteredEntries, Optional<LocalDate> peakDay) {
    if (!peakDay.isPresent()) {
      return Optional.empty();
    }
    LocalDate firstDate = peakDay.get().minusDays(EVALUATION_INTERVAL_DAYS - 1);
    List<ChartEntry> entries = new ArrayList<>();
    for (ChartEntry entry : unfilteredEntries) {
      if (!entry.observationEntry.hasMucus()) {
        continue;
      }
      if (entry.entryDate.isBefore(firstDate)) {
        continue;
      }
      if (entry.entryDate.isAfter(peakDay.get())) {
        continue;
      }
      entries.add(entry);
    }
    int totalPoints = 0;
    for (ChartEntry entry : entries) {
      totalPoints += getScore(entry);
    }
    return Optional.of((float) totalPoints / EVALUATION_INTERVAL_DAYS);
  }

  private static int getScore(ChartEntry entry) {
    Observation observation = entry.observationEntry.observation;
    if (observation == null) {
      Log.e(MccScorer.class.getSimpleName(), "Missing Observation for " + DateUtil.toWireStr(entry.entryDate));
      return 0;
    }
    Set<MucusModifier> modifiers = observation.dischargeSummary.mModifiers;
    DischargeType type = observation.dischargeSummary.mType;

    int points = 0;

    // Check color
    if (modifiers.contains(MucusModifier.B)) {
      points += 0;
    }
    if (modifiers.contains(MucusModifier.C) || modifiers.contains(MucusModifier.Y)) {
      points += 2;
    }
    if (modifiers.contains(MucusModifier.K) || modifiers.contains(MucusModifier.CK)) {
      points += 4;
    }

    // Check consistency
    if (type == DischargeType.STICKY) {
      if (modifiers.contains(MucusModifier.C)) {
        points -= 2; // only score 2 points for a PC or 6PC observation
      }
      points += 2;
    } else if (type == DischargeType.TACKY) {
      points += 2;
    } else if (type == DischargeType.STRETCHY) {
      points += 4;
    }

    // Check sensation
    if (modifiers.contains(MucusModifier.L)) {
      points += 4;
    }

    // Check "transition"
    if (observation.hasMucus()) {
      points += 2;
      if (modifiers.contains(MucusModifier.L)) {
        points += 2;
      }
    }
    return points;
  }
}
