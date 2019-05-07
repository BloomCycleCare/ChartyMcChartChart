package com.roamingroths.cmcc.ui.entry;

import android.os.Parcel;
import android.os.Parcelable;

import com.roamingroths.cmcc.data.entities.Cycle;

import java.util.ArrayList;

/**
 * Created by parkeroth on 11/19/17.
 */
public class EntrySaveResult implements Parcelable {
  public ArrayList<Cycle> droppedCycles = new ArrayList<>();
  public ArrayList<Cycle> changedCycles = new ArrayList<>();
  public ArrayList<Cycle> newCycles = new ArrayList<>();
  public Cycle cycleToShow;

  public static EntrySaveResult forCycle(Cycle cycleToShow) {
    EntrySaveResult result = new EntrySaveResult();
    result.cycleToShow = cycleToShow;
    return result;
  }

  private EntrySaveResult() {}

  protected EntrySaveResult(Parcel in) {
    droppedCycles = in.createTypedArrayList(Cycle.CREATOR);
    changedCycles = in.createTypedArrayList(Cycle.CREATOR);
    newCycles = in.createTypedArrayList(Cycle.CREATOR);
    cycleToShow = in.readParcelable(Cycle.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedList(droppedCycles);
    dest.writeTypedList(changedCycles);
    dest.writeTypedList(newCycles);
    dest.writeParcelable(cycleToShow, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<EntrySaveResult> CREATOR = new Creator<EntrySaveResult>() {
    @Override
    public EntrySaveResult createFromParcel(Parcel in) {
      return new EntrySaveResult(in);
    }

    @Override
    public EntrySaveResult[] newArray(int size) {
      return new EntrySaveResult[size];
    }
  };
}
