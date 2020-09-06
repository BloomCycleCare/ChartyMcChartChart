package com.bloomcyclecare.cmcc.data.models.instructions;

import com.bloomcyclecare.cmcc.data.db.Converters;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Parcel
@Entity
public class Instructions {

  public static Instructions createBasicInstructions(LocalDate startDate) {
    return new Instructions(startDate, ImmutableList.of(
        BasicInstruction.D_1, BasicInstruction.D_2, BasicInstruction.D_3, BasicInstruction.D_4, BasicInstruction.D_5, BasicInstruction.D_6,
        BasicInstruction.E_1, /*BasicInstruction.E_2,*/ BasicInstruction.E_3, BasicInstruction.E_4, /*BasicInstruction.E_5, BasicInstruction.E_6,*/ BasicInstruction.E_7),
        ImmutableList.of(), ImmutableList.of());
  }

  @NonNull
  @PrimaryKey
  public LocalDate startDate;
  @TypeConverters(Converters.class)
  public List<BasicInstruction> activeItems;
  @TypeConverters(Converters.class)
  public List<SpecialInstruction> specialInstructions;
  @TypeConverters(Converters.class)
  public List<YellowStampInstruction> yellowStampInstructions;

  @ParcelConstructor
  public Instructions(
      @NonNull LocalDate startDate,
      @NonNull List<BasicInstruction> activeItems,
      @NonNull List<SpecialInstruction> specialInstructions,
      List<YellowStampInstruction> yellowStampInstructions) {
    this.startDate = startDate;
    this.activeItems = new ArrayList<>(activeItems);
    this.specialInstructions = new ArrayList<>(specialInstructions);
    if (yellowStampInstructions == null) {
      this.yellowStampInstructions = new ArrayList<>();
    } else {
      this.yellowStampInstructions = new ArrayList<>(yellowStampInstructions);
    }
  }

  public Instructions(Instructions that) {
    this(that.startDate, that.activeItems, that.specialInstructions, that.yellowStampInstructions);
  }

  public Instructions addInstructions(BasicInstruction... instructions) {
    activeItems.addAll(Arrays.asList(instructions));
    return this;
  }

  public Instructions addInstructions(YellowStampInstruction... instructions) {
    yellowStampInstructions.addAll(Arrays.asList(instructions));
    return this;
  }

  public boolean isActive(SpecialInstruction specialInstruction) {
    return specialInstructions.contains(specialInstruction);
  }

  public boolean isActive(BasicInstruction basicInstruction) {
    return activeItems.contains(basicInstruction);
  }

  public boolean isActive(YellowStampInstruction yellowStampInstruction) {
    return yellowStampInstructions.contains(yellowStampInstruction);
  }

  public boolean anyActive(BasicInstruction... basicInstructions) {
    return anyActive(Arrays.asList(basicInstructions));
  }

  public boolean anyActive(Collection<BasicInstruction> basicInstructions) {
    for (BasicInstruction i : basicInstructions) {
      if (isActive(i)) {
        return true;
      }
    }
    return false;
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
          && this.activeItems.containsAll(that.activeItems)
          && this.yellowStampInstructions.size() == that.yellowStampInstructions.size()
          && this.yellowStampInstructions.containsAll(that.yellowStampInstructions)
          && this.specialInstructions.size() == that.specialInstructions.size()
          && this.specialInstructions.containsAll(that.specialInstructions);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(startDate, activeItems, specialInstructions, yellowStampInstructions);
  }
}
