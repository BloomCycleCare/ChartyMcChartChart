package com.bloomcyclecare.cmcc.models.stickering;

import com.google.common.base.Strings;

import androidx.annotation.Nullable;
import timber.log.Timber;

public enum StickerText {
  P('P'), ONE('1'), TWO('2'), THREE('3');

  public final char value;

  StickerText(char value) {
    this.value = value;
  }

  @Nullable
  public static StickerText fromString(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return null;
    }
    if (str.length() > 1) {
      Timber.w("Invalid string! %s", str);
      return null;
    }
    switch (str.charAt(0)) {
      case 'P':
        return P;
      case '1':
        return ONE;
      case '2':
        return TWO;
      case '3':
        return THREE;
      default:
        Timber.w("Invalid string! %s", str);
        return null;
    }
  }
}
