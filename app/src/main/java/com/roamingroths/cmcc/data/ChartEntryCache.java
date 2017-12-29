package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Set;

import io.reactivex.functions.Action;

/**
 * Created by parkeroth on 12/29/17.
 */

public class ChartEntryCache {

  private static final int MAX_CACHED_ENTRIES = 100;
  private static final String TAG = ChartEntryCache.class.getSimpleName();

  private final Set<Cycle> mCachedCycles = Sets.newConcurrentHashSet();
  private final Cache<LocalDate, ChartEntry> mEntryCache = CacheBuilder.newBuilder()
      .maximumSize(MAX_CACHED_ENTRIES)
      .removalListener(new RemovalListener<LocalDate, ChartEntry>() {
        @Override
        public void onRemoval(RemovalNotification<LocalDate, ChartEntry> notification) {
          LocalDate date = notification.getKey();
          for (Cycle cycle : mCachedCycles) {
            if (dateInCycle(cycle, date)) {
              Log.i(TAG, "Removing cycle " + cycle.id + " from cache.");
              mCachedCycles.remove(cycle);
            }
          }
        }
      }).build();

  public Action fillCache(final Cycle cycle, final List<ChartEntry> entries) {
    return new Action() {
      @Override
      public void run() throws Exception {
        Log.i(TAG, "Filling " + cycle.id);
        for (ChartEntry entry : entries) {
          mEntryCache.put(entry.entryDate, entry);
        }
        mCachedCycles.add(cycle);
      }
    };
  }

  public List<ChartEntry> getEntries(Cycle cycle) {
    if (!mCachedCycles.contains(cycle)) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<ChartEntry> entries = ImmutableList.builder();
    LocalDate endDate = cycle.endDate != null ? cycle.endDate : LocalDate.now();
    for (LocalDate date = cycle.startDate;
         date.isBefore(endDate) || date.isEqual(endDate);
         date = date.plusDays(1)) {
      ChartEntry entry = mEntryCache.getIfPresent(date);
      if (entry != null) {
        entries.add(entry);
      }
    }
    return entries.build();
  }

  private static boolean dateInCycle(Cycle cycle, LocalDate date) {
    if (date.isBefore(cycle.endDate.plusDays(1)) && (
        cycle.endDate == null || date.isAfter(cycle.startDate.minusDays(1)))) {
      return true;
    }
    return false;
  }
}
