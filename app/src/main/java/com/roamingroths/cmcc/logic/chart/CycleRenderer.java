package com.roamingroths.cmcc.logic.chart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.roamingroths.cmcc.data.domain.BasicInstruction;
import com.roamingroths.cmcc.data.domain.DischargeSummary;
import com.roamingroths.cmcc.data.domain.IntercourseTimeOfDay;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.data.domain.SpecialInstruction;
import com.roamingroths.cmcc.data.domain.YellowStampInstruction;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CycleRenderer {

  private final Cycle mCycle;
  private final TreeSet<ChartEntry> mEntries;
  private final TreeSet<Instructions> mInstructions;

  public CycleRenderer(Cycle cycle, Collection<ChartEntry> entries, Collection<Instructions> allInstructions) {
    mCycle = cycle;
    mEntries = new TreeSet<>((a, b) -> a.entryDate.compareTo(b.entryDate));
    mEntries.addAll(entries);
    mInstructions = new TreeSet<>((a, b) -> a.startDate.compareTo(b.startDate));
    mInstructions.addAll(allInstructions);
  }

  public static class State {
    public Cycle cycle;
    public ChartEntry entry;
    public Instructions instructions;
    public int entryNum;
    public boolean isPrePeak;
    public boolean isPeakDay;
    public boolean isPostPeak;
    public int daysPostPeak = -1;
    public boolean isInMenstrualFlow;
    public boolean isPointOfChange;
    public boolean isBeforePointOfChange;
    public Map<Enum, CountOfThreeReason> countOfThreeReasons;
    public ChartEntry previousEntry;
  }

  public List<RenderableEntry> render() {
    return render(getStates());
  }

  public List<State> getStates() {
    List<ChartEntry> entriesEvaluated = new ArrayList<>();
    Set<LocalDate> daysOfFlow = new HashSet<>();
    TreeSet<LocalDate> daysOfUnusualBleeding = new TreeSet<>();
    TreeSet<LocalDate> peakDays = new TreeSet<>();
    TreeSet<LocalDate> pointsOfChange = new TreeSet<>();
    LocalDate mostRecentPeakTypeMucus = null;
    LocalDate lastDayOfThreeOrMoreDaysOfMucus = null;
    int consecutiveDaysOfMucus = 0;
    ChartEntry previousEntry = null;
    boolean yesterdayWasFertile = false;
    boolean countOfThreeStarted = false;

    List<State> outStates = new ArrayList<>(mEntries.size());
    // For every day before the current entry...
    for (ChartEntry e : mEntries) {
      entriesEvaluated.add(e);

      Instructions instructions = null;
      for (Instructions i : mInstructions.descendingSet()) {
        if (!e.entryDate.isBefore(i.startDate)) {
          instructions = i;
          break;
        }
      }

      Set<BasicInstruction> fertilityReasons = new HashSet<>();
      if (e.observationEntry.peakDay) {
        peakDays.add(e.entryDate);
      }
      if (e.observationEntry.pointOfChange) {
        pointsOfChange.add(e.entryDate);
      }
      if (e.observationEntry.unusualBleeding) {
        daysOfUnusualBleeding.add(e.entryDate);
        fertilityReasons.add(BasicInstruction.D_6);
      }
      if (e.observationEntry.observation == null) {
        consecutiveDaysOfMucus = 0;
      } else {
        Observation observation = e.observationEntry.observation;
        if (observation.flow != null) {
          daysOfFlow.add(e.entryDate);
        }
        if (observation.hasMucus()) {
          consecutiveDaysOfMucus++;
          if (consecutiveDaysOfMucus >= 3) {
            lastDayOfThreeOrMoreDaysOfMucus = e.entryDate;
            fertilityReasons.add(BasicInstruction.D_4);
          } else {
            fertilityReasons.add(BasicInstruction.D_3);
          }
          if (observation.dischargeSummary.isPeakType()) {
            mostRecentPeakTypeMucus = e.entryDate;
            fertilityReasons.add(BasicInstruction.D_5);
          }
        } else {
          consecutiveDaysOfMucus = 0;
        }
      }

      boolean isInMenstrualFlow = entriesEvaluated.size() == daysOfFlow.size();
      if (isInMenstrualFlow) {
        fertilityReasons.add(BasicInstruction.D_1);
      }

      State state = new State();
      state.cycle = mCycle;
      state.entry = e;
      state.instructions = instructions;
      state.entryNum = Days.daysBetween(mCycle.startDate, e.entryDate).getDays() + 1;
      state.isPeakDay = e.observationEntry.peakDay;
      state.isPrePeak = !state.isPeakDay && peakDays.isEmpty();
      state.isPostPeak = !state.isPeakDay && !peakDays.isEmpty();
      state.isInMenstrualFlow = isInMenstrualFlow;
      state.isPointOfChange = !pointsOfChange.isEmpty() && pointsOfChange.last().equals(e.entryDate);
      state.isBeforePointOfChange = pointsOfChange.isEmpty() || e.entryDate.isBefore(pointsOfChange.first());
      state.previousEntry = previousEntry;
      state.countOfThreeReasons = new HashMap<>();

      LocalDate threeDaysAgo = e.entryDate.minusDays(3);
      Optional<LocalDate> mostRecentPeakDay = getMostRecent(peakDays, e.entryDate);
      if (state.instructions.isActive(BasicInstruction.D_2)
          && mostRecentPeakDay.isPresent()
          && !mostRecentPeakDay.get().isBefore(threeDaysAgo)) {
        state.countOfThreeReasons.put(BasicInstruction.D_2, new CountOfThreeReason(
            BasicInstruction.D_2,
            Days.daysBetween(mostRecentPeakDay.get(), e.entryDate).getDays()));
      }
      if (mostRecentPeakDay.isPresent()) {
        state.daysPostPeak = Days.daysBetween(mostRecentPeakDay.get(), e.entryDate).getDays();
      }
      Optional<LocalDate> mostRecentPointOfChange = getMostRecent(pointsOfChange, e.entryDate);
      if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
          && mostRecentPointOfChange.isPresent()
          && !mostRecentPointOfChange.get().equals(e.entryDate)
          && !mostRecentPointOfChange.get().isBefore(threeDaysAgo)) {
        state.countOfThreeReasons.put(YellowStampInstruction.YS_1_C, new CountOfThreeReason(
            YellowStampInstruction.YS_1_C,
            Days.daysBetween(mostRecentPointOfChange.get(), e.entryDate).getDays()));
      }

      // Check count of three conditions...
      boolean endOfFertitilityStretch = yesterdayWasFertile && fertilityReasons.isEmpty();
      if (endOfFertitilityStretch || countOfThreeStarted) {
        if (instructions.isActive(BasicInstruction.D_5)
            && mostRecentPeakTypeMucus != null
            && !mostRecentPeakTypeMucus.isBefore(threeDaysAgo)) {
          state.countOfThreeReasons.put(BasicInstruction.D_5, new CountOfThreeReason(
              BasicInstruction.D_5,
              Days.daysBetween(mostRecentPeakTypeMucus, e.entryDate).getDays()));
        }
        if (instructions.isActive(BasicInstruction.D_4)
            && state.isPrePeak
            && lastDayOfThreeOrMoreDaysOfMucus != null
            && !lastDayOfThreeOrMoreDaysOfMucus.equals(e.entryDate)
            && !lastDayOfThreeOrMoreDaysOfMucus.isBefore(threeDaysAgo)) {
          state.countOfThreeReasons.put(BasicInstruction.D_4, new CountOfThreeReason(
              BasicInstruction.D_4,
              Days.daysBetween(lastDayOfThreeOrMoreDaysOfMucus, e.entryDate).getDays()));
        }
        Optional<LocalDate> mostRecentUnusualBleeding = getMostRecent(daysOfUnusualBleeding, e.entryDate);
        if (instructions.isActive(BasicInstruction.D_6)
            && mostRecentUnusualBleeding.isPresent()
            && !mostRecentUnusualBleeding.get().equals(e.entryDate)
            && !mostRecentUnusualBleeding.get().isBefore(threeDaysAgo)) {
          state.countOfThreeReasons.put(BasicInstruction.D_6, new CountOfThreeReason(
              BasicInstruction.D_6,
              Days.daysBetween(mostRecentUnusualBleeding.get(), e.entryDate).getDays()));
        }
      }

      outStates.add(state);

      previousEntry = e;
      yesterdayWasFertile = !fertilityReasons.isEmpty();
      countOfThreeStarted = !state.countOfThreeReasons.isEmpty();
    }

    return outStates;
  }

  public static class CountOfThreeReason {
    public final Enum instruction;
    public final int count;

    public CountOfThreeReason(Enum instruction, int count) {
      this.instruction = instruction;
      this.count = count;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof CountOfThreeReason) {
        CountOfThreeReason that = (CountOfThreeReason) o;
        return Objects.equal(this.instruction, that.instruction) && Objects.equal(this.count, that.count);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(instruction, count);
    }
  }

  public static class RenderableEntry {
    public String entrySummary;
    public StickerColor backgroundColor;
    public int entryNum;
    public String dateSummary;
    public String peakDayText;
    public boolean showBaby;
    public IntercourseTimeOfDay intercourseTimeOfDay;
    public boolean isPointOfChange;
    public EntryModificationContext modificationContext;

    @NonNull
    @Override
    public String toString() {
      return String.format("%s: %s", dateSummary, entrySummary);
    }
  }

  private static RenderableEntry render(State state) {
    RenderableEntry entry = new RenderableEntry();

    entry.entryNum = state.entryNum;
    entry.dateSummary = DateUtil.toNewUiStr(state.entry.entryDate);
    entry.entrySummary = state.entry.observationEntry.getListUiText();
    entry.backgroundColor = getBackgroundColor(state.entry.observationEntry.observation, state);
    entry.showBaby = shouldShowBaby(state.entry.observationEntry, state);
    entry.peakDayText = peakDayText(state);
    entry.intercourseTimeOfDay = state.entry.observationEntry.intercourseTimeOfDay;
    entry.isPointOfChange = state.isPointOfChange;

    EntryModificationContext modificationContext = new EntryModificationContext();
    modificationContext.cycle = state.cycle;
    modificationContext.entry = state.entry;
    modificationContext.hasPreviousCycle = false;
    modificationContext.expectUnusualBleeding = expectUnusualBleeding(state);
    modificationContext.isFirstEntry = state.entryNum == 1;
    modificationContext.shouldAskEssentialSameness = shouldAskEssentialSameness(state);
    entry.modificationContext = modificationContext;

    return entry;
  }

  @Parcel
  public static class EntryModificationContext {
    public Cycle cycle;
    public ChartEntry entry;
    public boolean hasPreviousCycle;
    public boolean expectUnusualBleeding;
    public boolean isFirstEntry;
    public boolean shouldAskEssentialSameness;
  }

  private static List<RenderableEntry> render(List<State> states) {
    List<RenderableEntry> entries = new ArrayList<>(states.size());
    for (State state : states) {
      entries.add(render(state));
    }
    return entries;
  }

  private static String peakDayText(State state) {
    if (state.isPeakDay) {
      return "P";
    }
    if (state.isPointOfChange) {
      return "";
    }
    int lowestCount = Integer.MAX_VALUE;
   for (CountOfThreeReason reason : state.countOfThreeReasons.values()) {
      if (reason.count < lowestCount) {
        lowestCount = reason.count;
      }
    }
    if (lowestCount < Integer.MAX_VALUE) {
      return String.valueOf(lowestCount);
    }
    return "";
  }

  private static boolean shouldShowBaby(ObservationEntry observationEntry, @NonNull State state) {
    if (state.instructions == null) {
      return false;
    }
    if (observationEntry.observation == null) {
      return false;
    }
    Observation observation = observationEntry.observation;
    if (observation.hasBlood()) {
      return false;
    }
    if (state.instructions.isActive(BasicInstruction.K_1)
        && state.isPrePeak
        && state.isBeforePointOfChange) {
      return false;
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
        && state.isPointOfChange) {
      return true;
    }
    if (state.instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
        && Optional.fromNullable(state.previousEntry).transform(e -> e.observationEntry.intercourse).or(false)
        && observationEntry.isEssentiallyTheSame
        && observation.hasMucus()
        && observation.dischargeSummary.isPeakType()) {
      return false;
    }
    for (CountOfThreeReason reason : state.countOfThreeReasons.values()) {
      if (reason.instruction.equals(YellowStampInstruction.YS_1_C)
          && !state.instructions.isActive(YellowStampInstruction.YS_1_C)) {
        return false;
      }
      if (reason.instruction.equals(YellowStampInstruction.YS_1_D)
          && !state.instructions.isActive(YellowStampInstruction.YS_1_D)) {
        return false;
      }
      return true;
    }
    if (state.isPrePeak
        && state.instructions.isActive(YellowStampInstruction.YS_2_A)) {
      return false;
    }
    if (state.isPostPeak
        && (state.instructions.isActive(YellowStampInstruction.YS_2_B)
        || state.instructions.isActive(YellowStampInstruction.YS_2_C)
        || state.instructions.isActive(YellowStampInstruction.YS_2_D))) {
      return false;
    }
    return observation.hasMucus();
  }

  private static StickerColor getBackgroundColor(@Nullable Observation observation, @NonNull State state) {
    if (state.instructions == null) {
      return StickerColor.GREY;
    }
    if (observation == null) {
      return StickerColor.GREY;
    }
    if (observation.flow != null) {
      return StickerColor.RED;
    }
    if (observation.dischargeSummary.mModifiers.contains(DischargeSummary.MucusModifier.B)) {
      return StickerColor.RED;
    }
    if (!observation.dischargeSummary.mType.hasMucus()) {
      return StickerColor.GREEN;
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
        && state.isPointOfChange) {
      return StickerColor.WHITE;
    }
    if (state.instructions.isActive(BasicInstruction.K_1)
        && state.isPrePeak && state.isBeforePointOfChange) {
      return StickerColor.YELLOW;
    }
    if (state.instructions.anyActive(BasicInstruction.postPeakYellowBasicInstructions)
        && state.isPostPeak) {
      return StickerColor.YELLOW;
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
        && state.countOfThreeReasons.containsKey(YellowStampInstruction.YS_1_C)
        && state.entry.observationEntry.observation != null
        && state.entry.observationEntry.observation.hasMucus()) {
      return StickerColor.YELLOW;
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_2_A)
        && state.isPrePeak) {
      return StickerColor.YELLOW;
    }
    if (state.instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
        && Optional.fromNullable(state.previousEntry).transform(e -> e.observationEntry.intercourse).or(false)
        && state.entry.observationEntry.observation != null
        && state.entry.observationEntry.observation.dischargeSummary.isPeakType()
        && state.entry.observationEntry.isEssentiallyTheSame) {
      return StickerColor.YELLOW;
    }
    return StickerColor.WHITE;
  }

  private static boolean shouldAskEssentialSameness(State state) {
    if (state.instructions == null) {
      return false;
    }
    boolean askForSpecialInstruction = state.instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
        && Optional.fromNullable(state.previousEntry).transform(e -> e.observationEntry.intercourse).or(false);
    boolean askForPrePeakYellow = state.instructions.isActive(BasicInstruction.K_1) && state.isPrePeak;
    return askForPrePeakYellow || askForSpecialInstruction;
  }

  private static boolean expectUnusualBleeding(State state) {
    if (state.previousEntry == null) {
      return false;
    }
    if (state.previousEntry.observationEntry.unusualBleeding) {
      return true;
    }
    return state.previousEntry.observationEntry.observation == null
        || !state.previousEntry.observationEntry.observation.hasBlood();
  }

  private static Optional<LocalDate> getMostRecent(TreeSet<LocalDate> dates, LocalDate targetDate) {
    for (LocalDate d : dates.descendingSet()) {
      if (!targetDate.isBefore(d)) {
        return Optional.of(d);
      }
    }
    return Optional.absent();
  }

}
