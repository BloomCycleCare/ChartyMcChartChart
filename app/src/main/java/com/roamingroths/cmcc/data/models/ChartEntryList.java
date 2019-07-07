package com.roamingroths.cmcc.data.models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.SortedList;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.domain.BasicInstruction;
import com.roamingroths.cmcc.data.domain.DischargeSummary;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.data.domain.SpecialInstruction;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.Entry;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.logic.chart.MccScorer;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryAdapter;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by parkeroth on 6/24/17.
 */

public class ChartEntryList {

  private final boolean DEBUG = false;
  private final String TAG = ChartEntryList.class.getSimpleName();

  public enum Stat {
    MCS, LPP
  }

  // Chart state members
  public final Cycle mCurrentCycle;
  private final SortedList<ChartEntry> mEntries;
  private final Map<LocalDate, ChartEntry> mEntryIndex = new HashMap<>();
  private final SortedSet<LocalDate> mPeakDays = new TreeSet<>();
  private final ReentrantReadWriteLock mInstructionsLock = new ReentrantReadWriteLock();
  private final TreeSet<Instructions> mInstructions = new TreeSet<>((a, b) -> a.startDate.compareTo(b.startDate));

  private LocalDate mPointOfChange;

  public static Builder builder(Cycle currentCycle) {
    return new Builder(currentCycle);
  }

  private ChartEntryList(Cycle currentCycle, SortedList<ChartEntry> entries) {
    mEntries = entries;
    mCurrentCycle = currentCycle;
  }

  public void updateInstructions(Collection<Instructions> newInstructions) {
    try {
      mInstructionsLock.writeLock().lock();
      mInstructions.clear();
      mInstructions.addAll(newInstructions);
    } finally {
      mInstructionsLock.writeLock().unlock();
    }
  }

  private Optional<Instructions> getActiveInstructions(LocalDate date) {
    try {
      mInstructionsLock.readLock().lock();
      for (Instructions instructions : mInstructions.descendingSet()) {
        if (date.isAfter(instructions.startDate)) {
          return Optional.of(instructions);
        }
      }
      return Optional.absent();
    } finally {
      mInstructionsLock.readLock().unlock();
    }
  }

  private boolean postPeakYellowEnabled(LocalDate date) {
    return getActiveInstructions(date).transform(instructions -> {
      for (BasicInstruction i : BasicInstruction.postPeakYellowBasicInstructions) {
        if (instructions.isActive(i)) {
          return true;
        }
      }
      return false;
    }).or(false);
  }

  public ImmutableMap<Stat, Object> getStats() {
    List<ChartEntry> entries = new ArrayList<>();
    Optional<LocalDate> firstPeakDay = mPeakDays.isEmpty() ? Optional.absent() : Optional.of(mPeakDays.first());
    int lengthOfPrepeakMucusCycle = 0;
    for (int i = 0; i < mEntries.size(); i++) {
      ChartEntry entry = mEntries.get(i);
      entries.add(entry);
      if (firstPeakDay.isPresent() && firstPeakDay.get().isAfter(entry.entryDate)
          && entry.observationEntry.hasMucus()) {
        lengthOfPrepeakMucusCycle++;
      }
    }
    return ImmutableMap.<Stat, Object>builder()
        .put(Stat.MCS, String.format("%.1f", MccScorer.getScore(entries, firstPeakDay)))
        .put(Stat.LPP, String.format("%d", lengthOfPrepeakMucusCycle))
        .build();
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

  public int size() {
    return mEntries.size();
  }

  public ChartEntry get(int index) {
    return mEntries.get(index);
  }

  private boolean expectUnusualBleeding(int index) {
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

  private boolean askEssentialSamenessQuestion(int index) {
    ChartEntry entry = get(index);
    // TODO: change wording of question
    boolean askForSpecialInstruction = false;
    if (isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS, entry.entryDate)) {
      int previousIndex = index + 1;
      if (previousIndex < mEntries.size()) {
        askForSpecialInstruction = mEntries.get(previousIndex).observationEntry.intercourse;
      }
    }
    boolean askForPrePeakYellow = false;
    if (isActive(BasicInstruction.K_1, entry.entryDate) && isPreakPeak(entry.observationEntry)) {
      askForPrePeakYellow = true;
    }
    return askForPrePeakYellow || askForSpecialInstruction;
  }

  private int getCount(ChartEntry currentEntry) {
    int entryIndex = mEntries.indexOf(currentEntry);
    if (currentEntry.observationEntry == null) {
      return -1;
    }
    if (isActive(BasicInstruction.K_1, currentEntry.entryDate)
        && isBeforePointOfChange(currentEntry.observationEntry)) {
      return -1;
    }
    if (currentEntry.observationEntry.unusualBleeding
        || currentEntry.observationEntry.peakDay
        || currentEntry.observationEntry.hasPeakTypeMucus()) {
      return 0;
    }
    int lastPositionToCheck = entryIndex + 3;
    for (int i = entryIndex + 1; i < mEntries.size() && i <= lastPositionToCheck; i++) {
      ChartEntry previousEntry = mEntries.get(i);
      if (previousEntry.observationEntry.observation == null) {
        continue;
      }
      // Check if any unusual bleeding within count of three (D.6)
      if (previousEntry.observationEntry.unusualBleeding) {
        return i - entryIndex;
      }
      if (previousEntry.observationEntry.observation.dischargeSummary == null) {
        continue;
      }
      if (previousEntry.observationEntry.peakDay) {
        return i - entryIndex;
      }
      // Check for 1 day of peak mucus (D.5)
      if (previousEntry.observationEntry.observation.dischargeSummary.isPeakType()) {
        if (isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS, currentEntry.entryDate)
            && previousEntry.observationEntry.isEssentiallyTheSame) {
          continue;
        } else {
          return i - entryIndex;
        }
      }
      if (previousEntry.observationEntry.observation.dischargeSummary.mType.hasMucus()
          && isPreakPeak(currentEntry.observationEntry)) {
        // Check for 3 consecutive days of non-peak mucus pre peak (D.4)
        int lastPositionInWindow = i + 3;
        int consecutiveDays = 0;
        for (int j = i; j < mEntries.size() && j < lastPositionInWindow; j++) {
          if (mEntries.get(j).observationEntry.hasMucus()) {
            consecutiveDays++;
          } else {
            consecutiveDays = 0;
          }
        }
        if (consecutiveDays == 3) {
          return i - entryIndex;
        }
      }
    }
    return -1;
  }

  private boolean isActive(SpecialInstruction specialInstruction, LocalDate date) {
    return getActiveInstructions(date).transform(i -> i.isActive(specialInstruction)).or(false);
  }

  private boolean isActive(BasicInstruction basicInstruction, LocalDate date) {
    return getActiveInstructions(date).transform(i -> i.isActive(basicInstruction)).or(false);
  }

  private int getPosition(Entry entry) {
    if (!mEntryIndex.containsKey(entry.getDate())) {
      return -1;
    }
    return mEntries.indexOf(mEntryIndex.get(entry.getDate()));
  }

  private boolean isWithinCountOfThree(ObservationEntry entry) {
    int position = getPosition(entry);
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
      // Check for 1 day of peak mucus (D.5)
      if (previousEntry.observationEntry.observation.dischargeSummary.isPeakType()) {
        if (isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS, entry.getDate())
            && previousEntry.observationEntry.isEssentiallyTheSame) {
          continue;
        } else {
          return true;
        }
      }
      if (previousEntry.observationEntry.observation.dischargeSummary.mType.hasMucus() && isPreakPeak(entry)) {
        // Check for 3 consecutive days of non-peak mucus pre peak (D.4)
        int lastNonPeakMucus = i + 3;
        boolean consecutiveNonPeakMucus = true;
        for (int j = i; j < mEntries.size() && j < lastNonPeakMucus; j++) {
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

  public boolean shouldShowBaby(ChartEntry entry) {
    if (entry.observationEntry == null) {
      return false;
    }
    int position = getEntryIndex(entry.entryDate);
    return shouldShowBaby(position, entry.observationEntry);
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
    if (isActive(BasicInstruction.K_1, entry.getDate())
        && isPreakPeak(entry) && isBeforePointOfChange(entry)) {
      return false;
    }
    // Suppress for special instruction
    if (isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS, entry.getDate())
        && entry.observation.dischargeSummary.isPeakType() && entry.isEssentiallyTheSame) {
      return false;
    }
    if (isWithinCountOfThree(entry)) {
      return true;
    }
    // TODO: migrate to use getCount
    LocalDate mostRecentPeakDay = getMostRecentPeakDay(entry);
    if (mostRecentPeakDay != null) {
      if (mostRecentPeakDay.minusDays(1).isBefore(entry.getDate())
          && mostRecentPeakDay.plusDays(4).isAfter(entry.getDate())) {
        return true;
      }
      if (postPeakYellowEnabled(entry.getDate())
          && entry.getDate().isAfter(mostRecentPeakDay.plusDays(3))) {
        return false;
      }
    }
    return entry.observation != null && entry.observation.hasMucus();
  }

  public String getPeakDayViewText(ChartEntry entry) {
    if (entry == null || entry.observationEntry == null || entry.observationEntry.observation == null) {
      return "";
    }
    if (entry.observationEntry.peakDay) {
      return "P";
    }
    int count = getCount(entry);
    if (count > 0) {
      return String.valueOf(count);
    } else {
      return "";
    }
  }

  public int getEntryColorResource(ObservationEntry entry) {
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
    if (isActive(BasicInstruction.K_1, entry.getDate())) {
      // Prepeak yellow stickers enabled
      if (isPreakPeak(entry) && isBeforePointOfChange(entry)) {
        return R.color.entryYellow;
      }
    }
    if (postPeakYellowEnabled(entry.getDate())) {
      boolean isPostPeakPlus4 = Optional
          .fromNullable(getMostRecentPeakDay(entry))
          .transform(mostRecentPeakDay -> entry.getDate().isAfter(mostRecentPeakDay.plusDays(3)))
          .or(false);
      // Postpeak yellow stickers enabled
      if (isPostPeakPlus4) {
        return R.color.entryYellow;
      }
    }
    if (isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS, entry.getDate())
        && entry.observation.dischargeSummary.isPeakType()
        && entry.isEssentiallyTheSame) {
      return R.color.entryYellow;
    }
    return R.color.entryWhite;
  }

  public Set<String> isUsableDay(ObservationEntry entry) {
    Instructions instructions = getActiveInstructions(entry.getDate()).orNull();
    if (instructions == null) {
      return ImmutableSet.of("IDK, no active instructions...");
    }
    // TODO: finish
    return ImmutableSet.of();
  }

  private Set<BasicInstruction> getFertilityReasons(@NonNull Instructions instructions, ObservationEntry entry) {
    ImmutableSet.Builder<BasicInstruction> fertilityReasons = ImmutableSet.builder();
    if (instructions.isActive(BasicInstruction.D_1)
        && isInMenstrualFlow(entry)) {
      fertilityReasons.add(BasicInstruction.D_1);
    }
    if (instructions.isActive(BasicInstruction.D_2)
        && entry.getDate().isBefore(mPeakDays.last().plusDays(4))
        && anyMucusOnOrBefore(entry.getDate())) {
      fertilityReasons.add(BasicInstruction.D_2);
    }
    if (instructions.isActive(BasicInstruction.D_3)
        && entry.observation != null && entry.observation.hasMucus()) {
      fertilityReasons.add(BasicInstruction.D_3);
    }
    return fertilityReasons.build();
  }

  private boolean anyMucusOnOrBefore(LocalDate date) {
    for (int i=mEntries.size()-1; i <=0; i++) {
      ChartEntry previousEntry = mEntries.get(i);
      if (previousEntry.entryDate.isAfter(date)) {
        break;
      }
      if (previousEntry.observationEntry.observation == null) {
        return false;
      }
      if (previousEntry.observationEntry.observation.hasMucus()) {
        return true;
      }
    }
    return false;
  }

  private boolean isInMenstrualFlow(ObservationEntry entry) {
    for (int i=mEntries.size()-1; i <=0; i++) {
      ChartEntry previousEntry = mEntries.get(i);
      if (previousEntry.entryDate.isAfter(entry.getDate())) {
        break;
      }
      if (previousEntry.observationEntry.observation == null) {
        return false;
      }
      Observation previousObservation = previousEntry.observationEntry.observation;
      if (previousObservation.flow == null) {
        return false;
      }
    }
    return true;
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

    private final Cycle currentCycle;
    private final SortedList<ChartEntry> entries;
    private ChartEntryAdapter adapter;

    private Builder(Cycle currentCycle) {
      this.currentCycle = Preconditions.checkNotNull(currentCycle);

      entries = new SortedList<>(ChartEntry.class, new SortedList.Callback<ChartEntry>() {
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
    }

    public Builder withAdapter(ChartEntryAdapter adapter) {
      this.adapter = adapter;
      return this;
    }

    public Builder addAll(Collection<ChartEntry> chartEntries) {
      entries.addAll(chartEntries);
      return this;
    }

    public ChartEntryList build() {
      return new ChartEntryList(currentCycle, entries);
    }
  }

  private void logV(String message) {
    Log.v("ChartEntryList", message);
  }
}
