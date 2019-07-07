package com.roamingroths.cmcc.logic.chart;

import com.roamingroths.cmcc.R;

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
