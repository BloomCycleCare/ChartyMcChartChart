package com.roamingroths.cmcc.data.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

/**
 * Created by parkeroth on 4/30/17.
 */
@Entity
public class Cycle implements Parcelable {

  public final String id;
  @PrimaryKey
  @NonNull
  public final LocalDate startDate;
  public LocalDate endDate;
  @Ignore
  public final String startDateStr;

  @Deprecated
  public static class Builder {
    public final String id;
    public final LocalDate startDate;
    public LocalDate endDate;

    private Builder(String id, LocalDate startDate) {
      this.id = id;
      this.startDate = startDate;
      endDate = null;
    }

    public Cycle build() throws NoSuchAlgorithmException {
      return new Cycle(
          id,
          startDate,
          endDate);
    }
  }

  public static Builder builder(String id, LocalDate startDate) {
    return new Builder(id, startDate);
  }

  public static Comparator<Cycle> comparator() {
    return new Comparator<Cycle>() {
      @Override
      public int compare(Cycle c1, Cycle c2) {
        if (c1.equals(c2)) {
          return 0;
        }
        return c1.startDate.isBefore(c2.startDate) ? -1 : 1;
      }
    };
  }

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

  protected Cycle(Parcel in) {
    this(
        in.readString(),
        Preconditions.checkNotNull(DateUtil.fromWireStr(in.readString())),
        DateUtil.fromWireStr(in.readString()));
  }

  public static final Creator<Cycle> CREATOR = new Creator<Cycle>() {
    @Override
    public Cycle createFromParcel(Parcel in) {
      return new Cycle(in);
    }

    @Override
    public Cycle[] newArray(int size) {
      return new Cycle[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(id);
    dest.writeString(startDateStr);
    dest.writeString(DateUtil.toWireStr(endDate));
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
