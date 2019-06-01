package com.roamingroths.cmcc.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
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
  @Nullable
  public LocalDate endDate;
  @TypeConverters(Converters.class)
  public List<Instruction> activeItems;

  @ParcelConstructor
  public Instructions(@NonNull LocalDate startDate, @Nullable LocalDate endDate, @NonNull List<Instruction> activeItems) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.activeItems = new ArrayList<>();
    this.activeItems.addAll(activeItems);
  }

  public Instructions(@NonNull LocalDate startDate, @NonNull Optional<LocalDate> endDate, @NonNull List<Instruction> activeItems) {
    this(startDate, endDate.orNull(), activeItems);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o instanceof Instructions) {
      Instructions that = (Instructions) o;
      return Objects.equal(this.startDate, that.startDate)
          && Objects.equal(this.endDate, that.endDate)
          && this.activeItems.size() == that.activeItems.size()
          && this.activeItems.containsAll(that.activeItems);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(startDate, endDate, activeItems);
  }
}
