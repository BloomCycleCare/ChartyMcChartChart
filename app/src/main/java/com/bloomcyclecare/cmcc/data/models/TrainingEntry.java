package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.data.domain.Observation;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;

import org.joda.time.LocalDate;

public class TrainingEntry {

  private final String observationText;
  private boolean peakDay = false;
  private boolean intercourse = false;
  private boolean pointOfChange = false;
  private boolean unusualBleeding = false;

  private TrainingEntry(String observationText) {
    this.observationText = observationText;
  }

  public static TrainingEntry forText(String observationText) {
    return new TrainingEntry(observationText);
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

  public ObservationEntry asChartEntry(LocalDate date) throws ObservationParser.InvalidObservationException {
    Observation observation = ObservationParser.parse(observationText).orNull();
    return new ObservationEntry(date, observation, peakDay, intercourse, false, false, pointOfChange, unusualBleeding, null, false, "");
  }
}
