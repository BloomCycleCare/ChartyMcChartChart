package com.bloomcyclecare.cmcc.data.entities;

import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Created by parkeroth on 4/30/17.
 */
@Parcel
@Entity(foreignKeys = @ForeignKey(
    entity = Pregnancy.class,
    onDelete = ForeignKey.CASCADE,
    parentColumns = "id",
    childColumns = "pregnancyId"))
public class Cycle implements Comparable<Cycle> {

  public String id;
  @PrimaryKey
  @NonNull
  public LocalDate startDate;
  public LocalDate endDate;
  @Ignore
  public String startDateStr;
  public Long pregnancyId;

  @Ignore
  public Cycle() {}

  public Cycle(String id, @NonNull LocalDate startDate, @Nullable LocalDate endDate, @Nullable Long pregnancyId) {
    Preconditions.checkNotNull(startDate);
    this.id = id;
    this.startDate = startDate;
    this.startDateStr = DateUtil.toWireStr(this.startDate);
    this.endDate = endDate;
    this.pregnancyId = pregnancyId;
  }

  public Cycle(Cycle other) {
    this.id = other.id;
    this.startDate = other.startDate;
    this.startDateStr = other.startDateStr;
    this.endDate = other.endDate;
    this.pregnancyId = other.pregnancyId;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cycle) {
      Cycle that = (Cycle) o;
      return Objects.equal(this.id, that.id) &&
          Objects.equal(this.pregnancyId, that.pregnancyId) &&
          Objects.equal(this.startDate, that.startDate) &&
          Objects.equal(this.endDate, that.endDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, startDate, endDate, pregnancyId);
  }

  @Override
  public String toString() {
    return GsonUtil.getGsonInstance().toJson(this);
  }

  @Override
  public int compareTo(Cycle cycle) {
    return this.startDate.compareTo(cycle.startDate);
  }

  public boolean isPregnancy() {
    return pregnancyId != null;
  }
}
