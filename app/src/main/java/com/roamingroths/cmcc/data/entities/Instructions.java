package com.roamingroths.cmcc.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.common.base.Objects;
import com.roamingroths.cmcc.data.db.Converters;
import com.roamingroths.cmcc.data.domain.Instruction;

import org.joda.time.LocalDate;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.util.ArrayList;
import java.util.List;

@Parcel
@Entity
public class Instructions {

  @NonNull
  @PrimaryKey
  public LocalDate startDate;
  @TypeConverters(Converters.class)
  public List<Instruction> activeItems;

  @ParcelConstructor
  public Instructions(@NonNull LocalDate startDate, @NonNull List<Instruction> activeItems) {
    this.startDate = startDate;
    this.activeItems = new ArrayList<>();
    this.activeItems.addAll(activeItems);
  }

  public Instructions(Instructions that) {
    this(that.startDate, that.activeItems);
  }

  @NonNull
  @Override
  public String toString() {
    return startDate.toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o instanceof Instructions) {
      Instructions that = (Instructions) o;
      return Objects.equal(this.startDate, that.startDate)
          && this.activeItems.size() == that.activeItems.size()
          && this.activeItems.containsAll(that.activeItems);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(startDate, activeItems);
  }
}
