package com.roamingroths.cmcc.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by parkeroth on 4/30/17.
 */

public class Cycle implements Parcelable {

  private static final SimpleDateFormat WIRE_DATE_FORMAT = new SimpleDateFormat("yyy-MM-dd");

  public Date firstDay;
  public List<ChartEntry> entries;

  public Cycle(Date firstDay, List<ChartEntry> entries) {
    this.firstDay = firstDay;
    this.entries = entries;
  }

  protected Cycle(Parcel in) {
    this(readWireDate(in.readString()), in.createTypedArrayList(ChartEntry.CREATOR));
  }

  private static Date readWireDate(String wireDate) {
    try {
      return WIRE_DATE_FORMAT.parse(wireDate);
    } catch (ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
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
    dest.writeString(WIRE_DATE_FORMAT.format(firstDay));
    dest.writeTypedList(entries);
  }
}
