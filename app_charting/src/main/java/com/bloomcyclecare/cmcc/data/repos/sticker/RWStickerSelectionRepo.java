package com.bloomcyclecare.cmcc.data.repos.sticker;

import com.bloomcyclecare.cmcc.models.stickering.StickerSelection;

import org.joda.time.LocalDate;

import io.reactivex.Completable;

public interface RWStickerSelectionRepo extends ROStickerSelectionRepo {

  Completable recordSelection(StickerSelection selection, LocalDate entryDate);

  Completable deleteAll();

  Completable delete(LocalDate date);
}
