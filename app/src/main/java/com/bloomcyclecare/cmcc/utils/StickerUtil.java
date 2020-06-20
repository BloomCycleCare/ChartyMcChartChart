package com.bloomcyclecare.cmcc.utils;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.models.stickering.Sticker;

public class StickerUtil {

  public static int resourceId(Sticker sticker) {
    switch (sticker) {
      case GREY:
        return R.drawable.sticker_grey;
      case RED:
        return R.drawable.sticker_red;
      case GREEN:
        return R.drawable.sticker_green;
      case GREEN_BABY:
        return R.drawable.sticker_green_baby;
      case WHITE_BABY:
        return R.drawable.sticker_white_baby;
      case YELLOW:
        return R.drawable.sticker_yellow;
      case YELLOW_BABY:
        return R.drawable.sticker_yellow_baby;
    }
    throw new IllegalStateException();
  }
}
