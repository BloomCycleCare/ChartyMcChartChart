package com.bloomcyclecare.cmcc.models.stickering;

import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.google.common.base.Objects;

import org.parceler.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.bloomcyclecare.cmcc.models.stickering.Sticker.GREY;

@Parcel
public class StickerSelection {

  public Sticker sticker;
  @Nullable public StickerText text;

  public static StickerSelection create(Sticker sticker, StickerText text) {
    StickerSelection stickerSelection = new StickerSelection();
    stickerSelection.text = text;
    stickerSelection.sticker = sticker;
    return stickerSelection;
  }

  public static StickerSelection empty() {
    return create(Sticker.GREY, null);
  }

  public static StickerSelection fromExpectations(TrainingCycle.StickerExpectations expectations) {
    return create(
        Sticker.fromStickerColor(expectations.backgroundColor, expectations.shouldHaveBaby),
        StickerText.fromString(expectations.peakText));
  }

  public static StickerSelection fromRenderableEntry(CycleRenderer.RenderableEntry renderableEntry, ViewMode viewMode) {
    return create(
        Sticker.fromStickerColor(renderableEntry.backgroundColor(), renderableEntry.showBaby()),
        viewMode == ViewMode.TRAINING ? null : StickerText.fromString(renderableEntry.peakDayText()));
  }

  public boolean hasSticker() {
    return sticker != null && sticker != GREY;
  }

  public boolean isEmpty() {
    return text == null && !hasSticker();
  }

  @NonNull
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append(sticker.name()).append(":");
    if (text != null) {
      b.append(text.name());
    }
    return b.toString();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof StickerSelection) {
      StickerSelection that = (StickerSelection) obj;

      return Objects.equal(this.sticker, that.sticker)
          && Objects.equal(this.text, that.text);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(sticker, text);
  }

}
