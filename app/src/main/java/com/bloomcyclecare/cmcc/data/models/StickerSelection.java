package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.StickerColor;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.parceler.Parcel;

import androidx.annotation.Nullable;
import timber.log.Timber;

import static com.bloomcyclecare.cmcc.data.models.StickerSelection.Sticker.GREY;
import static com.bloomcyclecare.cmcc.data.models.StickerSelection.Sticker.YELLOW;
import static com.bloomcyclecare.cmcc.logic.chart.StickerColor.WHITE;

@Parcel
public class StickerSelection {

  public Sticker sticker;
  public Text text;

  public static StickerSelection fromRenderableEntry(CycleRenderer.RenderableEntry renderableEntry) {
    Preconditions.checkArgument(renderableEntry.peakDayText().length() <= 1);
    StickerSelection stickerSelection = new StickerSelection();
    stickerSelection.text = Text.fromString(renderableEntry.peakDayText());
    if (renderableEntry.showBaby()) {
      switch (renderableEntry.backgroundColor()) {
        case GREEN:
          stickerSelection.sticker = Sticker.GREEN_BABY;
          break;
        case WHITE:
          stickerSelection.sticker = Sticker.WHITE_BABY;
          break;
        case YELLOW:
          stickerSelection.sticker = Sticker.YELLOW_BABY;
          break;
        default:
          throw new IllegalStateException();
      }
    } else {
      switch (renderableEntry.backgroundColor()) {
        case GREY:
          stickerSelection.sticker = GREY;
          break;
        case GREEN:
          stickerSelection.sticker = Sticker.GREEN;
          break;
        case RED:
          stickerSelection.sticker = Sticker.RED;
          break;
        case YELLOW:
          stickerSelection.sticker = YELLOW;
          break;
        default:
          throw new IllegalStateException();
      }
    }
    return stickerSelection;
  }

  public boolean hasSticker() {
    return sticker != null && sticker != GREY;
  }

  public boolean isEmpty() {
    return text == null && !hasSticker();
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

  public enum Sticker {
    GREY(StickerColor.GREY, false, R.drawable.sticker_grey),
    RED(StickerColor.RED, false, R.drawable.sticker_red),
    GREEN(StickerColor.GREEN, false, R.drawable.sticker_green),
    GREEN_BABY(StickerColor.GREEN, true, R.drawable.sticker_green_baby),
    WHITE_BABY(WHITE, true, R.drawable.sticker_white_baby),
    YELLOW(StickerColor.YELLOW, false, R.drawable.sticker_yellow),
    YELLOW_BABY(StickerColor.YELLOW, true, R.drawable.sticker_yellow_baby);

    public final StickerColor color;
    public final boolean hasBaby;
    public final int resourceId;

    Sticker(StickerColor color, boolean hasBaby, int resourceId) {
      this.color = color;
      this.hasBaby = hasBaby;
      this.resourceId = resourceId;
    }
  }

  public enum Text {
    P('P'), ONE('1'), TWO('2'), THREE('3');

    public final char value;

    Text(char value) {
      this.value = value;
    }

    @Nullable
    public static Text fromString(String str) {
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

}
