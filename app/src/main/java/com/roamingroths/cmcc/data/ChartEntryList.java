package com.roamingroths.cmcc.data;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.DischargeSummary;
import com.roamingroths.cmcc.logic.ObservationEntry;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryViewHolder;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 6/24/17.
 */

public class ChartEntryList {

  private final boolean DEBUG = true;
  private final String TAG = ChartEntryList.class.getSimpleName();
  private final AtomicBoolean mInitialized = new AtomicBoolean(false);

  // Chart state members
  public final Cycle mCycle;
  private final SortedList<ChartEntry> mEntries;
  private final Map<LocalDate, ChartEntry> mEntryIndex = new HashMap<>();
  private final SortedSet<LocalDate> mPeakDays = new TreeSet<>();
  private final Preferences mPreferences;
  private LocalDate mPointOfChange;

  public static Builder builder(Cycle cycle, Preferences preferences) {
    return new Builder(cycle, preferences);
  }

  private ChartEntryList(Cycle cycle, Preferences preferences, SortedList<ChartEntry> entries) {
    mEntries = entries;
    mCycle = cycle;
    mPreferences = preferences;
  }

  public void bindViewHolder(ChartEntryViewHolder holder, int position, Context context) {
    if (DEBUG) Log.v(TAG, "bindViewHolder(" + position + ")");
    ObservationEntry entry = mEntries.get(position).observationEntry;
    holder.setEntrySummary(entry.getListUiText());
    holder.setBackgroundColor(getEntryColorResource(entry, context));
    holder.setEntryNum(mEntries.size() - position);
    holder.setDate(DateUtil.toWireStr(entry.getDate()));
    holder.setPeakDayText(getPeakDayViewText(entry));
    holder.setIntercourse(entry.intercourse);
    holder.setShowBaby(shouldShowBaby(position, entry));
  }

  public Completable initialize(final CycleProvider cycleProvider) {
    if (DEBUG) Log.v(TAG, "Initialize");
    if (mInitialized.compareAndSet(false, true)) {
      return cycleProvider.maybeCreateNewEntries(mCycle)
          .andThen(fillFromProvider(cycleProvider));
    } else {
      return Completable.error(new IllegalStateException());
    }
  }

  public synchronized void addEntry(ChartEntry chartEntry) {
    if (mEntryIndex.containsKey(chartEntry.entryDate)) {
      changeEntry(chartEntry);
      return;
    }
    // Maybe add peak day to set
    if (chartEntry.observationEntry.peakDay) {
      mPeakDays.add(chartEntry.entryDate);
    }
    // Maybe set point of change
    if (chartEntry.observationEntry.pointOfChange) {
      setPointOfChange(chartEntry.entryDate);
    }
    // Add entry to list
    mEntries.add(chartEntry);
    mEntryIndex.put(chartEntry.entryDate, chartEntry);
    return;
  }

  private void setPointOfChange(LocalDate date) {
    if (mPointOfChange != null && !mPointOfChange.equals(date)) {
      throw new IllegalStateException("Cannot have two points of change!");
    }
    mPointOfChange = date;
  }

  public synchronized void changeEntry(ChartEntry chartEntry) {
    // Maybe add or remove from peak day set
    if (mEntryIndex.containsKey(chartEntry.entryDate)
        && mEntryIndex.get(chartEntry.entryDate).equals(chartEntry)) {
      return;
    }
    if (chartEntry.observationEntry.peakDay) {
      mPeakDays.add(chartEntry.entryDate);
    } else {
      mPeakDays.remove(chartEntry.entryDate);
    }
    if (chartEntry.observationEntry.pointOfChange) {
      setPointOfChange(chartEntry.entryDate);
    } else {
      mPointOfChange = null;
    }
    int entryIndex = getEntryIndex(chartEntry.entryDate);
    if (entryIndex < 0) {
      Log.w("ChartEntryList", "No entry to update for: " + chartEntry.entryDate + ", adding instead.");
    } else {
      mEntries.updateItemAt(entryIndex, chartEntry);
    }
  }

  public void removeEntry(String entryDateStr) {
    removeEntry(findEntry(entryDateStr));
  }

  public synchronized void removeEntry(ChartEntry chartEntry) {
    if (chartEntry.observationEntry.pointOfChange) {
      mPointOfChange = null;
    }
    // Maybe remove peak day from set
    mEntryIndex.remove(chartEntry.entryDate);
    mPeakDays.remove(chartEntry.entryDate);
    mEntries.remove(chartEntry);
  }

  @Nullable
  public ChartEntry findEntry(String dateStr) {
    int index = getEntryIndex(DateUtil.fromWireStr(dateStr));
    if (index < 0) {
      return null;
    }
    return get(index);
  }

  public int size() {
    return mEntries.size();
  }

  public ChartEntry get(int index) {
    return mEntries.get(index);
  }

  public boolean expectUnusualBleeding(int index) {
    int previousIndex = index + 1;
    if (previousIndex >= mEntries.size()) {
      return false;
    }
    ChartEntry previousEntry = mEntries.get(previousIndex);
    if (previousEntry.observationEntry.unusualBleeding) {
      return true;
    }
    return !(previousEntry.observationEntry.observation != null
        && previousEntry.observationEntry.observation.hasBlood());
  }

  private boolean isWithinCountOfThree(int position, ObservationEntry entry) {
    int lastPosition = position + 3;
    for (int i = position + 1; i < mEntries.size() && i <= lastPosition; i++) {
      ChartEntry previousEntry = mEntries.get(i);
      if (previousEntry.observationEntry.observation == null) {
        continue;
      }
      // Check if any unusual bleeding within count of three (D.6)
      if (previousEntry.observationEntry.unusualBleeding) {
        return true;
      }
      if (previousEntry.observationEntry.observation.dischargeSummary == null) {
        continue;
      }
      if (previousEntry.observationEntry.observation.dischargeSummary.isPeakType()) {
        // Check for 1 day of peak mucus (D.5)
        return true;
      }
      if (previousEntry.observationEntry.observation.dischargeSummary.mType.hasMucus() && isPreakPeak(entry)) {
        // Check for 3 consecutive days of non-peak mucus pre peak (D.4)
        int lastNonPeakMucus = i + 3;
        boolean consecutiveNonPeakMucus = true;
        for (int j = i; i < mEntries.size() && j < lastNonPeakMucus; j++) {
          if (!mEntries.get(j).observationEntry.hasMucus()) {
            consecutiveNonPeakMucus = false;
            break;
          }
        }
        return consecutiveNonPeakMucus;
      }
    }
    return false;
  }

  private boolean shouldShowBaby(int position, ObservationEntry entry) {
    if (entry == null || entry.observation == null) {
      return false;
    }
    // Check for a red sticker
    if (entry.observation.hasBlood()) {
      return false;
    }
    // Suppress if prepeak and yellow stickers enabled
    if (mPreferences.prePeakYellowEnabled() && isPreakPeak(entry) && isBeforePointOfChange(entry)) {
      return false;
    }
    if (isWithinCountOfThree(position, entry)) {
      return true;
    }
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    if (mostRecentPeakDay != null) {
      if (mostRecentPeakDay.minusDays(1).isBefore(entry.getDate())
          && mostRecentPeakDay.plusDays(4).isAfter(entry.getDate())) {
        return true;
      }
      if (mPreferences.postPeakYellowEnabled()
          && entry.getDate().isAfter(mostRecentPeakDay.plusDays(3))) {
        return false;
      }
    }
    return entry.observation != null && entry.observation.hasMucus();
  }

  private String getPeakDayViewText(ObservationEntry entry) {
    if (entry == null || entry.observation == null) {
      return "";
    }
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    if (mostRecentPeakDay == null) {
      return "";
    }
    return getPeakDayViewText(entry, mostRecentPeakDay);
  }

  public int getEntryColorResource(ObservationEntry entry, Context context) {
    if (entry.observation == null) {
      return R.color.entryGrey;
    }
    if (entry.observation.flow != null) {
      return R.color.entryRed;
    }
    if (entry.observation.dischargeSummary.mModifiers.contains(DischargeSummary.MucusModifier.B)) {
      return R.color.entryRed;
    }
    if (!entry.observation.dischargeSummary.mType.hasMucus()) {
      return R.color.entryGreen;
    }
    if (mPreferences.prePeakYellowEnabled()) {
      // Prepeak yellow stickers enabled
      if (isPreakPeak(entry) && isBeforePointOfChange(entry)) {
        return R.color.entryYellow;
      }
    }
    if (mPreferences.postPeakYellowEnabled()) {
      // Postpeak yellow stickers enabled
      if (isPostPeak(entry)) {
        return R.color.entryYellow;
      }
    }
    return R.color.entryWhite;
  }

  private boolean isPostPeak(ObservationEntry entry) {
    Preconditions.checkNotNull(entry);
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    return mostRecentPeakDay != null && mostRecentPeakDay.isBefore(entry.getDate());
  }

  private boolean isPreakPeak(ObservationEntry entry) {
    Preconditions.checkNotNull(entry);
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    return mostRecentPeakDay == null || mostRecentPeakDay.isAfter(entry.getDate());
  }

  private boolean isBeforePointOfChange(ObservationEntry entry) {
    Preconditions.checkNotNull(entry);
    return mPointOfChange == null || mPointOfChange.isAfter(entry.getDate());
  }

  @Nullable
  private LocalDate getMostRecentPeakDay(ObservationEntry entry) {
    LocalDate mostRecentPeakDay = null;
    for (LocalDate peakDay : mPeakDays) {
      if (peakDay.isAfter(entry.getDate())) {
        continue;
      }
      if (mostRecentPeakDay == null) {
        mostRecentPeakDay = peakDay;
      }
      if (mostRecentPeakDay.isBefore(peakDay)) {
        mostRecentPeakDay = peakDay;
      }
    }
    return mostRecentPeakDay;
  }

  private String getPeakDayViewText(ObservationEntry entry, LocalDate peakDay) {
    if (entry.getDate().isBefore(peakDay)) {
      return "";
    }
    if (entry.getDate().equals(peakDay)) {
      return "P";
    }
    int daysAfterPeak = Days.daysBetween(peakDay, entry.getDate()).getDays();
    if (daysAfterPeak < 4) {
      return String.valueOf(daysAfterPeak);
    }
    return "";
  }

  private int getEntryIndex(LocalDate entryDate) {
    for (int i = 0; i < mEntries.size(); i++) {
      ChartEntry chartEntry = mEntries.get(i);
      if (chartEntry.entryDate.equals(entryDate)) {
        return i;
      }
    }
    return -1;
  }

  public static class Builder {

    private final Cycle cycle;
    private final Preferences preferences;
    private ChartEntryAdapter adapter;

    private Builder(Cycle cycle, Preferences preferences) {
      this.cycle = Preconditions.checkNotNull(cycle);
      this.preferences = Preconditions.checkNotNull(preferences);
    }

    public Builder withAdapter(ChartEntryAdapter adapter) {
      this.adapter = adapter;
      return this;
    }

    public ChartEntryList build() {
      SortedList<ChartEntry> entries = new SortedList<ChartEntry>(ChartEntry.class, new SortedList.Callback<ChartEntry>() {
        @Override
        public void onInserted(int position, int count) {
          if (adapter != null) {
            adapter.notifyItemRangeInserted(position, count);
          }
        }

        @Override
        public void onRemoved(int position, int count) {
          if (adapter != null) {
            adapter.notifyItemRangeRemoved(position, count);
          }
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
          if (adapter != null) {
            adapter.notifyItemMoved(fromPosition, toPosition);
          }
        }

        @Override
        public void onChanged(int position, int count) {
          if (adapter != null) {
            adapter.notifyItemRangeChanged(position, count);
          }
        }

        @Override
        public int compare(ChartEntry e1, ChartEntry e2) {
          return e2.entryDate.compareTo(e1.entryDate);
        }

        @Override
        public boolean areContentsTheSame(ChartEntry oldItem, ChartEntry newItem) {
          return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(ChartEntry item1, ChartEntry item2) {
          return item1 == item2;
        }
      });
      return new ChartEntryList(cycle, preferences, entries);
    }
  }

  private void fillFromIntent(Intent intent) {
    List<ChartEntry> containers = intent.getParcelableArrayListExtra(ChartEntry.class.getName());
    for (ChartEntry container : containers) {
      addEntry(container);
    }
  }

  private Completable fillFromProvider(CycleProvider cycleProvider) {
    logV("Begin filling from DB");
    return cycleProvider.getEntries(mCycle)
        .observeOn(AndroidSchedulers.mainThread())
        .flatMapCompletable(new Function<ChartEntry, CompletableSource>() {
          @Override
          public CompletableSource apply(final ChartEntry chartEntry) throws Exception {
            return Completable.fromAction(new Action() {
              @Override
              public void run() throws Exception {
                addEntry(chartEntry);
              }
            });
          }
        });
  }

  private void logV(String message) {
    Log.v("ChartEntryList", message);
  }
}
