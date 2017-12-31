package com.roamingroths.cmcc.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.logic.ChartEntry;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;

/**
 * Created by parkeroth on 12/29/17.
 */

public class ChartEntryCache {

  private static final int MAX_CACHED_ENTRIES = 200;

  private final Observer<ChartEntry> mFillObserver;
  private final Cache<LocalDate, ChartEntry> mEntryCache =
      CacheBuilder.newBuilder().maximumSize(MAX_CACHED_ENTRIES).build();

  public ChartEntryCache() {
    mFillObserver = new Observer<ChartEntry>() {
      @Override
      public void onSubscribe(Disposable d) {}

      @Override
      public void onNext(ChartEntry chartEntry) {
        mEntryCache.put(chartEntry.entryDate, chartEntry);
      }

      @Override
      public void onError(Throwable e) {}

      @Override
      public void onComplete() {}
    };
  }

  public Observer<ChartEntry> fill() {
    return mFillObserver;
  }

  @Deprecated
  public Action putEntry(final ChartEntry entry) {
    return new Action() {
      @Override
      public void run() throws Exception {
        mEntryCache.put(entry.entryDate, entry);
      }
    };
  }

  public List<ChartEntry> getEntries(List<LocalDate> entryDates) {
    ImmutableList.Builder<ChartEntry> builder = ImmutableList.builder();
    for (LocalDate date : entryDates) {
      ChartEntry entry = mEntryCache.getIfPresent(date);
      if (entry != null) {
        builder.add(entry);
      }
    }
    return builder.build();
  }
}
