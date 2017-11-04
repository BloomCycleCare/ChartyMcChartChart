package com.roamingroths.cmcc.logic;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.crypto.Cipherable;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 9/18/17.
 */

public class WellnessEntry extends Entry implements Parcelable, Cipherable {

  public Map<String, Boolean> wellnessItems;

  public static void fromEncryptedString(
      String encryptedEntry, SecretKey secretKey, Callbacks.Callback<WellnessEntry> callback) {
    CryptoUtil.decrypt(encryptedEntry, secretKey, WellnessEntry.class, callback);
  }

  public static void fromSnapshot(
      DataSnapshot snapshot, SecretKey secretKey, Callbacks.Callback<WellnessEntry> callback) {
    fromEncryptedString(snapshot.getValue(String.class), secretKey, callback);
  }

  public static WellnessEntry emptyEntry(LocalDate date, SecretKey key) {
    return new WellnessEntry(date, new HashMap<String, Boolean>(), key);
  }

  public WellnessEntry(LocalDate date, Map<String, Boolean> wellnessItems, SecretKey key) {
    super(date);
    this.wellnessItems = wellnessItems;
    swapKey(key);
  }

  public WellnessEntry(Parcel in) {
    super(DateUtil.fromWireStr(in.readString()));
    int size = in.readInt();
    wellnessItems = new HashMap<>(size);
    for (int i = 0; i < size; i++) {
      wellnessItems.put(in.readString(), in.readByte() != 0);
    }
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