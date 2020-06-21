package com.bloomcyclecare.cmcc.data.models.pregnancy;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Parcel
@Entity
public class Pregnancy {
  @PrimaryKey(autoGenerate = true)
  public long id;

  public LocalDate positiveTestDate;

  @Nullable
  public LocalDate dueDate;

  @Nullable
  public LocalDate deliveryDate;

  public Pregnancy() {}

  private Pregnancy(Pregnancy that) {
    this.id = that.id;
    this.positiveTestDate = that.positiveTestDate;
  }

  public WrappedPregnancy wrap() {
    return new WrappedPregnancy(this);
  }
}
