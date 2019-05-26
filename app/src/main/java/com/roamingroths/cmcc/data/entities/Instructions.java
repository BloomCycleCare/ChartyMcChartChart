package com.roamingroths.cmcc.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.common.base.Objects;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

@Parcel
@Entity
public class Instructions {

  @NonNull
  @PrimaryKey
  public LocalDate startDate;
  @Nullable
  public LocalDate endDate;

  public boolean usePostPeakYellowStickers = false;
  public boolean usePrePeakYellowStickers = false;
  public boolean useSpecialSamenessYellowStickers = false;

  @Override
  public boolean equals(@Nullable Object o) {
    if (o instanceof Instructions) {
      Instructions that = (Instructions) o;
      return Objects.equal(this.startDate, that.startDate)
          && Objects.equal(this.endDate, that.endDate)
          && Objects.equal(this.usePostPeakYellowStickers, that.usePostPeakYellowStickers)
          && Objects.equal(this.usePrePeakYellowStickers, that.usePrePeakYellowStickers)
          && Objects.equal(this.useSpecialSamenessYellowStickers, that.useSpecialSamenessYellowStickers);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(startDate, endDate, usePostPeakYellowStickers, usePrePeakYellowStickers, useSpecialSamenessYellowStickers);
  }
}
