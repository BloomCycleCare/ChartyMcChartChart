package com.bloomcyclecare.cmcc.models.stickering;

import timber.log.Timber;

import static com.bloomcyclecare.cmcc.models.stickering.StickerColor.WHITE;

public enum Sticker {
  GREY(StickerColor.GREY, false),
  RED(StickerColor.RED, false),
  GREEN(StickerColor.GREEN, false),
  GREEN_BABY(StickerColor.GREEN, true),
  WHITE_BABY(WHITE, true),
  YELLOW(StickerColor.YELLOW, false),
  YELLOW_BABY(StickerColor.YELLOW, true);

  public final StickerColor color;
  public final boolean hasBaby;

  Sticker(StickerColor color, boolean hasBaby) {
    this.color = color;
    this.hasBaby = hasBaby;
  }

  public static Sticker fromStickerColor(StickerColor stickerColor, boolean showBaby) {
    if (showBaby) {
      switch (stickerColor) {
        case GREEN:
          return Sticker.GREEN_BABY;
        case WHITE:
          return Sticker.WHITE_BABY;
        case YELLOW:
          return Sticker.YELLOW_BABY;
        case RED:
          Timber.w("Red baby should not happen!!!");
          return Sticker.RED;
        default:
          throw new IllegalStateException();
      }
    } else {
      switch (stickerColor) {
        case GREY:
          return GREY;
        case GREEN:
          return Sticker.GREEN;
        case RED:
          return Sticker.RED;
        case YELLOW:
          return YELLOW;
        default:
          throw new IllegalStateException();
      }
    }
  }
}
