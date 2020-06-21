package com.bloomcyclecare.cmcc.data.models.pregnancy;

import android.os.Parcelable;

import org.parceler.Parcels;

public class WrappedPregnancy implements Parcelable {

  public Pregnancy pregnancy;

  WrappedPregnancy(Pregnancy pregnancy) {
    this.pregnancy = pregnancy;
  }

  WrappedPregnancy(android.os.Parcel in) {
    this(Parcels.<Pregnancy>unwrap(in.readParcelable(Pregnancy.class.getClassLoader())));
  }

  public static final Creator<WrappedPregnancy> CREATOR = new Creator<WrappedPregnancy>() {
    @Override
    public WrappedPregnancy createFromParcel(android.os.Parcel in) {
      return new WrappedPregnancy(in);
    }

    @Override
    public WrappedPregnancy[] newArray(int size) {
      return new WrappedPregnancy[size];
    }
  };

  @Override
  public int describeContents() {
    return Parcels.wrap(pregnancy).describeContents();
  }

  @Override
  public void writeToParcel(android.os.Parcel dest, int flags) {
    dest.writeParcelable(Parcels.wrap(pregnancy), flags);
  }
}
