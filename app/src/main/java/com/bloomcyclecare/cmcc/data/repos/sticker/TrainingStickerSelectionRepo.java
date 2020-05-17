package com.bloomcyclecare.cmcc.data.repos.sticker;

import android.util.Range;

import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.google.common.collect.ImmutableMap;

import org.joda.time.LocalDate;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

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
      Timber.v("Updating selection for %s to %s", entryDate, selection);
      mSelections.put(entryDate, selection);
      mUpdateSubject.onNext(UpdateEvent.create(entryDate, selection));
      return Completable.complete();
    });
  }

  @Override
  public Flowable<Map<LocalDate, StickerSelection>> getSelections(Range<LocalDate> dateRange) {
    return mUpdateSubject
        .map(ignoredValue -> subset(dateRange))
        .startWith(subset(dateRange))
        .<Map<LocalDate, StickerSelection>>map(ImmutableMap::copyOf)
        .doOnNext(m -> Timber.v("Emitting new selection for %s", dateRange.toString()))
        .toFlowable(BackpressureStrategy.BUFFER);
  }

  @Override
  public Flowable<Map<LocalDate, StickerSelection>> getSelections() {
    return mUpdateSubject
        .map(ignoredValue -> mSelections)
        .startWith(mSelections)
        .<Map<LocalDate, StickerSelection>>map(ImmutableMap::copyOf)
        .toFlowable(BackpressureStrategy.BUFFER);
  }

  private Map<LocalDate, StickerSelection> subset(Range<LocalDate> dateRange) {
    // NOTE: range upper is inclusive while subMap upper is exclusive
    return mSelections.subMap(dateRange.getLower(), dateRange.getUpper().plusDays(1));
  }

  @Override
  public Single<Optional<StickerSelection>> getSelection(LocalDate date) {
    return Single.just(Optional.ofNullable(mSelections.get(date)));
  }
}
