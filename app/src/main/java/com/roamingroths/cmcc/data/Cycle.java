package com.roamingroths.cmcc.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

/**
 * Created by parkeroth on 4/30/17.
 */

public class Cycle implements Parcelable {

  public String id;
  public LocalDate startDate;
  public LocalDate endDate;

  public static Cycle fromSnapshot(DataSnapshot snapshot) {
    LocalDate startDate = DateUtil.fromWireStr(snapshot.child("start-date").getValue(String.class));
    LocalDate endDate = null;
    if (snapshot.hasChild("end-date")) {
      endDate = DateUtil.fromWireStr(snapshot.child("end-date").getValue(String.class));
    }
    return new Cycle(snapshot.getKey(), startDate, endDate);
  }

  public Cycle(String id, LocalDate startDate, LocalDate endDate) {
    this.id = id;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  protected Cycle(Parcel in) {
    this(in.readString(), DateUtil.fromWireStr(in.readString()), DateUtil.fromWireStr(in.readString()));
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
    dest.writeString(DateUtil.toWireStr(startDate));
    dest.writeString(DateUtil.toWireStr(endDate));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cycle) {
      Cycle that = (Cycle) o;
      return this.id.equals(that.id) &&
          this.startDate.equals(that.startDate) &&
          this.endDate.equals(that.endDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, startDate, endDate);
  }
}
