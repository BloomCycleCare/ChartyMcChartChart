package com.roamingroths.cmcc.logic.chart;

import com.google.common.base.Joiner;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;
import timber.log.Timber;

public class CycleRenderer {

  private static final Joiner ON_NEW_LINE = Joiner.on("\n");
  private static final Joiner ON_DOUBLE_NEW_LINE = Joiner.on("\n\n");

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
    List<LocalDate> pointsOfChangeToward = new ArrayList<>();
    List<LocalDate> pointsOfChangeAway = new ArrayList<>();
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
      LocalDate yesterday = e.entryDate.minusDays(1);

      State state = new State();
      state.cycle = mCycle;
      state.entry = e;
      state.entryDate = e.entryDate;
      state.entryNum = Days.daysBetween(mCycle.startDate, e.entryDate).getDays() + 1;
      state.previousEntry = previousEntry;

      // Step 1: Gather basic info which does not depend on the active instructions
      if (e.observationEntry.peakDay) {
        peakDays.add(e.entryDate);
      }
      if (e.observationEntry.pointOfChange) {
        if (pointsOfChangeToward.size() == pointsOfChangeAway.size()) {
          pointsOfChangeToward.add(e.entryDate);
        } else {
          pointsOfChangeAway.add(e.entryDate);
        }
      }
      state.firstPointOfChangeToward = pointsOfChangeToward.isEmpty() ? Optional.absent()
          : Optional.of(pointsOfChangeToward.get(0));
      state.mostRecentPointOfChangeToward = pointsOfChangeToward.isEmpty() ? Optional.absent()
          : Optional.of(pointsOfChangeToward.get(pointsOfChangeToward.size() - 1));
      state.mostRecentPointOfChangeAway = pointsOfChangeAway.isEmpty() ? Optional.absent()
          : Optional.of(pointsOfChangeAway.get(pointsOfChangeAway.size() - 1));
      if (e.observationEntry.unusualBleeding) {
        daysOfUnusualBleeding.add(e.entryDate);
      }
      if (e.observationEntry.hasMucus()) {
        daysOfMucus.add(e.entryDate);
      }
      daysOfIntercourse.put(e.entryDate, e.observationEntry.intercourseTimeOfDay != IntercourseTimeOfDay.NONE);
      boolean todayHasMucus = false;
      Flow todaysFlow = null;
      if (e.observationEntry.observation == null) {
        consecutiveDaysOfMucus = 0;
      } else {
        Observation observation = e.observationEntry.observation;
        todayHasMucus = observation.hasMucus();
        if (observation.flow != null) {
          todaysFlow = observation.flow;
          daysOfFlow.add(e.entryDate);
        }
        if (todayHasMucus) {
          consecutiveDaysOfMucus++;
          if (consecutiveDaysOfMucus >= 3) {
            lastDayOfThreeOrMoreDaysOfMucus = e.entryDate;
          } else {
          }
          if (observation.dischargeSummary.isPeakType()) {
            mostRecentPeakTypeMucus = e.entryDate;
          }
        } else {
          consecutiveDaysOfMucus = 0;
        }
      }
      if (peakDays.isEmpty()) {
        state.firstPeakDay = Optional.absent();
        state.mostRecentPeakDay = Optional.absent();
      } else {
        state.firstPeakDay = Optional.of(peakDays.first());
        state.mostRecentPeakDay = Optional.of(peakDays.last());
      }
      state.hasHadAnyMucus = !daysOfMucus.isEmpty();
      state.consecutiveDaysOfMucus = consecutiveDaysOfMucus;
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
      Optional<LocalDate> effectivePointOfChange = effectivePointOfChange(pointsOfChangeToward, pointsOfChangeAway);
      if (effectivePointOfChange.isPresent()) {
        state.countsOfThree.put(
            CountOfThreeReason.POINT_OF_CHANGE,
            Days.daysBetween(effectivePointOfChange.get(), e.entryDate).getDays());
      }

      boolean isInMenstrualFlow = entriesEvaluated.size() == daysOfFlow.size();
      boolean hasHadAnyMucus = !daysOfMucus.isEmpty();
      boolean hadIntercourseYesterday = daysOfIntercourse.containsKey(yesterday) && daysOfIntercourse.get(yesterday);

      // Step 2: Evaluate fertility reasons
      Instructions instructions = null;
      for (Instructions i : mInstructions.descendingSet()) {
        if (!e.entryDate.isBefore(i.startDate)) {
          instructions = i;
          break;
        }
      }
      state.instructions = Preconditions.checkNotNull(instructions);

      Pair<Integer, AbstractInstruction> countOfThree = new Pair<>(null, null);

      // Basic Instruction fertility reasons (section D)
      if (instructions.isActive(BasicInstruction.D_1)
          && isInMenstrualFlow) {
        state.fertilityReasons.add(BasicInstruction.D_1);
      }
      if (state.instructions.isActive(BasicInstruction.D_2)
          && state.hasHadAnyMucus
          && !state.isPostPeakPlus(3)) {
        state.fertilityReasons.add(BasicInstruction.D_2);
        state.updateCountOfThree(
            state.getCount(CountOfThreeReason.PEAK_DAY), BasicInstruction.D_2);
      }
      if (state.instructions.isActive(BasicInstruction.D_3)
          && state.isPrePeak()
          && consecutiveDaysOfMucus > 0
          && consecutiveDaysOfMucus < 3) {
        state.fertilityReasons.add(BasicInstruction.D_3);
      }
      if (state.instructions.isActive(BasicInstruction.D_4)
          && state.isPrePeak()
          && state.isWithinCountOfThree(CountOfThreeReason.CONSECUTIVE_DAYS_OF_MUCUS)) {
        state.fertilityReasons.add(BasicInstruction.D_4);
        state.updateCountOfThree(
            state.getCount(CountOfThreeReason.CONSECUTIVE_DAYS_OF_MUCUS), BasicInstruction.D_4);
      }
      if (state.instructions.isActive(BasicInstruction.D_5)
          && state.isWithinCountOfThree(CountOfThreeReason.PEAK_TYPE_MUCUS)) {
        state.fertilityReasons.add(BasicInstruction.D_5);
        state.updateCountOfThree(
            state.getCount(CountOfThreeReason.PEAK_TYPE_MUCUS), BasicInstruction.D_5);
      }
      if (state.instructions.isActive(BasicInstruction.D_6)
          && state.isWithinCountOfThree(CountOfThreeReason.UNUSUAL_BLEEDING)) {
        state.fertilityReasons.add(BasicInstruction.D_6);
        state.updateCountOfThree(
            state.getCount(CountOfThreeReason.UNUSUAL_BLEEDING), BasicInstruction.D_6);
      }

      // Basic Instruction infertility reasons (section E)
      if (!todayHasMucus && state.isPrePeak()) {
        if (state.instructions.isActive(BasicInstruction.E_1)) {
          state.infertilityReasons.add(BasicInstruction.E_1);
        }
        if (state.instructions.isActive(BasicInstruction.E_2)) {
          state.infertilityReasons.add(BasicInstruction.E_2);
        }
      }
      if (state.instructions.isActive(BasicInstruction.E_3)
          && state.isExactlyPostPeakPlus(4)) {
        state.infertilityReasons.add(BasicInstruction.E_3);
      }
      if (!todayHasMucus && state.isPostPeakPlus(3)) {
        if (state.instructions.isActive(BasicInstruction.E_4)) {
          state.infertilityReasons.add(BasicInstruction.E_4);
        }
        if (state.instructions.isActive(BasicInstruction.E_5)) {
          state.infertilityReasons.add(BasicInstruction.E_5);
        }
        if (state.instructions.isActive(BasicInstruction.E_6)) {
          state.infertilityReasons.add(BasicInstruction.E_6);
        }
      }
      if (!todayHasMucus && isInMenstrualFlow && (
          todaysFlow.equals(Flow.L) || todaysFlow.equals(Flow.VL))) { // TODO: B
        state.infertilityReasons.add(BasicInstruction.E_7);
      }

      // Basic Instruction yellow stamp reasons (section K)
      boolean withinEssentialSamenessPattern = state.isPrePeak() && effectivePointOfChange.isPresent();
      if (instructions.isActive(BasicInstruction.K_1)
          && state.isPrePeak()
          && (!effectivePointOfChange.isPresent()
          || state.entryDate.isBefore(effectivePointOfChange.get()))) {
        state.suppressBasicInstructions(BasicInstruction.K_1);
      }
      if (state.isPostPeak()) {
        if (instructions.isActive(BasicInstruction.K_2)) {
          state.relaxBasicInstruction(BasicInstruction.E_4, BasicInstruction.K_2);
        }
        if (instructions.isActive(BasicInstruction.K_3)) {
          state.relaxBasicInstruction(BasicInstruction.E_5, BasicInstruction.K_3);
        }
        if (instructions.isActive(BasicInstruction.K_4)) {
          state.relaxBasicInstruction(BasicInstruction.E_6, BasicInstruction.K_4);
        }
      }

      // Special Instruction Yellow Stamp fertility reasons (section 1)
      if (state.instructions.isActive(YellowStampInstruction.YS_1_A)
          && isInMenstrualFlow) {
        state.fertilityReasons.add(YellowStampInstruction.YS_1_A);
      }
      if (state.instructions.isActive(YellowStampInstruction.YS_1_B)
          // This is to catch cases where you have a peak day w/o a point of change...
          // TODO: flag this as an issue?
          && (state.isWithinCountOfThree(CountOfThreeReason.PEAK_DAY)
          || effectivePointOfChange.isPresent()
          && !state.entryDate.isBefore(effectivePointOfChange.get())
          && state.isPrePeak())) {
        state.fertilityReasons.add(YellowStampInstruction.YS_1_B);
        state.updateCountOfThree(
            state.getCount(CountOfThreeReason.PEAK_DAY), YellowStampInstruction.YS_1_B);
      }
      if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
          && effectivePointOfChange.isPresent()
          && !state.entryDate.isAfter(effectivePointOfChange.get().plusDays(3))) {
        state.fertilityReasons.add(YellowStampInstruction.YS_1_C);
        state.updateCountOfThree(
            state.getCount(CountOfThreeReason.POINT_OF_CHANGE), YellowStampInstruction.YS_1_C);
      }
      if (state.instructions.isActive(YellowStampInstruction.YS_1_D)
          && state.isWithinCountOfThree(CountOfThreeReason.UNUSUAL_BLEEDING)) {
        state.fertilityReasons.add(YellowStampInstruction.YS_1_D);
        state.updateCountOfThree(
            state.getCount(CountOfThreeReason.UNUSUAL_BLEEDING), YellowStampInstruction.YS_1_D);
      }

      // Special Instruction Yellow Stamp infertility reasons (section 2)
      if (state.instructions.isActive(YellowStampInstruction.YS_2_A)
          && state.isPrePeak()
          && (!effectivePointOfChange.isPresent()
          || state.entryDate.isBefore(effectivePointOfChange.get()))) {
        state.suppressBasicInstructions(YellowStampInstruction.YS_2_A);
      }
      if (state.isPostPeakPlus(3)) {
        if (state.instructions.isActive(YellowStampInstruction.YS_2_B)) {
          state.relaxBasicInstruction(BasicInstruction.E_4, YellowStampInstruction.YS_2_B);
        }
        if (state.instructions.isActive(YellowStampInstruction.YS_2_C)) {
          state.relaxBasicInstruction(BasicInstruction.E_5, YellowStampInstruction.YS_2_C);
        }
        if (state.instructions.isActive(YellowStampInstruction.YS_2_D)) {
          state.relaxBasicInstruction(BasicInstruction.E_6, YellowStampInstruction.YS_2_D);
        }
      }

      // Super special infertility instructions...
      if (state.instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
          && Optional.fromNullable(state.previousEntry).transform(pe -> pe.observationEntry.intercourse).or(false)
          && state.entry.observationEntry.observation != null
          && state.entry.observationEntry.observation.dischargeSummary.isPeakType()
          && state.entry.observationEntry.isEssentiallyTheSame) {
        state.suppressBasicInstructions(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);
      }

      outStates.add(state);

      previousEntry = e;
      yesterdayWasFertile = !state.fertilityReasons.isEmpty();
      countOfThreeStarted = !state.countOfThreeReasons.isEmpty();
    }

    return outStates;
  }

  private static Optional<LocalDate> effectivePointOfChange(List<LocalDate> toward, List<LocalDate> away) {
    if (toward.isEmpty() || toward.size() == away.size()) {
      return Optional.absent();
    }
    return Optional.of(toward.get(toward.size() - 1));
  }

  private static RenderableEntry render(State state) {
    RenderableEntry entry = new RenderableEntry();

    entry.entryNum = state.entryNum;
    entry.dateSummary = DateUtil.toNewUiStr(state.entry.entryDate);
    entry.entrySummary = state.entry.observationEntry.getListUiText();
    entry.backgroundColor = getBackgroundColor(state.entry.observationEntry.observation, state);
    entry.showBaby = shouldShowBaby(state);
    entry.peakDayText = peakDayText(state);
    entry.intercourseTimeOfDay = state.entry.observationEntry.intercourseTimeOfDay;
    if (state.isPocTowardFertility()) {
      entry.pocSummary = "POC↑";
    } else if (state.isPocAwayFromFertility()) {
      entry.pocSummary = "POC↓";
    } else {
      entry.pocSummary = "";
    }
    entry.countOfThreeCount = Optional.fromNullable(state.effectiveCountOfThree.first).or(-1);
    entry.countOfThreeInstruction = state.effectiveCountOfThree.second;
    entry.fertilityReasons.addAll(state.fertilityReasons);
    entry.infertilityReasons.addAll(state.infertilityReasons);
    entry.suppressedFertilityReasons.putAll(state.suppressedFertilityReasons);
    entry.relaxedInfertilityReasons.putAll(state.relaxedInfertilityReasons);
    entry.instructionSummary = getInstructionSummary(state);

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

  private static String getInstructionSummary(State state) {
    List<String> instructionSummaryLines = new ArrayList<>();
    List<String> subsectionLines = new ArrayList<>();
    if (!state.fertilityReasons.isEmpty()) {
      subsectionLines.add("Fertility Reasons:");
      for (AbstractInstruction i : state.fertilityReasons) {
        subsectionLines.add(String.format(" - %s", AbstractInstruction.summary(i)));
      }
      instructionSummaryLines.add(ON_NEW_LINE.join(subsectionLines));
      subsectionLines.clear();
    }
    if (!state.infertilityReasons.isEmpty()) {
      subsectionLines.add("Infertility Reasons:");
      for (AbstractInstruction i : state.infertilityReasons) {
        subsectionLines.add(String.format(" - %s", AbstractInstruction.summary(i)));
      }
      instructionSummaryLines.add(ON_NEW_LINE.join(subsectionLines));
      subsectionLines.clear();
    }
    boolean provideYellowStampSummary =
        state.infertilityReasons.isEmpty() && state.fertilityReasons.isEmpty();
    if (provideYellowStampSummary && !state.relaxedInfertilityReasons.isEmpty()) {
      subsectionLines.add("Relaxed Infertility Reasons:");
      for (AbstractInstruction i : state.relaxedInfertilityReasons.keySet()) {
        subsectionLines.add(String.format(" - %s", AbstractInstruction.summary(i)));
      }
      instructionSummaryLines.add(ON_NEW_LINE.join(subsectionLines));
      subsectionLines.clear();
    }
    if (provideYellowStampSummary && !state.suppressedFertilityReasons.isEmpty()) {
      subsectionLines.add("Suppressed Fertility Reasons:");
      for (AbstractInstruction i : state.suppressedFertilityReasons.keySet()) {
        subsectionLines.add(String.format(" - %s", AbstractInstruction.summary(i)));
      }
      instructionSummaryLines.add(ON_NEW_LINE.join(subsectionLines));
      subsectionLines.clear();
    }
    if (!subsectionLines.isEmpty()) {
      Timber.w("Leaking strings!");
    }
    return ON_DOUBLE_NEW_LINE.join(instructionSummaryLines);
  }

  private static String peakDayText(State state) {
    if (state.isPeakDay()) {
      return "P";
    }
    if (state.fertilityReasons.isEmpty()) {
      return "";
    }
    if (state.effectiveCountOfThree.first == null || state.effectiveCountOfThree.first == 0) {
      return "";
    }
    if (state.effectiveCountOfThree.first > 0) {
      return String.valueOf(state.effectiveCountOfThree.first);
    }
    return "";
  }

  private static boolean shouldShowBaby(@NonNull State state) {
    if (state.fertilityReasons.isEmpty()) {
      return false;
    }
    if (state.entry.observationEntry.hasBlood()) {
      return false;
    }
    return true;
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
    if (state.fertilityReasons.isEmpty() && state.suppressedFertilityReasons.isEmpty() && state.relaxedInfertilityReasons.isEmpty()) {
      return StickerColor.GREEN;
    }
    // Now it's either white or yellow...
    if (!state.suppressedFertilityReasons.isEmpty()) {
      return StickerColor.YELLOW;
    }
    if (!state.relaxedInfertilityReasons.isEmpty()) {
      return StickerColor.YELLOW;
    }
    if (!state.fertilityReasons.isEmpty()) {
      return StickerColor.WHITE;
    }
    // TODO: change to WHITE?
    return StickerColor.GREY;
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
    public Optional<LocalDate> firstPointOfChangeToward;
    public Optional<LocalDate> mostRecentPointOfChangeToward;
    public Optional<LocalDate> mostRecentPointOfChangeAway;
    public boolean hasHadAnyMucus;
    public int consecutiveDaysOfMucus;
    public boolean hadIntercourseYesterday;
    public Optional<LocalDate> mostRecentDayOfThreeOrMoreConsecutiveDaysOfMucus;
    public Map<CountOfThreeReason, Integer> countsOfThree = new HashMap<>();

    public Set<AbstractInstruction> fertilityReasons = new HashSet<>();
    public Map<AbstractInstruction, AbstractInstruction> suppressedFertilityReasons = new HashMap<>();
    public Map<AbstractInstruction, AbstractInstruction> relaxedInfertilityReasons = new HashMap<>();
    public Set<AbstractInstruction> infertilityReasons = new HashSet<>();

    public Map<AbstractInstruction, CountOfThreeReason> countOfThreeReasons = new HashMap<>();
    public Pair<Integer, AbstractInstruction> effectiveCountOfThree = Pair.create(null, null);
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

    public boolean isExactlyPostPeakPlus(int numDays) {
      return mostRecentPeakDay.isPresent() && entryDate.equals(mostRecentPeakDay.get().plusDays(numDays));
    }

    public boolean isPocTowardFertility() {
      return mostRecentPointOfChangeToward.isPresent() && mostRecentPointOfChangeToward.get().equals(entryDate);
    }

    public boolean isPocAwayFromFertility() {
      return mostRecentPointOfChangeAway.isPresent() && mostRecentPointOfChangeAway.get().equals(entryDate);
    }

    public int getCount(CountOfThreeReason reason) {
      if (!countsOfThree.containsKey(reason)) {
        return -1;
      }
      if (reason == CountOfThreeReason.POINT_OF_CHANGE) {
        // We don't number this case
        return -1;
      }
      return countsOfThree.get(reason);
    }

    public boolean isWithinCountOfThree(CountOfThreeReason reason) {
      int count = getCount(reason);
      return count >= 0 && count < 4;
    }

    public void suppressBasicInstructions(
        AbstractInstruction suppressionReason) {
      for (BasicInstruction reason : BasicInstruction.suppressableByPrePeakYellow) {
        if (fertilityReasons.remove(reason)) {
          suppressedFertilityReasons.put(reason, suppressionReason);
        }
      }
    }

    public void relaxBasicInstruction(BasicInstruction instructionToRelax, AbstractInstruction reasonToRelax) {
      for (BasicInstruction reason : BasicInstruction.suppressableByPostPeakYellow) {
        if (fertilityReasons.remove(reason)) {
          suppressedFertilityReasons.put(reason, reasonToRelax);
        }
      }
      relaxedInfertilityReasons.put(instructionToRelax, reasonToRelax);
    }

    public void updateCountOfThree(int newCount, AbstractInstruction instruction) {
      if (newCount >= 0 && (
          effectiveCountOfThree == null || effectiveCountOfThree.first == null || newCount < effectiveCountOfThree.first)) {
        effectiveCountOfThree = Pair.create(newCount, instruction);
      }
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
    public String instructionSummary;
    public boolean showBaby;
    public IntercourseTimeOfDay intercourseTimeOfDay;
    public String pocSummary;
    public EntryModificationContext modificationContext;
    public Set<AbstractInstruction> fertilityReasons = new HashSet<>();
    public Map<AbstractInstruction, AbstractInstruction> suppressedFertilityReasons = new HashMap<>();
    public Map<AbstractInstruction, AbstractInstruction> relaxedInfertilityReasons = new HashMap<>();
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
