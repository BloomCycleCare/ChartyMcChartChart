package com.bloomcyclecare.cmcc.models.stickering;

import com.bloomcyclecare.cmcc.R;

public enum StickerColor {
  WHITE(R.color.entryWhite),
  YELLOW(R.color.entryYellow),
  GREY(R.color.entryGrey),
  GREEN(R.color.entryGreen),
  RED(R.color.entryRed);

  public int resourceId;

  StickerColor(int resourceId) {
    this.resourceId = resourceId;
  }
}
