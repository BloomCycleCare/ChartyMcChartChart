package com.roamingroths.cmcc.logic;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by parkeroth on 4/24/17.
 */
public enum Occurrences implements Parcelable {
  X1("Seen only once"),
  X2("Seen twice"),
  X3("Seen three times"),
  AD("Seen all day");

  private final String description;

  Occurrences(String description) {
    this.description = description;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<Occurrences> CREATOR = new Creator<Occurrences>() {
    @Override
    public Occurrences createFromParcel(Parcel in) {
      return Occurrences.valueOf(in.readString());
    }

    @Override
    public Occurrences[] newArray(int size) {
      return new Occurrences[size];
    }
  };

  public String getDescription() {
    return description;
  }
}
