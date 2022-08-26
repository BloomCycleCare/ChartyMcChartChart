package com.bloomcyclecare.cmcc.data.models.medication;

import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.auto.value.AutoValue;

import org.parceler.Parcel;

import java.util.Objects;

@AutoValue
@Entity
public abstract class Medication implements Parcelable {
  @AutoValue.CopyAnnotations
  @PrimaryKey(autoGenerate = true)
  public abstract long id();
  public abstract String name();
  public abstract String description();

  public static Medication create(long id, String name, String description) {
    return new AutoValue_Medication(id, name, description);
  }

  public Medication() {}
}
