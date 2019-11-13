package com.roamingroths.cmcc.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.joda.time.LocalDate;

@Entity
public class CycleStats {
  @PrimaryKey
  public LocalDate cycleStartDate;

  public Float mcs;
  public Integer daysPrePeak;
  public Integer daysPostPeak;
}
