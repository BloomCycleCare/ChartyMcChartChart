package com.bloomcyclecare.cmcc.data.repos.sticker;

import android.util.Range;

import com.bloomcyclecare.cmcc.data.models.StickerSelection;

import org.joda.time.LocalDate;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

class TrainingStickerSelectionRepo implements RWStickerSelectionRepo {

  private final TreeMap<LocalDate, StickerSelection> mSelections = new TreeMap<>();
  private final Subject<UpdateEvent> mUpdateSubject = PublishSubject.create();

  @Override
  public Observable<UpdateEvent> updateStream() {
    return mUpdateSubject;
  }

  @Override
  public Completable recordSelection(StickerSelection selection, LocalDate entryDate) {
    return Completable.defer(() -> {
      mSelections.put(entryDate, selection);
      mUpdateSubject.onNext(UpdateEvent.create(entryDate, selection));
      return Completable.complete();
    });
  }

  @Override
  public Flowable<Map<LocalDate, StickerSelection>> getSelections(Range<LocalDate> dateRange) {
    return Flowable.just(mSelections.subMap(dateRange.getLower(), dateRange.getUpper()));
  }

  @Override
  public Single<Optional<StickerSelection>> getSelection(LocalDate date) {
    return Single.just(Optional.ofNullable(mSelections.get(date)));
  }
}
