package com.roamingroths.cmcc.data;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.ChartEntryViewHolder;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.DischargeSummary;
import com.roamingroths.cmcc.logic.EntryContainer;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by parkeroth on 6/24/17.
 */

public class EntryContainerList {

  private final boolean DEBUG = true;
  private final String TAG = EntryContainerList.class.getSimpleName();
  private final AtomicBoolean mInitialized = new AtomicBoolean(false);

  // Chart state members
  public final Cycle mCycle;
  private final SortedList<EntryContainer> mEntries;
  private final Map<LocalDate, EntryContainer> mEntryIndex = new HashMap<>();
  private final SortedSet<LocalDate> mPeakDays = new TreeSet<>();
  private final Preferences mPreferences;
  private LocalDate mPointOfChange;

  public static Builder builder(Cycle cycle, Preferences preferences) {
    return new Builder(cycle, preferences);
  }

  private EntryContainerList(Cycle cycle, Preferences preferences, SortedList<EntryContainer> entries) {
    mEntries = entries;
    mCycle = cycle;
    mPreferences = preferences;
  }

  public void bindViewHolder(ChartEntryViewHolder holder, int position, Context context) {
    ChartEntry entry = mEntries.get(position).chartEntry;
    holder.setEntrySummary(entry.getListUiText());
    holder.setBackgroundColor(getEntryColorResource(entry, context));
    holder.setEntryNum(mEntries.size() - position);
    holder.setDate(DateUtil.toWireStr(entry.getDate()));
    holder.setPeakDayText(getPeakDayViewText(entry));
    holder.setIntercourse(entry.intercourse);
    holder.setShowBaby(shouldShowBaby(position, entry));
  }

  public void initialize(final CycleProvider cycleProvider, final Callbacks.Callback<Void> doneCallback) {
    if (DEBUG) Log.v(TAG, "Initialize");
    if (mInitialized.compareAndSet(false, true)) {
      cycleProvider.maybeCreateNewEntries(mCycle, new Callbacks.ErrorForwardingCallback<Void>(doneCallback) {
        @Override
        public void acceptData(Void data) {
          fillFromProvider(cycleProvider.getChartEntryProvider(), doneCallback);
        }
      });
    } else {
      doneCallback.handleError(DatabaseError.fromException(new IllegalStateException()));
    }
  }

  public synchronized void addEntry(EntryContainer entryContainer) {
    if (mEntryIndex.containsKey(entryContainer.entryDate)) {
      changeEntry(entryContainer);
      return;
    }
    // Maybe add peak day to set
    if (entryContainer.chartEntry.peakDay) {
      mPeakDays.add(entryContainer.entryDate);
    }
    // Maybe set point of change
    if (entryContainer.chartEntry.pointOfChange) {
      setPointOfChange(entryContainer.entryDate);
    }
    // Add entry to list
    mEntries.add(entryContainer);
    mEntryIndex.put(entryContainer.entryDate, entryContainer);
    return;
  }

  private void setPointOfChange(LocalDate date) {
    if (mPointOfChange != null && !mPointOfChange.equals(date)) {
      throw new IllegalStateException("Cannot have two points of change!");
    }
    mPointOfChange = date;
  }

  public synchronized void changeEntry(EntryContainer entryContainer) {
    // Maybe add or remove from peak day set
    if (mEntryIndex.containsKey(entryContainer.entryDate)
        && mEntryIndex.get(entryContainer.entryDate).equals(entryContainer)) {
      return;
    }
    if (entryContainer.chartEntry.peakDay) {
      mPeakDays.add(entryContainer.entryDate);
    } else {
      mPeakDays.remove(entryContainer.entryDate);
    }
    if (entryContainer.chartEntry.pointOfChange) {
      setPointOfChange(entryContainer.entryDate);
    } else {
      mPointOfChange = null;
    }
    int entryIndex = getEntryIndex(entryContainer.entryDate);
    if (entryIndex < 0) {
      Log.w("ChartEntryList", "No entry to update for: " + entryContainer.entryDate + ", adding instead.");
    } else {
      mEntries.updateItemAt(entryIndex, entryContainer);
    }
  }

  public void removeEntry(String entryDateStr) {
    removeEntry(findEntry(entryDateStr));
  }

  public synchronized void removeEntry(EntryContainer entryContainer) {
    if (entryContainer.chartEntry.pointOfChange) {
      mPointOfChange = null;
    }
    // Maybe remove peak day from set
    mEntryIndex.remove(entryContainer.entryDate);
    mPeakDays.remove(entryContainer.entryDate);
    mEntries.remove(entryContainer);
  }

  @Nullable
  public EntryContainer findEntry(String dateStr) {
    int index = getEntryIndex(DateUtil.fromWireStr(dateStr));
    if (index < 0) {
      return null;
    }
    return get(index);
  }

  public int size() {
    return mEntries.size();
  }

  public EntryContainer get(int index) {
    return mEntries.get(index);
  }

  public boolean expectUnusualBleeding(int index) {
    int previousIndex = index + 1;
    if (previousIndex >= mEntries.size()) {
      return false;
    }
    EntryContainer previousEntry = mEntries.get(previousIndex);
    if (previousEntry.chartEntry.unusualBleeding) {
      return true;
    }
    return !(previousEntry.chartEntry.observation != null
        && previousEntry.chartEntry.observation.hasBlood());
  }

  private boolean isWithinCountOfThree(int position, ChartEntry entry) {
    int lastPosition = position + 3;
    for (int i = position + 1; i < mEntries.size() && i <= lastPosition; i++) {
      EntryContainer previousEntry = mEntries.get(i);
      if (previousEntry.chartEntry.observation == null) {
        continue;
      }
      // Check if any unusual bleeding within count of three (D.6)
      if (previousEntry.chartEntry.unusualBleeding) {
        return true;
      }
      if (previousEntry.chartEntry.observation.dischargeSummary == null) {
        continue;
      }
      if (previousEntry.chartEntry.observation.dischargeSummary.isPeakType()) {
        // Check for 1 day of peak mucus (D.5)
        return true;
      }
      if (previousEntry.chartEntry.observation.dischargeSummary.mType.hasMucus() && isPreakPeak(entry)) {
        // Check for 3 consecutive days of non-peak mucus pre peak (D.4)
        int lastNonPeakMucus = i + 3;
        boolean consecutiveNonPeakMucus = true;
        for (int j = i; i < mEntries.size() && j < lastNonPeakMucus; j++) {
          if (!mEntries.get(j).chartEntry.hasMucus()) {
            consecutiveNonPeakMucus = false;
            break;
          }
        }
        return consecutiveNonPeakMucus;
      }
    }
    return false;
  }

  private boolean shouldShowBaby(int position, ChartEntry entry) {
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

  private String getPeakDayViewText(ChartEntry entry) {
    if (entry == null || entry.observation == null) {
      return "";
    }
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    if (mostRecentPeakDay == null) {
      return "";
    }
    return getPeakDayViewText(entry, mostRecentPeakDay);
  }

  public int getEntryColorResource(ChartEntry entry, Context context) {
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

  private boolean isPostPeak(ChartEntry entry) {
    Preconditions.checkNotNull(entry);
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    return mostRecentPeakDay != null && mostRecentPeakDay.isBefore(entry.getDate());
  }

  private boolean isPreakPeak(ChartEntry entry) {
    Preconditions.checkNotNull(entry);
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    return mostRecentPeakDay == null || mostRecentPeakDay.isAfter(entry.getDate());
  }

  private boolean isBeforePointOfChange(ChartEntry entry) {
    Preconditions.checkNotNull(entry);
    return mPointOfChange == null || mPointOfChange.isAfter(entry.getDate());
  }

  @Nullable
  private LocalDate getMostRecentPeakDay(ChartEntry entry) {
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

  private String getPeakDayViewText(ChartEntry entry, LocalDate peakDay) {
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
      EntryContainer entryContainer = mEntries.get(i);
      if (entryContainer.entryDate.equals(entryDate)) {
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

    public EntryContainerList build() {
      SortedList<EntryContainer> entries = new SortedList<EntryContainer>(EntryContainer.class, new SortedList.Callback<EntryContainer>() {
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
        public int compare(EntryContainer e1, EntryContainer e2) {
          return e2.entryDate.compareTo(e1.entryDate);
        }

        @Override
        public boolean areContentsTheSame(EntryContainer oldItem, EntryContainer newItem) {
          return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(EntryContainer item1, EntryContainer item2) {
          return item1 == item2;
        }
      });
      return new EntryContainerList(cycle, preferences, entries);
    }
  }

  private void fillFromProvider(
      ChartEntryProvider chartEntryProvider,
      final Callbacks.Callback<Void> doneCallback) {
    logV("Begin filling from DB");
    chartEntryProvider.getDecryptedEntries(mCycle, new Callbacks.ErrorForwardingCallback<Map<LocalDate, ChartEntry>>(doneCallback) {
      @Override
      public void acceptData(Map<LocalDate, ChartEntry> entries) {
        for (Map.Entry<LocalDate, ChartEntry> entry : entries.entrySet()) {
          addEntry(new EntryContainer(entry.getKey(), entry.getValue()));
        }
        doneCallback.acceptData(null);
      }
    });
  }

  private void logV(String message) {
    Log.v("EntryContainerList", message);
  }
}
