package com.bloomcyclecare.cmcc.data.repos.sticker;

import android.util.Range;

import com.bloomcyclecare.cmcc.models.stickering.StickerSelection;
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
  public Completable deleteAll() {
    return Completable.defer(() -> {
      mSelections.clear();
      mUpdateSubject.onNext(UpdateEvent.create(null, null));
      return Completable.complete();
    });
  }

  @Override
  public Completable delete(LocalDate date) {
    return Completable.defer(() -> {
      mSelections.remove(date);
      mUpdateSubject.onNext(UpdateEvent.create(date, null));
      return Completable.complete();
    });
  }

  @Override
  public Flowable<Map<LocalDate, StickerSelection>> getSelections(Range<LocalDate> dateRange) {
    return mUpdateSubject
        .doOnNext(u -> Timber.v("FOO"))
        .map(ignoredValue -> subset(dateRange))
        .startWith(subset(dateRange))
        .<Map<LocalDate, StickerSelection>>map(ImmutableMap::copyOf)
        .doOnNext(m -> Timber.v("Emitting new selection for %s", dateRange.toString()))
        .toFlowable(BackpressureStrategy.BUFFER)
        .doOnSubscribe(s -> Timber.v("SUB"))
        .doOnComplete(() -> Timber.d("getSelections: COMPLETE"));
  }

  @Override
  public Flowable<Map<LocalDate, StickerSelection>> getSelections() {
    return mUpdateSubject
        .map(ignoredValue -> ImmutableMap.copyOf(mSelections))
        .startWith(ImmutableMap.copyOf(mSelections))
        .map(tm -> (Map<LocalDate, StickerSelection>) tm)
        .toFlowable(BackpressureStrategy.BUFFER);
  }

  private Map<LocalDate, StickerSelection> subset(Range<LocalDate> dateRange) {
    // NOTE: range upper is inclusive while subMap upper is exclusive
    return ImmutableMap.copyOf(mSelections.subMap(dateRange.getLower(), dateRange.getUpper().plusDays(1)));
  }

  @Override
  public Single<Optional<StickerSelection>> getSelection(LocalDate date) {
    return getSelectionStream(date).firstOrError();
  }

  @Override
  public Flowable<Optional<StickerSelection>> getSelectionStream(LocalDate date) {
    return mUpdateSubject
        .map(ignoredValue -> Optional.ofNullable(mSelections.get(date)))
        .startWith(Optional.ofNullable(mSelections.get(date)))
        .toFlowable(BackpressureStrategy.BUFFER)
        .distinctUntilChanged();
  }
}
