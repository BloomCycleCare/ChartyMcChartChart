package com.bloomcyclecare.cmcc.models.observation;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Strings;

/**
 * Created by parkeroth on 4/24/17.
 */
public enum Flow implements Parcelable {
  H("Heavy flow", true),
  M("Medium flow", true),
  L("Light flow", false),
  VL("Very light flow", false);

  private final String description;
  private final boolean isLegit;

  Flow(String description, boolean isLegit) {
    this.description = description;
    this.isLegit = isLegit;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean isLegit() {
    return isLegit;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<Flow> CREATOR = new Creator<Flow>() {
    @Override
    public Flow createFromParcel(Parcel in) {
      String name = in.readString();
      if (Strings.isNullOrEmpty(name)) {
        return null;
      }
      return Flow.valueOf(name);
    }

    @Override
    public Flow[] newArray(int size) {
      return new Flow[size];
    }
  };
}
