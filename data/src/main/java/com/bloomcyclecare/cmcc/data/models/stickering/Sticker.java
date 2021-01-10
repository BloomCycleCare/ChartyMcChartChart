package com.bloomcyclecare.cmcc.data.models.stickering;

import static com.bloomcyclecare.cmcc.data.models.stickering.StickerColor.WHITE;

public enum Sticker {
  GREY(StickerColor.GREY, false),
  RED(StickerColor.RED, false),
  GREEN(StickerColor.GREEN, false),
  GREEN_BABY(StickerColor.LIGHTGREEN, true),
  WHITE_BABY(WHITE, true),
  YELLOW(StickerColor.YELLOW, false),
  YELLOW_BABY(StickerColor.YELLOW, true);

  public final StickerColor color;
  public final boolean hasBaby;

  Sticker(StickerColor color, boolean hasBaby) {
    this.color = color;
    this.hasBaby = hasBaby;
  }
}
