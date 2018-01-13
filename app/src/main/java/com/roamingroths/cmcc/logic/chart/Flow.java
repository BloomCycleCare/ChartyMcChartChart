package com.roamingroths.cmcc.logic.chart;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Strings;

/**
 * Created by parkeroth on 4/24/17.
 */
public enum Flow implements Parcelable {
  H("Heavy flow"),
  M("Medium flow"),
  L("Light flow"),
  VL("Very light flow");

  private final String description;

  Flow(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
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
