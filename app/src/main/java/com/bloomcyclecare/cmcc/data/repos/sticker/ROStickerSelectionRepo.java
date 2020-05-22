package com.bloomcyclecare.cmcc.data.repos.sticker;

import android.util.Range;

import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.google.auto.value.AutoValue;

import org.joda.time.LocalDate;

import java.util.Map;
import java.util.Optional;

import androidx.annotation.Nullable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface ROStickerSelectionRepo {

  @AutoValue
  abstract class UpdateEvent {
    @Nullable public abstract LocalDate date();
    @Nullable public abstract StickerSelection selection();

    static UpdateEvent create(@Nullable LocalDate date, @Nullable StickerSelection selection) {
      return new AutoValue_ROStickerSelectionRepo_UpdateEvent(date, selection);
    }
  }

  Observable<UpdateEvent> updateStream();

  Flowable<Map<LocalDate, StickerSelection>> getSelections(Range<LocalDate> dateRange);

  Flowable<Map<LocalDate, StickerSelection>> getSelections();

  Single<Optional<StickerSelection>> getSelection(LocalDate date);

  Flowable<Optional<StickerSelection>> getSelectionStream(LocalDate date);
}
