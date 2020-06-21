package com.bloomcyclecare.cmcc.data.models.charting;

import org.joda.time.LocalDate;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class CycleStats {
  @PrimaryKey
  public LocalDate cycleStartDate;

  public Float mcs;
  public Integer daysPrePeak;
  public Integer daysPostPeak;
}
