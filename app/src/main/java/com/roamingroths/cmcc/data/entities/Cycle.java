package com.roamingroths.cmcc.data.entities;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

/**
 * Created by parkeroth on 4/30/17.
 */
@Parcel
@Entity
public class Cycle {

  public String id;
  @PrimaryKey
  @NonNull
  public LocalDate startDate;
  public LocalDate endDate;
  @Ignore
  public String startDateStr;

  @Ignore
  public Cycle() {}

  public Cycle(String id, LocalDate startDate, LocalDate endDate) {
    Preconditions.checkNotNull(startDate);
    this.id = id;
    this.startDate = startDate;
    this.startDateStr = DateUtil.toWireStr(this.startDate);
    this.endDate = endDate;
  }

  public Cycle(Cycle other) {
    this.id = other.id;
    this.startDate = other.startDate;
    this.startDateStr = other.startDateStr;
    this.endDate = other.endDate;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cycle) {
      Cycle that = (Cycle) o;
      return Objects.equal(this.id, that.id) &&
          Objects.equal(this.startDate, that.startDate) &&
          Objects.equal(this.endDate, that.endDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, startDate, endDate);
  }

  @Override
  public String toString() {
    return id;
  }
}
