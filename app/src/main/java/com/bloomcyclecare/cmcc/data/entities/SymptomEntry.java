package com.bloomcyclecare.cmcc.data.entities;

import android.os.Parcel;
import android.os.Parcelable;

import com.bloomcyclecare.cmcc.utils.BoolMapping;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.base.Objects;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
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
public class SymptomEntry extends Entry implements Parcelable {

  public BoolMapping symptoms;

  public static SymptomEntry emptyEntry(LocalDate date) {
    return new SymptomEntry(date, new BoolMapping());
  }

  public SymptomEntry(LocalDate entryDate, BoolMapping symptoms) {
    this(entryDate, symptoms, null);
  }

  @Deprecated
  public SymptomEntry(LocalDate entryDate, Map<String, Boolean> symptoms, SecretKey key) {
    super(entryDate);
    this.symptoms = new BoolMapping(symptoms);
  }

  public SymptomEntry(Parcel in) {
    super(DateUtil.fromWireStr(in.readString()));
    int size = in.readInt();
    symptoms = new BoolMapping();
    for (int i = 0; i < size; i++) {
      symptoms.put(in.readString(), in.readByte() != 0);
    }
  }

  public SymptomEntry() {
    super();
  }

  public int getNumSymptoms() {
    return Collections2.filter(symptoms.values(), Predicates.equalTo(true)).size();
  }

  public boolean hasItem(String key) {
    return symptoms.containsKey(key) && symptoms.get(key);
  }

  @Override
  public List<String> getSummaryLines() {
    List<String> lines = new ArrayList<>();
    lines.add("Symptoms");
    for (Map.Entry<String, Boolean> entry : symptoms.entrySet()) {
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
    dest.writeInt(symptoms.size());
    for (Map.Entry<String, Boolean> entry : symptoms.entrySet()) {
      dest.writeString(entry.getKey());
      dest.writeByte((byte) (entry.getValue() ? 1 : 0));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SymptomEntry)) {
      return false;
    }
    SymptomEntry that = (SymptomEntry) obj;
    return Objects.equal(this.getDate(), that.getDate())
        && Maps.difference(this.symptoms, that.symptoms).areEqual();
  }

  @Override
  public int hashCode() {
    Object[] items = new Object[1 + symptoms.size()];

    int i = 0;
    items[i++] = getDate();
    for (Map.Entry<String, Boolean> entry : symptoms.entrySet()) {
      items[i++] = entry;
    }
    return Objects.hashCode(items);
  }

  public static final Creator<SymptomEntry> CREATOR = new Creator<SymptomEntry>() {
    @Override
    public SymptomEntry createFromParcel(Parcel in) {
      return new SymptomEntry(in);
    }

    @Override
    public SymptomEntry[] newArray(int size) {
      return new SymptomEntry[size];
    }
  };
}
