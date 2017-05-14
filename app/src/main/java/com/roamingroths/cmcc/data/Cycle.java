package com.roamingroths.cmcc.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by parkeroth on 4/30/17.
 */

public class Cycle implements Parcelable {

  public String id;
  public Date startDate;
  public List<ChartEntry> entries;

  public Cycle(String id, Date startDate, List<ChartEntry> entries) {
    this.id = id;
    this.startDate = startDate;
    this.entries = entries;
  }

  protected Cycle(Parcel in) {
    this(in.readString(), readWireDate(in.readString()), in.createTypedArrayList(ChartEntry.CREATOR));
  }

  public static Cycle fromDataSnapshot(Context context, DataSnapshot cycleSnapshot)
      throws CryptoUtil.CryptoException, ParseException {
    Date startDate =
        DateUtil.fromWireStr(cycleSnapshot.child("start-date").getValue(String.class));
    List<ChartEntry> entries = new ArrayList<>();
    if (cycleSnapshot.hasChild("entries")) {
      PrivateKey privateKey = CryptoUtil.getPersonalPrivateKey(context);
      for (DataSnapshot entrySnapshot : cycleSnapshot.child("entries").getChildren()) {
        String encryptedEntry = entrySnapshot.getValue(String.class);
        entries.add(CryptoUtil.decrypt(encryptedEntry, privateKey, ChartEntry.class));
      }
    }
    return new Cycle(cycleSnapshot.getKey(), startDate, entries);
  }

  private static Date readWireDate(String wireDate) {
    try {
      return DateUtil.fromWireStr(wireDate);
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

  public String getDateStr() {
    return DateUtil.toWireStr(startDate);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(DateUtil.toWireStr(startDate));
    dest.writeTypedList(entries);
  }
}
