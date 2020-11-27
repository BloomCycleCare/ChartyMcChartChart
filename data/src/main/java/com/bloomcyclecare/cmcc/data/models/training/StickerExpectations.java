package com.bloomcyclecare.cmcc.data.models.training;

import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;

import androidx.annotation.Nullable;

public class StickerExpectations {

  public StickerSelection stickerSelection;
  public boolean shouldHaveIntercourse = false;

  private StickerExpectations(StickerSelection stickerSelection) {
    this.stickerSelection = stickerSelection;
  }

  public static StickerExpectations redSticker() {
    return new StickerExpectations(StickerSelection.create(Sticker.RED, null));
  }

  public static StickerExpectations greenSticker() {
    return new StickerExpectations(StickerSelection.create(Sticker.GREEN, null));
  }

  public static StickerExpectations greenBabySticker(@Nullable StickerText text) {
    return new StickerExpectations(StickerSelection.create(Sticker.GREEN_BABY, text));
  }

  public static StickerExpectations yellowBabySticker(@Nullable StickerText text) {
    return new StickerExpectations(StickerSelection.create(Sticker.YELLOW_BABY, text));
  }

  public static StickerExpectations yellowSticker() {
    return new StickerExpectations(StickerSelection.create(Sticker.YELLOW, null));
  }

  public static StickerExpectations whiteBabySticker(@Nullable StickerText text) {
    return new StickerExpectations(StickerSelection.create(Sticker.WHITE_BABY, text));
  }

  public static StickerExpectations greySticker() {
    return new StickerExpectations(StickerSelection.create(Sticker.GREY, null));
  }

  public StickerExpectations withIntercourse() {
    shouldHaveIntercourse = true;
    return this;
  }
}
