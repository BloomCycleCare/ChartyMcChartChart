package com.bloomcyclecare.cmcc.data.models.medication;

import android.os.Parcelable;

import org.parceler.Parcels;

public class WrappedMedication implements Parcelable {

  public Medication medication;

  public WrappedMedication(Medication medication) {
    this.medication = medication;
  }

  WrappedMedication(android.os.Parcel in) {
    this(Parcels.<Medication>unwrap(in.readParcelable(Medication.class.getClassLoader())));
  }

  public static final Creator<WrappedMedication> CREATOR = new Creator<WrappedMedication>() {
    @Override
    public WrappedMedication createFromParcel(android.os.Parcel in) {
      return new WrappedMedication(in);
    }

    @Override
    public WrappedMedication[] newArray(int size) {
      return new WrappedMedication[size];
    }
  };

  @Override
  public int describeContents() {
    return Parcels.wrap(medication).describeContents();
  }

  @Override
  public void writeToParcel(android.os.Parcel dest, int flags) {
    dest.writeParcelable(Parcels.wrap(medication), flags);
  }
}
