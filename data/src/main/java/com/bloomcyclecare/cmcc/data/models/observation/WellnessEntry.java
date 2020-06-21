package com.bloomcyclecare.cmcc.data.models.observation;

import android.os.Parcel;
import android.os.Parcelable;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.bloomcyclecare.cmcc.utils.BoolMapping;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import androidx.room.Entity;

/**
 * Created by parkeroth on 9/18/17.
 */
@Entity
public class WellnessEntry extends Entry implements Parcelable {

  public BoolMapping wellnessItems;

  public static WellnessEntry emptyEntry(LocalDate date) {
    return new WellnessEntry(date, new BoolMapping());
  }

  public WellnessEntry(LocalDate entryDate, Map<String, Boolean> wellnessItems) {
    this(entryDate, wellnessItems, null);
  }

  @Deprecated
  public WellnessEntry(LocalDate entryDate, Map<String, Boolean> wellnessItems, SecretKey key) {
    super(entryDate);
    this.wellnessItems = new BoolMapping(wellnessItems);
  }

  public WellnessEntry(Parcel in) {
    super(DateUtil.fromWireStr(in.readString()));
    int size = in.readInt();
    wellnessItems = new BoolMapping();
    for (int i = 0; i < size; i++) {
      wellnessItems.put(in.readString(), in.readByte() != 0);
    }
  }

  public WellnessEntry() {
    super();
  }

  public boolean hasItem(String key) {
    return wellnessItems.containsKey(key) && wellnessItems.get(key);
  }

  @Override
  public List<String> getSummaryLines() {
    List<String> lines = new ArrayList<>();
    lines.add("Wellness Items");
    for (Map.Entry<String, Boolean> entry : wellnessItems.entrySet()) {
      if (entry.getValue()) {
        lines.add(entry.getKey());
      }
    }
    if (lines.size() == 1) {
      return new ArrayList<>();
    }
    return lines;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(getDateStr());
    dest.writeInt(wellnessItems.size());
    for (Map.Entry<String, Boolean> entry : wellnessItems.entrySet()) {
      dest.writeString(entry.getKey());
      dest.writeByte((byte) (entry.getValue() ? 1 : 0));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WellnessEntry)) {
      return false;
    }
    WellnessEntry that = (WellnessEntry) obj;
    return Objects.equal(this.getDate(), that.getDate())
        && Maps.difference(this.wellnessItems, that.wellnessItems).areEqual();
  }

  @Override
  public int hashCode() {
    Object[] items = new Object[1 + wellnessItems.size()];

    int i = 0;
    items[i++] = getDate();
    for (Map.Entry<String, Boolean> entry : wellnessItems.entrySet()) {
      items[i++] = entry;
    }
    return Objects.hashCode(items);
  }

  public static final Creator<WellnessEntry> CREATOR = new Creator<WellnessEntry>() {
    @Override
    public WellnessEntry createFromParcel(Parcel in) {
      return new WellnessEntry(in);
    }

    @Override
    public WellnessEntry[] newArray(int size) {
      return new WellnessEntry[size];
    }
  };
}
