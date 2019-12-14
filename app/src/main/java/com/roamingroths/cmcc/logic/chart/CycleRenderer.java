package com.roamingroths.cmcc.logic.chart;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
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

  public RenderableCycle render() {
    TreeSet<LocalDate> entriesEvaluated = new TreeSet<>();
    TreeSet<LocalDate> daysOfFlow = new TreeSet<>();
    Set<LocalDate> daysOfMucus = new HashSet<>();
    TreeSet<LocalDate> daysOfUnusualBleeding = new TreeSet<>();
    TreeSet<LocalDate> peakDays = new TreeSet<>();
    TreeSet<LocalDate> pointsOfChangeToward = new TreeSet<>();
    TreeSet<LocalDate> pointsOfChangeAway = new TreeSet<>();
    Map<LocalDate, Boolean> daysOfIntercourse = new HashMap<>();
    LocalDate mostRecentPeakTypeMucus = null;
    LocalDate lastDayOfThreeOrMoreDaysOfMucus = null;
    int consecutiveDaysOfMucus = 0;
    ChartEntry previousEntry = null;
    boolean hasHadLegitFlow = false;

    RenderableCycle renderableCycle = new RenderableCycle(mCycle);

    // For every day before the current entry...
    for (ChartEntry e : mEntries) {
      entriesEvaluated.add(e.entryDate);
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
          : Optional.of(pointsOfChangeToward.first());
      state.mostRecentPointOfChangeToward = pointsOfChangeToward.isEmpty() ? Optional.absent()
          : Optional.of(pointsOfChangeToward.last());
      state.mostRecentPointOfChangeAway = pointsOfChangeAway.isEmpty() ? Optional.absent()
          : Optional.of(pointsOfChangeAway.last());
      if (e.observationEntry.unusualBleeding) {
        daysOfUnusualBleeding.add(e.entryDate);
      }
      if (e.observationEntry.hasMucus()) {
        daysOfMucus.add(e.entryDate);
      }
      daysOfIntercourse.put(e.entryDate, e.observationEntry.intercourseTimeOfDay != IntercourseTimeOfDay.NONE);
      boolean todayHasMucus = false;
      state.todayHasBlood = false;
      state.todaysFlow = null;
      if (e.observationEntry.observation == null) {
        consecutiveDaysOfMucus = 0;
      } else {
        Observation observation = e.observationEntry.observation;
        todayHasMucus = observation.hasMucus();
        state.todayHasBlood = observation.dischargeSummary != null && observation.dischargeSummary.hasBlood();
        if (observation.flow != null) {
          state.todaysFlow = observation.flow;
          hasHadLegitFlow |= observation.flow.isLegit();
        }
        if (todayHasMucus) {
          consecutiveDaysOfMucus++;
          if (consecutiveDaysOfMucus >= 3) {
            lastDayOfThreeOrMoreDaysOfMucus = e.entryDate;
          }
          if (observation.dischargeSummary.isPeakType()) {
            mostRecentPeakTypeMucus = e.entryDate;
          }
        } else {
          consecutiveDaysOfMucus = 0;
        }
      }
      state.hasHadLegitFlow = hasHadLegitFlow;
      if (state.todayHasBlood || state.todaysFlow != null) {
        daysOfFlow.add(e.entryDate);
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
      if (!pointsOfChangeAway.isEmpty()) {
        state.countsOfThree.put(
            CountOfThreeReason.POINT_OF_CHANGE,
            Days.daysBetween(pointsOfChangeAway.last().minusDays(1), e.entryDate).getDays());
      }

      state.isInMenstrualFlow = entriesEvaluated.size() == daysOfFlow.size();
      state.allPreviousDaysHaveHadBlood =
          entriesEvaluated.headSet(yesterday).size() == daysOfFlow.headSet(yesterday).size();

      // Step 2: Evaluate fertility reasons
      Instructions instructions = null;
      for (Instructions i : mInstructions.descendingSet()) {
        if (!e.entryDate.isBefore(i.startDate)) {
          instructions = i;
          break;
        }
      }
      state.instructions = Optional.fromNullable(instructions).or(new Instructions(e.entryDate, ImmutableList.of(), ImmutableList.of(), ImmutableList.of()));

      // Basic Instruction fertility reasons (section D)
      if (state.instructions.isActive(BasicInstruction.D_1)
          && state.isInMenstrualFlow) {
        state.fertilityReasons.add(BasicInstruction.D_1);
      }
      if (state.instructions.isActive(BasicInstruction.D_2)
          && todayHasMucus
          && !state.isPostPeakPlus(3)) {
        state.fertilityReasons.add(BasicInstruction.D_2);
        state.countOfThreeReasons.put(BasicInstruction.D_2, CountOfThreeReason.PEAK_DAY);
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
        state.countOfThreeReasons.put(BasicInstruction.D_4, CountOfThreeReason.CONSECUTIVE_DAYS_OF_MUCUS);
      }
      if (state.instructions.isActive(BasicInstruction.D_5)
          && state.isWithinCountOfThree(CountOfThreeReason.PEAK_TYPE_MUCUS)) {
        state.fertilityReasons.add(BasicInstruction.D_5);
        state.countOfThreeReasons.put(BasicInstruction.D_5, CountOfThreeReason.PEAK_TYPE_MUCUS);
      }
      if (state.instructions.isActive(BasicInstruction.D_6)
          && state.isWithinCountOfThree(CountOfThreeReason.UNUSUAL_BLEEDING)) {
        state.fertilityReasons.add(BasicInstruction.D_6);
        state.countOfThreeReasons.put(BasicInstruction.D_6, CountOfThreeReason.UNUSUAL_BLEEDING);
      }

      // Basic Instruction infertility reasons (section E)
      if (!todayHasMucus && !state.isInMenstrualFlow && state.isPrePeak()) {
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
      if (!todayHasMucus && state.isPostPeakPlus(4)) {
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
      if (!todayHasMucus && state.isInMenstrualFlow && (
          (state.todaysFlow != null && !state.todaysFlow.isLegit()) || state.todayHasBlood)) {
        state.infertilityReasons.add(BasicInstruction.E_7);
      }

      // Basic Instruction yellow stamp reasons (section K)
      Optional<LocalDate> effectivePointOfChange = effectivePointOfChange(pointsOfChangeToward, pointsOfChangeAway);
      if (state.instructions.isActive(BasicInstruction.K_1)
          && state.isPrePeak()
          && !state.isInMenstrualFlow
          && (!effectivePointOfChange.isPresent()
          || state.entryDate.isBefore(effectivePointOfChange.get()))) {
        state.suppressBasicInstructions(BasicInstruction.suppressableByPrePeakYellow, BasicInstruction.K_1);
      }
      if (state.isPostPeakPlus(4)) {
        if (state.instructions.isActive(BasicInstruction.K_2)) {
          state.suppressBasicInstructions(BasicInstruction.suppressableByPostPeakYellow, BasicInstruction.K_2);
        }
        if (state.instructions.isActive(BasicInstruction.K_3)) {
          state.suppressBasicInstructions(BasicInstruction.suppressableByPostPeakYellow, BasicInstruction.K_3);
        }
        if (state.instructions.isActive(BasicInstruction.K_4)) {
          state.suppressBasicInstructions(BasicInstruction.suppressableByPostPeakYellow, BasicInstruction.K_4);
        }
      }

      // Special Instruction Yellow Stamp fertility reasons (section 1)
      if (state.instructions.isActive(YellowStampInstruction.YS_1_A)
          && state.isInMenstrualFlow) {
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
        state.countOfThreeReasons.put(YellowStampInstruction.YS_1_B, CountOfThreeReason.PEAK_DAY);
      }
      if (state.instructions.isActive(YellowStampInstruction.YS_1_C)
          && !pointsOfChangeAway.isEmpty()
          // This last condition should check for +3 days but count starts on the Poc...?
          && !state.entryDate.isAfter(pointsOfChangeAway.last().plusDays(2))) {
        state.fertilityReasons.add(YellowStampInstruction.YS_1_C);
        state.countOfThreeReasons.put(YellowStampInstruction.YS_1_C, CountOfThreeReason.POINT_OF_CHANGE);
      }
      if (state.instructions.isActive(YellowStampInstruction.YS_1_D)
          && state.isWithinCountOfThree(CountOfThreeReason.UNUSUAL_BLEEDING)) {
        state.fertilityReasons.add(YellowStampInstruction.YS_1_D);
        state.countOfThreeReasons.put(YellowStampInstruction.YS_1_D, CountOfThreeReason.UNUSUAL_BLEEDING);
      }

      // Special Instruction Yellow Stamp infertility reasons (section 2)
      if (state.instructions.isActive(YellowStampInstruction.YS_2_A)
          && state.isPrePeak()
          && !state.isInMenstrualFlow
          && (!effectivePointOfChange.isPresent()
          || state.entryDate.isBefore(effectivePointOfChange.get()))) {
        state.suppressBasicInstructions(BasicInstruction.suppressableByPrePeakYellow, YellowStampInstruction.YS_2_A);
      }
      if (state.isPostPeakPlus(4)) {
        if (state.instructions.isActive(YellowStampInstruction.YS_2_B)) {
          state.suppressBasicInstructions(BasicInstruction.suppressableByPostPeakYellow, YellowStampInstruction.YS_2_B);
        }
        if (state.instructions.isActive(YellowStampInstruction.YS_2_C)) {
          state.suppressBasicInstructions(BasicInstruction.suppressableByPostPeakYellow, YellowStampInstruction.YS_2_C);
        }
        if (state.instructions.isActive(YellowStampInstruction.YS_2_D)) {
          state.suppressBasicInstructions(BasicInstruction.suppressableByPostPeakYellow, YellowStampInstruction.YS_2_D);
        }
      }

      // Super special infertility instructions...
      if (state.instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
          && Optional.fromNullable(state.previousEntry).transform(pe -> pe.observationEntry.intercourse).or(false)
          && state.entry.observationEntry.observation != null
          && state.entry.observationEntry.observation.dischargeSummary.isPeakType()
          && state.entry.observationEntry.isEssentiallyTheSame) {
        state.suppressBasicInstructions(BasicInstruction.suppressableByPrePeakYellow, SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);
      }

      for (Map.Entry<AbstractInstruction, CountOfThreeReason> mapEntry : state.countOfThreeReasons.entrySet()) {
        Optional<Integer> count = state.getCount(mapEntry.getValue());
        if (!count.isPresent()) {
          continue;
        }
        if (state.effectiveCountOfThree.first == null || count.get() < state.effectiveCountOfThree.first) {
          state.effectiveCountOfThree = Pair.create(count.get(), mapEntry.getKey());
        }
      }
      renderableCycle.entries.add(RenderableEntry.fromState(state));
      previousEntry = e;
    }

    renderableCycle.stats.mcs = MccScorer.getScore(mEntries, peakDays.isEmpty() ?
        Optional.absent() : Optional.of(peakDays.last()));
    if (!peakDays.isEmpty()) {
      renderableCycle.stats.daysPrePeak = Days.daysBetween(mCycle.startDate, peakDays.last()).getDays();
      if (mCycle.endDate != null) {
        renderableCycle.stats.daysPostPeak = Days.daysBetween(peakDays.last(), mCycle.endDate).getDays();
      }
    }
    return renderableCycle;
  }

  private static Optional<LocalDate> effectivePointOfChange(TreeSet<LocalDate> toward, TreeSet<LocalDate> away) {
    if (toward.isEmpty() || toward.size() == away.size()) {
      return Optional.absent();
    }
    return Optional.of(toward.last());
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
    public boolean hasHadLegitFlow;
    public boolean allPreviousDaysHaveHadBlood;
    public Flow todaysFlow;
    public boolean todayHasBlood;
    public Optional<LocalDate> firstPointOfChangeToward;
    public Optional<LocalDate> mostRecentPointOfChangeToward;
    public Optional<LocalDate> mostRecentPointOfChangeAway;
    public boolean hasHadAnyMucus;
    public int consecutiveDaysOfMucus;
    public boolean hadIntercourseYesterday;
    public Map<CountOfThreeReason, Integer> countsOfThree = new HashMap<>();

    public Set<AbstractInstruction> fertilityReasons = new HashSet<>();
    public Map<AbstractInstruction, AbstractInstruction> suppressedFertilityReasons = new HashMap<>();
    public Set<AbstractInstruction> infertilityReasons = new HashSet<>();

    public Map<AbstractInstruction, CountOfThreeReason> countOfThreeReasons = new HashMap<>();
    public Pair<Integer, AbstractInstruction> effectiveCountOfThree = Pair.create(null, null);
    public ChartEntry previousEntry;

    boolean isPrePeak() {
      return !firstPeakDay.isPresent() || entryDate.isBefore(firstPeakDay.get());
    }

    public boolean isPeakDay() {
      return mostRecentPeakDay.isPresent() && mostRecentPeakDay.get().equals(entryDate);
    }

    boolean isPostPeak() {
      return isPostPeakPlus(0);
    }

    boolean isPostPeakPlus(int numDays) {
      return mostRecentPeakDay.isPresent() && entryDate.isAfter(mostRecentPeakDay.get().plusDays(numDays));
    }

    boolean isExactlyPostPeakPlus(int numDays) {
      return mostRecentPeakDay.isPresent() && entryDate.equals(mostRecentPeakDay.get().plusDays(numDays));
    }

    boolean isPocTowardFertility() {
      return mostRecentPointOfChangeToward.isPresent() && mostRecentPointOfChangeToward.get().equals(entryDate);
    }

    boolean isPocAwayFromFertility() {
      return mostRecentPointOfChangeAway.isPresent() && mostRecentPointOfChangeAway.get().equals(entryDate);
    }

    public Optional<Integer> getCount(CountOfThreeReason reason) {
      return Optional.fromNullable(countsOfThree.get(reason));
    }

    boolean isWithinCountOfThree(CountOfThreeReason reason) {
      Optional<Integer> count = getCount(reason);
      return count.isPresent() && count.get() < 4;
    }

    void suppressBasicInstructions(Collection<BasicInstruction> instructionsToSuppress,
                                          AbstractInstruction suppressionReason) {
      for (BasicInstruction instruction : instructionsToSuppress) {
        if (fertilityReasons.remove(instruction)) {
          countOfThreeReasons.remove(instruction);
          suppressedFertilityReasons.put(instruction, suppressionReason);
        }
      }
      infertilityReasons.add(suppressionReason);
    }

    String peakDayText() {
      if (isPeakDay()) {
        return "P";
      }
      if (fertilityReasons.isEmpty()) {
        return "";
      }
      if (effectiveCountOfThree.first == null || effectiveCountOfThree.first == 0) {
        return "";
      }
      if (effectiveCountOfThree.first > 0) {
        return String.valueOf(effectiveCountOfThree.first);
      }
      return "";
    }

    String getInstructionSummary() {
      if (entry.observationEntry.observation == null) {
        return "Please provide an observation by clicking edit below.";
      }
      List<String> instructionSummaryLines = new ArrayList<>();
      List<String> subsectionLines = new ArrayList<>();
      instructionSummaryLines.add(String.format("Status: %s",
          fertilityReasons.isEmpty() ? "Infertile" : "Fertile"));
      if (!fertilityReasons.isEmpty()) {
        subsectionLines.add("Fertility Reasons:");
        for (AbstractInstruction i : fertilityReasons) {
          subsectionLines.add(String.format(" - %s", AbstractInstruction.summary(i)));
        }
        instructionSummaryLines.add(ON_NEW_LINE.join(subsectionLines));
        subsectionLines.clear();
      }
      if (!infertilityReasons.isEmpty()) {
        subsectionLines.add("Infertility Reasons:");
        for (AbstractInstruction i : infertilityReasons) {
          subsectionLines.add(String.format(" - %s", AbstractInstruction.summary(i)));
        }
        instructionSummaryLines.add(ON_NEW_LINE.join(subsectionLines));
        subsectionLines.clear();
      }
      if (!suppressedFertilityReasons.isEmpty()) {
        subsectionLines.add("Impact of Yellow Stamps:");
        for (Map.Entry<AbstractInstruction, AbstractInstruction> e : suppressedFertilityReasons.entrySet()) {
          subsectionLines.add(String.format(" - %s inhibited by %s",
              AbstractInstruction.summary(e.getKey()), AbstractInstruction.summary(e.getValue())));
        }
        instructionSummaryLines.add(ON_NEW_LINE.join(subsectionLines));
        subsectionLines.clear();
      }
      if (!subsectionLines.isEmpty()) {
        Timber.w("Leaking strings!");
      }
      return ON_DOUBLE_NEW_LINE.join(instructionSummaryLines);
    }

    StickerColor getBackgroundColor() {
      if (instructions == null) {
        return StickerColor.GREY;
      }
      Observation observation = entry.observationEntry.observation;
      if (observation == null) {
        return StickerColor.GREY;
      }
      if (observation.flow != null) {
        return StickerColor.RED;
      }
      if (observation.dischargeSummary.mModifiers.contains(DischargeSummary.MucusModifier.B)) {
        return StickerColor.RED;
      }
      if (!observation.hasMucus()) {
        return StickerColor.GREEN;
      }
      // All entries have mucus at this point
      if (!infertilityReasons.isEmpty()) {
        return StickerColor.YELLOW;
      }
      if (instructions.anyActive(BasicInstruction.K_2, BasicInstruction.K_3, BasicInstruction.K_4)
          && isPostPeak() && !isPostPeakPlus(4)) {
        return StickerColor.YELLOW;
      }
      return StickerColor.WHITE;
    }

    boolean shouldAskEssentialSameness() {
      if (instructions == null) {
        return false;
      }
      if (instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
          && Optional.fromNullable(previousEntry).transform(e -> e.observationEntry.intercourse).or(false)) {
        return true;
      }
      if (instructions.isActive(BasicInstruction.K_1) && isPrePeak() && (!isInMenstrualFlow || hasHadLegitFlow)) {
        return true;
      }
      return false;
    }

    boolean shouldAskDoublePeakQuestions() {
      return instructions.isActive(BasicInstruction.G_1) && isExactlyPostPeakPlus(3);
    }

    boolean shouldShowBaby() {
      if (fertilityReasons.isEmpty()) {
        return infertilityReasons.isEmpty();
      }
      return !entry.observationEntry.hasBlood();
    }

    EntryModificationContext entryModificationContext() {
      EntryModificationContext modificationContext = new EntryModificationContext();
      modificationContext.cycle = cycle;
      modificationContext.entry = entry;
      modificationContext.hasPreviousCycle = false;
      modificationContext.allPreviousDaysHaveHadBlood = allPreviousDaysHaveHadBlood;
      modificationContext.isFirstEntry = entryNum == 1;
      modificationContext.shouldAskEssentialSamenessIfMucus = shouldAskEssentialSameness();
      modificationContext.shouldAskDoublePeakQuestions = shouldAskDoublePeakQuestions();
      return modificationContext;
    }
  }

  public enum CountOfThreeReason {
    UNUSUAL_BLEEDING, PEAK_DAY, CONSECUTIVE_DAYS_OF_MUCUS, PEAK_TYPE_MUCUS, POINT_OF_CHANGE;
  }

  public static class RenderableCycle {
    public final List<RenderableEntry> entries = new ArrayList<>();
    public final CycleStats stats;

    public RenderableCycle(Cycle cycle) {
      stats = new CycleStats(cycle.startDate);
    }
  }

  public static class CycleStats implements Comparable<CycleStats> {
    public final LocalDate cycleStartDate;
    public Float mcs = null;
    public Integer daysPrePeak = null;
    public Integer daysPostPeak = null;

    public CycleStats(@NonNull LocalDate cycleStartDate) {
      this.cycleStartDate = cycleStartDate;
    }

    @Override
    public int compareTo(CycleStats other) {
      return cycleStartDate.compareTo(other.cycleStartDate);
    }
  }

  public static class RenderableEntry {
    public String entrySummary;
    public StickerColor backgroundColor;
    public int entryNum;
    public String dateSummary;
    public String peakDayText;
    public String instructionSummary;
    public String essentialSamenessSummary;
    public boolean showBaby;
    public IntercourseTimeOfDay intercourseTimeOfDay;
    public String pocSummary;
    public EntryModificationContext modificationContext;

    // TODO: add EoD / any time of day accounting for double peak Q's

    public static RenderableEntry fromState(State state) {
      RenderableEntry renderableEntry = new RenderableEntry();

      renderableEntry.entryNum = state.entryNum;
      renderableEntry.dateSummary = DateUtil.toNewUiStr(state.entry.entryDate);
      renderableEntry.entrySummary = state.entry.observationEntry.getListUiText();
      renderableEntry.backgroundColor = state.getBackgroundColor();
      renderableEntry.showBaby = state.shouldShowBaby();
      renderableEntry.peakDayText = state.peakDayText();
      renderableEntry.intercourseTimeOfDay = state.entry.observationEntry.intercourseTimeOfDay;
      if (state.isPocTowardFertility()) {
        renderableEntry.pocSummary = "POC↑";
      } else if (state.isPocAwayFromFertility()) {
        renderableEntry.pocSummary = "POC↓";
      } else {
        renderableEntry.pocSummary = "";
      }
      renderableEntry.instructionSummary = state.getInstructionSummary();
      renderableEntry.modificationContext = state.entryModificationContext();
      if (renderableEntry.modificationContext.shouldAskEssentialSamenessIfMucus
          && state.entry.observationEntry.hasMucus()) {
        renderableEntry.essentialSamenessSummary = state.entry.observationEntry.isEssentiallyTheSame ? "yes" : "no";
      } else {
        renderableEntry.essentialSamenessSummary = "";
      }

      return renderableEntry;
    }

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
    public boolean isFirstEntry;
    public boolean shouldAskDoublePeakQuestions;
    public boolean allPreviousDaysHaveHadBlood;
    public boolean shouldAskEssentialSamenessIfMucus;
  }
}
