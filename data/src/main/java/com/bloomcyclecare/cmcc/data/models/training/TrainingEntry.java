package com.bloomcyclecare.cmcc.data.models.training;

import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.google.common.base.Strings;

import org.joda.time.LocalDate;

import java.util.Optional;
import java.util.function.Function;

public class TrainingEntry {

  private final String observationText;
  private final String marker;
  private boolean peakDay = false;
  private boolean intercourse = false;
  private boolean pointOfChange = false;
  private boolean unusualBleeding = false;
  private boolean isEssentiallyTheSame = false;

  private TrainingEntry(String observationText, String marker) {
    this.observationText = observationText;
    this.marker = marker;
  }

  public static TrainingEntry forText(String observationText) {
    return forText(observationText, "");
  }

  public static TrainingEntry forText(String observationText, String marker) {
    return new TrainingEntry(observationText, marker);
  }

  public Optional<String> marker() {
    return Strings.isNullOrEmpty(marker) ? Optional.empty() : Optional.of(marker);
  }

  public TrainingEntry peakDay() {
    peakDay = true;
    return this;
  }

  public TrainingEntry intercourse() {
    intercourse = true;
    return this;
  }

  public TrainingEntry pointOfChange() {
    pointOfChange = true;
    return this;
  }

  public TrainingEntry unusualBleeding() {
    unusualBleeding = true;
    return this;
  }

  public TrainingEntry essentiallyTheSame() {
    isEssentiallyTheSame = true;
    return this;
  }

  public ObservationEntry asChartEntry(LocalDate date, Function<String, Optional<Observation>> observationParser) {
    Observation observation = observationParser.apply(observationText).orElse(null);
    return new ObservationEntry(date, observation, peakDay, intercourse, false, false, pointOfChange, unusualBleeding, null, isEssentiallyTheSame, "");
  }
}
