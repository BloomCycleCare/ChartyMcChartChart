package com.roamingroths.cmcc.data.entities;

import androidx.annotation.Nullable;
import androidx.room.Entity;

import com.google.common.base.Objects;
import com.roamingroths.cmcc.data.domain.ClarifyingQuestion;
import com.roamingroths.cmcc.data.domain.IntercourseTimeOfDay;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.utils.Copyable;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parkeroth on 4/22/17.
 */
@Entity
@Parcel
public class ObservationEntry extends Entry implements Copyable<ObservationEntry> {

  @Nullable public Observation observation;
  public boolean peakDay;
  public boolean intercourse;
  public boolean firstDay;
  public boolean pointOfChange;
  public boolean unusualBleeding;
  public boolean unusualStress;
  public boolean unusualBuildup;
  public IntercourseTimeOfDay intercourseTimeOfDay;
  public boolean isEssentiallyTheSame;
  public String note;

  public ObservationEntry(
      LocalDate entryDate,
      @Nullable Observation observation,
      boolean peakDay,
      boolean intercourse,
      boolean firstDay,
      boolean pointOfChange,
      boolean unusualBleeding,
      IntercourseTimeOfDay intercourseTimeOfDay,
      boolean isEssentiallyTheSame,
      String note) {
    super(entryDate);
    this.observation = observation;
    this.peakDay = peakDay;
    this.intercourse = intercourse;
    this.firstDay = firstDay;
    this.pointOfChange = pointOfChange;
    if (unusualBleeding && (observation == null || !observation.hasBlood())) {
      throw new IllegalArgumentException();
    }
    this.unusualBleeding = unusualBleeding;
    this.intercourseTimeOfDay = intercourseTimeOfDay;
    this.isEssentiallyTheSame = isEssentiallyTheSame;
    this.note = note;
  }

  public ObservationEntry() {
    super();
  }

  public static ObservationEntry emptyEntry(LocalDate date) {
    return new ObservationEntry(date, null, false, false, false, false, false, IntercourseTimeOfDay.NONE, false, "");
  }

  @Override
  public List<String> getSummaryLines() {
    List<String> lines = new ArrayList<>();
    if (observation != null) {
      lines.addAll(observation.getSummaryLines());
    } else {
      lines.add("Empty Observation");
    }
    if (intercourseTimeOfDay != IntercourseTimeOfDay.NONE) {
      lines.add("Intercourse @ " + intercourseTimeOfDay.toString());
    }
    if (pointOfChange) {
      lines.add("Point of change: yes");
    }
    if (peakDay) {
      lines.add("Peak day: yes");
    }
    if (unusualBleeding) {
      lines.add("Unusual bleeding: yes");
    }
    if (isEssentiallyTheSame) {
      lines.add("Essentially the same: yes");
    }
    return lines;
  }

  public boolean updateClarifyingQuestion(ClarifyingQuestion question, Boolean answer) {
    if (answer == null) {
      return false;
    }
    switch (question) {
      case UNUSUAL_BLEEDING:
        unusualBleeding = answer;
        return true;
      case UNUSUAL_BUILDUP:
        unusualBuildup = answer;
        return true;
      case UNUSUAL_STRESS:
        unusualStress = answer;
        return true;
      case ESSENTIAL_SAMENESS:
        isEssentiallyTheSame = answer;
        return true;
    }
    return false;
  }

  public boolean hasMucus() {
    if (observation == null || observation.dischargeSummary == null) {
      return false;
    }
    return observation.dischargeSummary.hasMucus();
  }

  public boolean hasBlood() {
    if (observation == null) {
      return false;
    }
    if (observation.flow != null) {
      return true;
    }
    if (observation.dischargeSummary == null) {
      return false;
    }
    return observation.dischargeSummary.hasBlood();
  }

  public boolean hasObservation() {
    return observation != null;
  }

  public boolean isDryDay() {
    return hasObservation() && !hasBlood() && !hasMucus();
  }

  public boolean hasPeakTypeMucus() {
    return hasMucus() && observation.dischargeSummary.isPeakType();
  }

  public String getListUiText() {
    if (observation == null) {
      return "----";
    }
    return String.format("%s %s", observation.toString(), intercourse ? "I" : "");
  }
  @Override
  public boolean equals(Object o) {
    if (o instanceof ObservationEntry) {
      ObservationEntry that = (ObservationEntry) o;
      return Objects.equal(this.observation, that.observation) &&
          Objects.equal(this.getDate(), that.getDate()) &&
          this.peakDay == that.peakDay &&
          this.intercourse == that.intercourse &&
          this.firstDay == that.firstDay &&
          this.pointOfChange == that.pointOfChange &&
          this.unusualBleeding == that.unusualBleeding &&
          this.isEssentiallyTheSame == that.isEssentiallyTheSame &&
          this.intercourseTimeOfDay == that.intercourseTimeOfDay &&
          Objects.equal(this.note, that.note);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        observation, peakDay, intercourse, getDate(), firstDay, pointOfChange, unusualBleeding, intercourseTimeOfDay, isEssentiallyTheSame, note);
  }

  @Override
  public ObservationEntry copy() {
    return new ObservationEntry(mEntryDate, observation, peakDay, intercourse, firstDay, pointOfChange, unusualBleeding, intercourseTimeOfDay, isEssentiallyTheSame, note);
  }
}

