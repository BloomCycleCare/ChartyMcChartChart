package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import io.reactivex.Completable;
import io.reactivex.Flowable;

public interface RWChartEntryRepo extends ROChartEntryRepo {

  class UpdateEvent {
    public final DateTime updateTime;
    public final LocalDate updateTarget;

    UpdateEvent(DateTime updateTime, LocalDate updateTarget) {
      this.updateTime = updateTime;
      this.updateTarget = updateTarget;
    }

    static UpdateEvent forEntry(ChartEntry entry) {
      return new UpdateEvent(DateTime.now(), entry.entryDate);
    }
  }

  Flowable<UpdateEvent> updateEvents();

  Completable updateStickerSelection(LocalDate date, StickerSelection selection);

  Completable insert(ChartEntry entry);

  Completable deleteAll();

  Completable delete(ChartEntry entry);
}
