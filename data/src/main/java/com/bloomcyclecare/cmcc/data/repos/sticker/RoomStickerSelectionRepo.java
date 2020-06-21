package com.bloomcyclecare.cmcc.data.repos.sticker;

import android.util.Range;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.StickerSelectionEntryDao;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelectionEntry;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.google.common.collect.ImmutableMap;

import org.joda.time.LocalDate;

import java.util.Map;
import java.util.Optional;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class RoomStickerSelectionRepo implements RWStickerSelectionRepo {

  private final Subject<UpdateEvent> mUpdateSubject = PublishSubject.create();
  private final StickerSelectionEntryDao mStickerSelectionDao;

  RoomStickerSelectionRepo(@NonNull AppDatabase db) {
    mStickerSelectionDao = db.stickerSelectionEntryDao();
  }

  @Override
  public Completable recordSelection(StickerSelection selection, LocalDate entryDate) {
    return mStickerSelectionDao
        .insert(new StickerSelectionEntry(entryDate, selection))
        .doOnComplete(() -> mUpdateSubject.onNext(UpdateEvent.create(entryDate, selection)))
        .subscribeOn(Schedulers.computation());

  }

  @Override
  public Completable deleteAll() {
    return mStickerSelectionDao.deleteAll()
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable delete(LocalDate date) {
    return getSelection(date).flatMapCompletable(e -> {
      if (!e.isPresent()) {
        return Completable.complete();
      }
      return mStickerSelectionDao.delete(new StickerSelectionEntry(date, e.get()));
    }).subscribeOn(Schedulers.computation());
  }

  @Override
  public Observable<UpdateEvent> updateStream() {
    return mUpdateSubject.hide();
  }

  @Override
  public Flowable<Map<LocalDate, StickerSelection>> getSelections(Range<LocalDate> dateRange) {
    return mStickerSelectionDao
        .getIndexedStream(dateRange.getLower(), dateRange.getUpper())
        .map(m -> {
          ImmutableMap.Builder<LocalDate, StickerSelection> out = ImmutableMap.builder();
          for (Map.Entry<LocalDate, StickerSelectionEntry> e : m.entrySet()) {
            if (e.getValue().selection == null) {
              continue;
            }
            out.put(e.getKey(), e.getValue().selection);
          }
          return out.build();
        });
  }

  @Override
  public Flowable<Map<LocalDate, StickerSelection>> getSelections() {
    return mStickerSelectionDao
        .getStream()
        .map(l -> {
          ImmutableMap.Builder<LocalDate, StickerSelection> out = ImmutableMap.builder();
          for (StickerSelectionEntry e : l) {
            if (e.selection == null) {
              continue;
            }
            out.put(e.getDate(), e.selection);
          }
          return out.build();
        });
  }

  @Override
  public Single<Optional<StickerSelection>> getSelection(LocalDate date) {
    return getSelectionStream(date).firstOrError();
  }

  @Override
  public Flowable<Optional<StickerSelection>> getSelectionStream(LocalDate date) {
    return mStickerSelectionDao
        .getOptionalStream(date)
        .map(stickerSelectionEntryOptional -> {
          if (!stickerSelectionEntryOptional.isPresent()) {
            return Optional.empty();
          }
          return Optional.ofNullable(stickerSelectionEntryOptional.get().selection);
        });
  }
}
