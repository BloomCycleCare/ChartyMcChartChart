package com.roamingroths.cmcc.logic.chart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.google.common.base.Optional;
import com.roamingroths.cmcc.data.domain.AbstractInstruction;
import com.roamingroths.cmcc.data.domain.BasicInstruction;
import com.roamingroths.cmcc.data.domain.DischargeSummary;
import com.roamingroths.cmcc.data.domain.Flow;
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

  public List<RenderableEntry> render() {
    return render(getStates());
  }

  public List<State> getStates() {
    List<ChartEntry> entriesEvaluated = new ArrayList<>();
    Set<LocalDate> daysOfFlow = new HashSet<>();
    Set<LocalDate> daysOfMucus = new HashSet<>();
    TreeSet<LocalDate> daysOfUnusualBleeding = new TreeSet<>();
    TreeSet<LocalDate> peakDays = new TreeSet<>();
    TreeSet<LocalDate> pointsOfChange = new TreeSet<>();
    Map<LocalDate, Boolean> daysOfIntercourse = new HashMap<>();
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

      // Step 1: Gather basic info which does not depend on the active instructions
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
      if (e.observationEntry.hasMucus()) {
        daysOfMucus.add(e.entryDate);
      }
      daysOfIntercourse.put(e.entryDate, e.observationEntry.intercourseTimeOfDay != IntercourseTimeOfDay.NONE);
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
      state.entryDate = e.entryDate;
      state.entryNum = Days.daysBetween(mCycle.startDate, e.entryDate).getDays() + 1;
      if (peakDays.isEmpty()) {
        state.firstPeakDay = Optional.absent();
        state.mostRecentPeakDay = Optional.absent();
      } else {
        state.firstPeakDay = Optional.of(peakDays.first());
        state.mostRecentPeakDay = Optional.of(peakDays.last());
      }
      state.isInMenstrualFlow = isInMenstrualFlow;
      if (pointsOfChange.isEmpty()) {
        state.firstPointOfChange = Optional.absent();
        state.mostRecentPointOfChange = Optional.absent();
      } else {
        state.firstPointOfChange = Optional.of(pointsOfChange.first());
        state.mostRecentPointOfChange = Optional.of(pointsOfChange.last());
      }
      state.hasHadAnyMucus = !daysOfMucus.isEmpty();
      state.consecutiveDaysOfMucus = consecutiveDaysOfMucus;
      LocalDate yesterday = e.entryDate.minusDays(1);
      state.hadIntercourseYesterday = daysOfIntercourse.containsKey(yesterday) && daysOfIntercourse.get(yesterday);
      if (!peakDays.isEmpty()) {
        state.countsOfThree.put(
            CountOfThreeReason.PEAK_DAY,
            Days.daysBetween(peakDays.last(), e.entryDate).getDays());
      }
      if (lastDayOfThreeOrMoreDaysOfMucus != null) {
        state.countsOfThree.put(
            CountOfThreeReason.CONSECUTIVE_DAYS_OF_MUCUS,
            Days.daysBetween(lastDayOfThreeOrMoreDaysOfMucus, e.entryDate).getDays());
      }
      if (mostRecentPeakTypeMucus != null) {
        state.countsOfThree.put(
            CountOfThreeReason.PEAK_TYPE_MUCUS,
            Days.daysBetween(mostRecentPeakTypeMucus, e.entryDate).getDays());
      }
      if (!daysOfUnusualBleeding.isEmpty()) {
        state.countsOfThree.put(
            CountOfThreeReason.UNUSUAL_BLEEDING,
            Days.daysBetween(daysOfUnusualBleeding.last(), e.entryDate).getDays());
      }
      if (!pointsOfChange.isEmpty()) {
        state.countsOfThree.put(
            CountOfThreeReason.POINT_OF_CHANGE,
            Days.daysBetween(pointsOfChange.last(), e.entryDate).getDays());
      }
      state.previousEntry = previousEntry;
      state.countOfThreeReasons = new HashMap<>();

      Instructions instructions = null;
      for (Instructions i : mInstructions.descendingSet()) {
        if (!e.entryDate.isBefore(i.startDate)) {
          instructions = i;
          break;
        }
      }
      state.instructions = instructions;

      // Step 2: Check conditions which depend on the active instructions
      /*LocalDate threeDaysAgo = e.entryDate.minusDays(3);
      Optional<LocalDate> mostRecentPeakDay = getMostRecent(peakDays, e.entryDate);
      if (state.instructions.isActive(BasicInstruction.D_2)
          && mostRecentPeakDay.isPresent()
          && !mostRecentPeakDay.get().isBefore(threeDaysAgo)) {
        state.countOfThreeReasons.put(BasicInstruction.D_2, new CountOfThreeReason(
            BasicInstruction.D_2,
            Days.daysBetween(mostRecentPeakDay.get(), e.entryDate).getDays()));
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

      // Step 3: Check conditions which depend both on active instructions and recent fertility trend
      boolean endOfFertilityStretch = yesterdayWasFertile && fertilityReasons.isEmpty();
      if (endOfFertilityStretch || countOfThreeStarted) {
        if (instructions.isActive(BasicInstruction.D_5)
            && mostRecentPeakTypeMucus != null
            && !mostRecentPeakTypeMucus.isBefore(threeDaysAgo)) {
          state.countOfThreeReasons.put(BasicInstruction.D_5, new CountOfThreeReason(
              BasicInstruction.D_5,
              Days.daysBetween(mostRecentPeakTypeMucus, e.entryDate).getDays()));
        }
        if (instructions.isActive(BasicInstruction.D_4)
            && state.isPrePeak()
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
      }*/

      outStates.add(state);

      previousEntry = e;
      yesterdayWasFertile = !fertilityReasons.isEmpty();
      countOfThreeStarted = !state.countOfThreeReasons.isEmpty();
    }

    return outStates;
  }

  private static Pair<Integer, AbstractInstruction> updateCountOfThree(
      Pair<Integer, AbstractInstruction> countOfThree, int newCount, AbstractInstruction instruction) {
    if (newCount >= 0 && (countOfThree.first == null || newCount < countOfThree.first)) {
      return Pair.create(newCount, instruction);
    }
    return countOfThree;
  }

  private static RenderableEntry render(State state) {
    RenderableEntry entry = new RenderableEntry();

    Pair<Integer, AbstractInstruction> countOfThree = new Pair<>(null, null);

    if (state.instructions.isActive(BasicInstruction.D_1)
        && state.isInMenstrualFlow) {
      entry.fertilityReasons.add(BasicInstruction.D_1);
    }
    if (state.instructions.isActive(BasicInstruction.D_2)
        && state.hasHadAnyMucus
        && !state.isPostPeakPlus(3)) {
      entry.fertilityReasons.add(BasicInstruction.D_2);
      countOfThree = updateCountOfThree(
          countOfThree, state.getCount(CountOfThreeReason.PEAK_DAY), BasicInstruction.D_2);
    }
    if (state.instructions.isActive(BasicInstruction.D_3)
        && state.isPrePeak()
        && state.consecutiveDaysOfMucus > 0
        && state.consecutiveDaysOfMucus < 3) {
      entry.fertilityReasons.add(BasicInstruction.D_3);
    }
    if (state.instructions.isActive(BasicInstruction.D_4)
        && state.isPrePeak()
        && state.isWithinCountOfThree(CountOfThreeReason.CONSECUTIVE_DAYS_OF_MUCUS)) {
      entry.fertilityReasons.add(BasicInstruction.D_4);
      countOfThree = updateCountOfThree(
          countOfThree, state.getCount(CountOfThreeReason.CONSECUTIVE_DAYS_OF_MUCUS), BasicInstruction.D_4);
    }
    if (state.instructions.isActive(BasicInstruction.D_5)
        && state.isWithinCountOfThree(CountOfThreeReason.PEAK_TYPE_MUCUS)) {
      entry.fertilityReasons.add(BasicInstruction.D_5);
      countOfThree = updateCountOfThree(
          countOfThree, state.getCount(CountOfThreeReason.PEAK_TYPE_MUCUS), BasicInstruction.D_5);
    }
    if (state.instructions.isActive(BasicInstruction.D_6)
        && state.isWithinCountOfThree(CountOfThreeReason.UNUSUAL_BLEEDING)) {
      entry.fertilityReasons.add(BasicInstruction.D_6);
      countOfThree = updateCountOfThree(
          countOfThree, state.getCount(CountOfThreeReason.UNUSUAL_BLEEDING), BasicInstruction.D_6);
    }

    if (state.instructions.isActive(YellowStampInstruction.YS_1_A)
        && state.isInMenstrualFlow) {
      entry.fertilityReasons.add(YellowStampInstruction.YS_1_A);
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_B)
        && !state.isBeforePointOfChange()
        && (state.isPrePeak() || state.isWithinCountOfThree(CountOfThreeReason.PEAK_DAY))) {
      entry.fertilityReasons.add(YellowStampInstruction.YS_1_B);
      countOfThree = updateCountOfThree(
          countOfThree, state.getCount(CountOfThreeReason.PEAK_DAY), YellowStampInstruction.YS_1_B);
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
        && state.isWithinCountOfThree(CountOfThreeReason.POINT_OF_CHANGE)) {
      entry.fertilityReasons.add(YellowStampInstruction.YS_1_C);
      countOfThree = updateCountOfThree(
          countOfThree, state.getCount(CountOfThreeReason.POINT_OF_CHANGE), YellowStampInstruction.YS_1_C);
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_D)
        && state.isWithinCountOfThree(CountOfThreeReason.UNUSUAL_BLEEDING)) {
      entry.fertilityReasons.add(YellowStampInstruction.YS_1_D);
      countOfThree = updateCountOfThree(
          countOfThree, state.getCount(CountOfThreeReason.UNUSUAL_BLEEDING), YellowStampInstruction.YS_1_D);
    }

    if (state.isPrePeak()
        && state.entry.observationEntry.isDryDay()) {
      if (state.instructions.isActive(BasicInstruction.E_1)
          && !state.hadIntercourseYesterday) {
        entry.infertilityReasons.add(BasicInstruction.E_1);
      }
      if (state.instructions.isActive(BasicInstruction.E_2)) {
        entry.infertilityReasons.add(BasicInstruction.E_2);
      }
    }
    Optional<LocalDate> peakPlusFour = state.mostRecentPeakDay.transform(peakDay -> peakDay.plusDays(4));
    if (peakPlusFour.isPresent()) {
      if (state.instructions.isActive(BasicInstruction.E_3)
          && state.entryDate.equals(peakPlusFour.get())) {
        entry.infertilityReasons.add(BasicInstruction.E_3);
      }
      if (state.entryDate.isAfter(peakPlusFour.get())
          && state.entry.observationEntry.isDryDay()) {
        if (state.instructions.isActive(BasicInstruction.E_4)
            && !state.hadIntercourseYesterday) {
          entry.infertilityReasons.add(BasicInstruction.E_4);
        }
        if (state.instructions.isActive(BasicInstruction.E_5)) {
          entry.infertilityReasons.add(BasicInstruction.E_5);
        }
        if (state.instructions.isActive(BasicInstruction.E_6)) {
          entry.infertilityReasons.add(BasicInstruction.E_6);
        }
      }
    }
    if (state.instructions.isActive(BasicInstruction.E_7)
        && state.isInMenstrualFlow
        && state.entry.observationEntry.hasObservation()
        && state.entry.observationEntry.hasBlood()
        && !(state.entry.observationEntry.observation.flow == Flow.H
             || state.entry.observationEntry.observation.flow == Flow.M)) {
      entry.infertilityReasons.add(BasicInstruction.E_7);
    }

    entry.entryNum = state.entryNum;
    entry.dateSummary = DateUtil.toNewUiStr(state.entry.entryDate);
    entry.entrySummary = state.entry.observationEntry.getListUiText();
    entry.backgroundColor = getBackgroundColor(state.entry.observationEntry.observation, state);
    entry.showBaby = shouldShowBaby(state.entry.observationEntry, state, countOfThree);
    entry.peakDayText = peakDayText(state, countOfThree);
    entry.intercourseTimeOfDay = state.entry.observationEntry.intercourseTimeOfDay;
    entry.isPointOfChange = state.isPointOfChange();
    entry.countOfThreeCount = Optional.fromNullable(countOfThree.first).or(-1);
    entry.countOfThreeInstruction = countOfThree.second;

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

  private static List<RenderableEntry> render(List<State> states) {
    List<RenderableEntry> entries = new ArrayList<>(states.size());
    for (State state : states) {
      entries.add(render(state));
    }
    return entries;
  }

  private static String peakDayText(State state, Pair<Integer, AbstractInstruction> countOfThree) {
    if (state.isPeakDay()) {
      return "P";
    }
    if (countOfThree.first == null || countOfThree.first == 0) {
      return "";
    }
    if (countOfThree.first > 0) {
      return String.valueOf(countOfThree.first);
    }
    return "";
  }

  private static boolean shouldShowBaby(ObservationEntry observationEntry, @NonNull State state, Pair<Integer, AbstractInstruction> countOfThree) {
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
        && state.isPrePeak()
        && state.isBeforePointOfChange()) {
      return false;
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
        && state.isPointOfChange()) {
      return true;
    }
    if (state.instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
        && Optional.fromNullable(state.previousEntry).transform(e -> e.observationEntry.intercourse).or(false)
        && observationEntry.isEssentiallyTheSame
        && observation.hasMucus()
        && observation.dischargeSummary.isPeakType()) {
      return false;
    }
    if (countOfThree.second != null) {
      return true;
    }
    if (state.isPrePeak()
        && state.instructions.isActive(YellowStampInstruction.YS_2_A)) {
      return false;
    }
    if (state.isPrePeak()
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
        && state.isPointOfChange()) {
      return StickerColor.WHITE;
    }
    if (state.instructions.isActive(BasicInstruction.K_1)
        && state.isPrePeak() && state.isBeforePointOfChange()) {
      return StickerColor.YELLOW;
    }
    if (state.instructions.anyActive(BasicInstruction.postPeakYellowBasicInstructions)
        && state.isPrePeak()) {
      return StickerColor.YELLOW;
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
        && state.countOfThreeReasons.containsKey(YellowStampInstruction.YS_1_C)
        && state.entry.observationEntry.observation != null
        && state.entry.observationEntry.observation.hasMucus()) {
      return StickerColor.YELLOW;
    }
    if (state.instructions.isActive(YellowStampInstruction.YS_2_A)
        && state.isPrePeak()) {
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
    boolean askForPrePeakYellow = state.instructions.isActive(BasicInstruction.K_1) && state.isPrePeak();
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

  public static class State {
    public Cycle cycle;
    public ChartEntry entry;
    public LocalDate entryDate;
    public Instructions instructions;
    public int entryNum;
    public Optional<LocalDate> firstPeakDay;
    public Optional<LocalDate> mostRecentPeakDay;
    public boolean isInMenstrualFlow;
    public Optional<LocalDate> firstPointOfChange;
    public Optional<LocalDate> mostRecentPointOfChange;
    public boolean hasHadAnyMucus;
    public int consecutiveDaysOfMucus;
    public boolean hadIntercourseYesterday;
    public Optional<LocalDate> mostRecentDayOfThreeOrMoreConsecutiveDaysOfMucus;
    public Map<CountOfThreeReason, Integer> countsOfThree = new HashMap<>();

    public Map<Enum, CountOfThreeReason> countOfThreeReasons;
    public ChartEntry previousEntry;

    public boolean isPrePeak() {
      return !firstPeakDay.isPresent() || entryDate.isBefore(firstPeakDay.get());
    }

    public boolean isPeakDay() {
      return mostRecentPeakDay.isPresent() && mostRecentPeakDay.get().equals(entryDate);
    }

    public boolean isPostPeak() {
      return isPostPeakPlus(0);
    }

    public boolean isPostPeakPlus(int numDays) {
      return mostRecentPeakDay.isPresent() && entryDate.isAfter(mostRecentPeakDay.get().plusDays(numDays));
    }

    public boolean isPointOfChange() {
      return mostRecentPointOfChange.isPresent() && mostRecentPointOfChange.get().equals(entryDate);
    }

    public boolean isBeforePointOfChange() {
      return !firstPointOfChange.isPresent() || entryDate.isBefore(firstPointOfChange.get());
    }

    public int getCount(CountOfThreeReason reason) {
      if (!countsOfThree.containsKey(reason)) {
        return -1;
      }
      return countsOfThree.get(reason);
    }

    public boolean isWithinCountOfThree(CountOfThreeReason reason) {
      int count = getCount(reason);
      return count >= 0 && count < 4;
    }
  }

  public enum CountOfThreeReason {
    UNUSUAL_BLEEDING, PEAK_DAY, CONSECUTIVE_DAYS_OF_MUCUS, PEAK_TYPE_MUCUS, POINT_OF_CHANGE;
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
    public Set<AbstractInstruction> fertilityReasons = new HashSet<>();
    public Set<AbstractInstruction> infertilityReasons = new HashSet<>();

    private int countOfThreeCount = -1;
    private AbstractInstruction countOfThreeInstruction = null;

    @NonNull
    @Override
    public String toString() {
      return String.format("%s: %s", dateSummary, entrySummary);
    }
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
}
