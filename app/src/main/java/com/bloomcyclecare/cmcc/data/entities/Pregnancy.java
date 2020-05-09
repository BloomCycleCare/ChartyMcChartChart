package com.bloomcyclecare.cmcc.data.entities;

import android.os.Parcelable;

import org.joda.time.LocalDate;
import org.parceler.Parcel;
import org.parceler.Parcels;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Parcel
@Entity
public class Pregnancy implements Parcelable {
  @PrimaryKey(autoGenerate = true)
  public long id;

  public LocalDate positiveTestDate;

  @Nullable
  public LocalDate dueDate;

  @Nullable
  public LocalDate deliveryDate;

  public Pregnancy() {}

  private Pregnancy(Pregnancy that) {
    this.id = that.id;
    this.positiveTestDate = that.positiveTestDate;
  }

  protected Pregnancy(android.os.Parcel in) {
    this(Parcels.<Pregnancy>unwrap(in.readParcelable(null)));
    id = in.readLong();
  }

  @Override
  public void writeToParcel(android.os.Parcel dest, int flags) {
    dest.writeParcelable(Parcels.wrap(this), flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<Pregnancy> CREATOR = new Creator<Pregnancy>() {
    @Override
    public Pregnancy createFromParcel(android.os.Parcel in) {
      return new Pregnancy(in);
    }

    @Override
    public Pregnancy[] newArray(int size) {
      return new Pregnancy[size];
    }
  };
}
