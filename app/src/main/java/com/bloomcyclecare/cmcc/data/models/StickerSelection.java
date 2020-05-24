package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.StickerColor;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import org.parceler.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

import static com.bloomcyclecare.cmcc.data.models.StickerSelection.Sticker.GREY;
import static com.bloomcyclecare.cmcc.logic.chart.StickerColor.WHITE;

@Parcel
public class StickerSelection {

  public Sticker sticker;
  @Nullable public Text text;

  public static StickerSelection create(Sticker sticker, Text text) {
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
        Text.fromString(expectations.peakText));
  }

  public static StickerSelection fromRenderableEntry(CycleRenderer.RenderableEntry renderableEntry, ViewMode viewMode) {
    return create(
        Sticker.fromStickerColor(renderableEntry.backgroundColor(), renderableEntry.showBaby()),
        viewMode == ViewMode.TRAINING ? null : Text.fromString(renderableEntry.peakDayText()));
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

    public static Sticker fromStickerColor(StickerColor stickerColor, boolean showBaby) {
      if (showBaby) {
        switch (stickerColor) {
          case GREEN:
            return Sticker.GREEN_BABY;
          case WHITE:
            return Sticker.WHITE_BABY;
          case YELLOW:
            return Sticker.YELLOW_BABY;
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
