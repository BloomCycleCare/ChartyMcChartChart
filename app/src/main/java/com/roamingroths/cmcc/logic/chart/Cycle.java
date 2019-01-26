package com.roamingroths.cmcc.logic.chart;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

import javax.crypto.SecretKey;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 4/30/17.
 */

public class Cycle implements Parcelable {

  public final String id;
  public final LocalDate startDate;
  public LocalDate endDate;
  public final String startDateStr;
  public transient Keys keys;

  public static class Builder {
    public final String id;
    public final LocalDate startDate;
    public LocalDate endDate;
    public Cycle previousCycle;
    public Cycle nextCycle;

    private Builder(String id, LocalDate startDate) {
      this.id = id;
      this.startDate = startDate;
      endDate = null;
    }

    public Cycle build() throws NoSuchAlgorithmException {
      Cycle.Keys keys = new Cycle.Keys(
          AesCryptoUtil.createKey(), AesCryptoUtil.createKey(), AesCryptoUtil.createKey());
      return new Cycle(
          id,
          startDate,
          endDate,
          keys);
    }
  }

  public void createKeys() throws Exception {
    this.keys = new Cycle.Keys(AesCryptoUtil.createKey(), AesCryptoUtil.createKey(), AesCryptoUtil.createKey());
  }

  public static Builder builder(String id, LocalDate startDate) {
    return new Builder(id, startDate);
  }

  public static Cycle fromSnapshot(DataSnapshot snapshot, Keys keys) {
    LocalDate startDate = DateUtil.fromWireStr(snapshot.child("start-date").getValue(String.class));
    LocalDate endDate = null;
    if (snapshot.hasChild("end-date")) {
      endDate = DateUtil.fromWireStr(snapshot.child("end-date").getValue(String.class));
    }
    return new Cycle(snapshot.getKey(), startDate, endDate, keys);
  }

  public static Function<Keys, Cycle> fromSnapshot(final DataSnapshot snapshot) {
    return new Function<Keys, Cycle>() {
      @Override
      public Cycle apply(@NonNull Keys keys) throws Exception {
        return fromSnapshot(snapshot, keys);
      }
    };
  }

  public static Comparator<Cycle> comparator() {
    return new Comparator<Cycle>() {
      @Override
      public int compare(Cycle c1, Cycle c2) {
        if (c1.equals(c2)) {
          return 0;
        }
        return c1.startDate.isBefore(c2.startDate) ? -1 : 1;
      }
    };
  }

  public Cycle(String id, LocalDate startDate, LocalDate endDate, Keys keys) {
    Preconditions.checkNotNull(startDate);
    this.id = id;
    this.startDate = startDate;
    this.startDateStr = DateUtil.toWireStr(this.startDate);
    this.endDate = endDate;
    this.keys = keys;
  }

  public Cycle(Cycle other) {
    this.id = other.id;
    this.startDate = other.startDate;
    this.startDateStr = other.startDateStr;
    this.endDate = other.endDate;
    this.keys = other.keys;
  }

  protected Cycle(Parcel in) {
    this(
        in.readString(),
        Preconditions.checkNotNull(DateUtil.fromWireStr(in.readString())),
        DateUtil.fromWireStr(in.readString()),
        new Keys(
            AesCryptoUtil.parseKey(in.readString()),
            AesCryptoUtil.parseKey(in.readString()),
            AesCryptoUtil.parseKey(in.readString())));
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
    dest.writeString(id);
    dest.writeString(startDateStr);
    dest.writeString(DateUtil.toWireStr(endDate));
    dest.writeString(AesCryptoUtil.serializeKey(keys.chartKey));
    dest.writeString(AesCryptoUtil.serializeKey(keys.wellnessKey));
    dest.writeString(AesCryptoUtil.serializeKey(keys.symptomKey));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cycle) {
      Cycle that = (Cycle) o;
      return Objects.equal(this.id, that.id) &&
          Objects.equal(this.startDate, that.startDate) &&
          Objects.equal(this.endDate, that.endDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, startDate, endDate);
  }

  public void setKeys(Keys keys) {
    this.keys = keys;
  }

  public static class Keys {
    public SecretKey chartKey;
    public SecretKey wellnessKey;
    public SecretKey symptomKey;

    public Keys(SecretKey chartKey, SecretKey wellnessKey, SecretKey symptomKey) {
      this.chartKey = chartKey;
      this.wellnessKey = wellnessKey;
      this.symptomKey = symptomKey;
    }
  }

  @Override
  public String toString() {
    return id;
  }
}
