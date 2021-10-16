package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.instructions.AbstractInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.InstructionSet;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.instructions.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.models.measurement.MonitorReading;
import com.bloomcyclecare.cmcc.data.models.observation.Flow;
import com.bloomcyclecare.cmcc.data.models.observation.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import timber.log.Timber;

public class CycleRenderer {

  private static final Joiner ON_NEW_LINE = Joiner.on("\n");
  private static final Joiner ON_DOUBLE_NEW_LINE = Joiner.on("\n\n");

  private final Cycle mCycle;
  private final Optional<Cycle> mPreviousCycle;
  private final TreeSet<ChartEntry> mEntries;
  private final TreeSet<Instructions> mInstructions;

  public CycleRenderer(Cycle cycle, Optional<Cycle> previousCycle, Collection<ChartEntry> entries, Collection<Instructions> allInstructions) {
    mCycle = cycle;
    mPreviousCycle = previousCycle;
    mEntries = new TreeSet<>((a, b) -> a.entryDate.compareTo(b.entryDate));
    mEntries.addAll(entries);
    mInstructions = new TreeSet<>((a, b) -> a.startDate.compareTo(b.startDate));
    mInstructions.addAll(allInstructions);
  }

  public Cycle cycle() {
    return mCycle;
  }

  public RenderableCycle render() {
    long renderStartMs = System.currentTimeMillis();
    try {
      Timber.v("Rendering cycle starting %s", mCycle.startDate);

      Set<LocalDate> daysWithAnObservation = new HashSet<>();
      TreeSet<LocalDate> entriesEvaluated = new TreeSet<>();
      TreeSet<LocalDate> daysOfFlow = new TreeSet<>();
      Set<LocalDate> daysOfMucus = new HashSet<>();
      TreeSet<LocalDate> daysOfUnusualBleeding = new TreeSet<>();
      TreeSet<LocalDate> daysOfUncertainty = new TreeSet<>();
      TreeSet<LocalDate> peakDays = new TreeSet<>();
      TreeSet<LocalDate> pointsOfChangeToward = new TreeSet<>();
      TreeSet<LocalDate> pointsOfChangeAway = new TreeSet<>();
      Map<LocalDate, Boolean> daysOfIntercourse = new HashMap<>();
      LocalDate mostRecentPeakTypeMucus = null;
      LocalDate lastDayOfThreeOrMoreDaysOfMucus = null;
      int consecutiveDaysOfNonPeakMucus = 0;
      ChartEntry previousEntry = null;
      boolean hasHadLegitFlow = false;

      List<RenderableEntry> renderableEntries = new ArrayList<>(mEntries.size());

      // For every day before the current entry...
      for (ChartEntry e : mEntries) {
        entriesEvaluated.add(e.entryDate);
        LocalDate yesterday = e.entryDate.minusDays(1);

        State state = new State();
        state.cycle = mCycle;
        state.previousCycle = mPreviousCycle;
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
        state.firstPointOfChangeToward = pointsOfChangeToward.isEmpty() ? Optional.empty()
            : Optional.of(pointsOfChangeToward.first());
        state.mostRecentPointOfChangeToward = pointsOfChangeToward.isEmpty() ? Optional.empty()
            : Optional.of(pointsOfChangeToward.last());
        state.mostRecentPointOfChangeAway = pointsOfChangeAway.isEmpty() ? Optional.empty()
            : Optional.of(pointsOfChangeAway.last());
        if (e.observationEntry.hasMucus()) {
          daysOfMucus.add(e.entryDate);
        }
        daysOfIntercourse.put(e.entryDate, e.observationEntry.intercourseTimeOfDay != IntercourseTimeOfDay.NONE);
        state.todayHasMucus = false;
        state.todayHasBlood = false;
        state.todaysFlow = null;
        if (e.observationEntry.observation == null) {
          consecutiveDaysOfNonPeakMucus = 0;
        } else {
          daysWithAnObservation.add(e.entryDate);
          Observation observation = e.observationEntry.observation;
          state.todayHasMucus = observation.hasMucus();
          boolean hasNonPeakMucus = observation.hasMucus() && !observation.dischargeSummary.isPeakType();
          if (observation.flow != null) {
            state.todaysFlow = observation.flow;
            hasHadLegitFlow |= observation.flow.isLegit();
          }
          state.todayHasBlood = state.todaysFlow != null
              || observation.dischargeSummary != null && observation.dischargeSummary.hasBlood();
          if (hasNonPeakMucus) {
            consecutiveDaysOfNonPeakMucus++;
            if (consecutiveDaysOfNonPeakMucus >= 3) {
              lastDayOfThreeOrMoreDaysOfMucus = e.entryDate;
            }
          } else {
            consecutiveDaysOfNonPeakMucus = 0;
            if (state.todayHasMucus) {
              mostRecentPeakTypeMucus = e.entryDate;
            }
          }
        }
        state.hasHadLegitFlow = hasHadLegitFlow;
        if (state.todayHasBlood || state.todaysFlow != null) {
          daysOfFlow.add(e.entryDate);
        }
        state.isInMenstrualFlow = entriesEvaluated.size() == daysOfFlow.size();
        if (!state.isInMenstrualFlow && state.todayHasBlood && !e.observationEntry.isEssentiallyTheSame) {
          daysOfUnusualBleeding.add(e.entryDate);
        }
        if (e.observationEntry.uncertain) {
          daysOfUncertainty.add(e.entryDate);
        }
        if (peakDays.isEmpty()) {
          state.firstPeakDay = Optional.empty();
          state.mostRecentPeakDay = Optional.empty();
        } else {
          state.firstPeakDay = Optional.of(peakDays.first());
          state.mostRecentPeakDay = Optional.of(peakDays.last());
        }
        state.hasHadAnyMucus = !daysOfMucus.isEmpty();
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
        Optional<LocalDate> effectivePointOfChange = effectivePointOfChange(pointsOfChangeToward, pointsOfChangeAway);
        if (effectivePointOfChange.isPresent()) {
          Integer existingCount = state.countsOfThree.get(CountOfThreeReason.POINT_OF_CHANGE);
          int count = Days.daysBetween(effectivePointOfChange.get(), e.entryDate).getDays();
          if (existingCount == null || count < existingCount) {
            state.countsOfThree.put(CountOfThreeReason.POINT_OF_CHANGE, count);
          }
        }
        if (!daysOfUncertainty.isEmpty()) {
          state.countsOfThree.put(
              CountOfThreeReason.UNCERTAIN,
              Days.daysBetween(daysOfUncertainty.last(), e.entryDate).getDays());
        }

        // Step 2: Evaluate fertility reasons
        Instructions instructions = null;
        for (Instructions i : mInstructions.descendingSet()) {
          if (!e.entryDate.isBefore(i.startDate)) {
            instructions = i;
            break;
          }
        }
        state.instructions = Optional.ofNullable(instructions).orElse(new Instructions(e.entryDate, ImmutableList.of(), ImmutableList.of(), ImmutableList.of()));

        // Basic Instruction fertility reasons (section D)
        if (state.instructions.isActive(BasicInstruction.D_1)
            && state.isInMenstrualFlow) {
          state.fertilityReasons.add(BasicInstruction.D_1);
        }
        if (state.instructions.isActive(BasicInstruction.D_2)
            && state.todayHasMucus
            && !state.isPostPeakPlus(3)) {
          state.fertilityReasons.add(BasicInstruction.D_2);
          state.countOfThreeReasons.put(BasicInstruction.D_2, CountOfThreeReason.PEAK_DAY);
        }
        if (state.instructions.isActive(BasicInstruction.D_3)
            && state.isPrePeak()
            && consecutiveDaysOfNonPeakMucus > 0
            && consecutiveDaysOfNonPeakMucus < 3) {
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
        if (!state.todayHasMucus && !state.isInMenstrualFlow && state.isPrePeak()) {
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
        if (!state.todayHasMucus && state.isPostPeakPlus(4)) {
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
        if (!state.todayHasMucus && state.isInMenstrualFlow && (
            (state.todaysFlow != null && !state.todaysFlow.isLegit()) || state.todayHasBlood)) {
          state.infertilityReasons.add(BasicInstruction.E_7);
        }
        if (state.instructions.isActive(BasicInstruction.H)
            && state.isWithinCountOfThree(CountOfThreeReason.UNCERTAIN)) {
          state.fertilityReasons.add(BasicInstruction.H);
          state.countOfThreeReasons.put(BasicInstruction.H, CountOfThreeReason.UNCERTAIN);
        }
        // Basic Instruction yellow stamp reasons (section K)
        if (state.instructions.isActive(BasicInstruction.K_1)
            && state.isPrePeak()
            && !state.isInMenstrualFlow
            && (!effectivePointOfChange.isPresent()
            || state.entryDate.isBefore(effectivePointOfChange.get()))) {
          state.suppressBasicInstructions(BasicInstruction.suppressableByPrePeakYellow, BasicInstruction.K_1);
        }
        if (state.isPostPeak()) {
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
            && Optional.ofNullable(state.countsOfThree.get(CountOfThreeReason.POINT_OF_CHANGE)).map(c -> c < 4).orElse(false)
            ) {
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
        if (state.instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)) {
          if (state.hadIntercourseYesterday
              && state.entry.observationEntry.observation != null
              && state.entry.observationEntry.observation.dischargeSummary.isPeakType()
              && state.entry.observationEntry.isEssentiallyTheSame) {
            state.suppressBasicInstructions(BasicInstruction.suppressableByPrePeakYellow, SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);
          }
          if (state.countOfThreeReasons.containsKey(BasicInstruction.D_5)) {
            int count = state.countsOfThree.get(state.countOfThreeReasons.get(BasicInstruction.D_5));
            int previousIndex = entriesEvaluated.size() - count - 2;
            if (previousIndex >= 0) {
              ChartEntry pe = Iterators.get(mEntries.iterator(), previousIndex);
              ChartEntry cse = Iterators.get(mEntries.iterator(), previousIndex + 1);
              if (pe.observationEntry.intercourse && cse.observationEntry.isEssentiallyTheSame) {
                state.suppressBasicInstructions(BasicInstruction.suppressableByPrePeakYellow, SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);
              }
            }
          }
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
        renderableEntries.add(RenderableEntry.fromState(state));
        previousEntry = e;
      }

      CycleStats.Builder statsBuilder = CycleStats.builder()
          .cycleStartDate(mCycle.startDate)
          .isPregnancy(mCycle.isPregnancy())
          .daysWithAnObservation(daysWithAnObservation.size())
          .daysOfFlow(daysOfFlow.size())
          .mcs(MccScorer.getScore(mEntries, peakDays.isEmpty() ? Optional.empty() : Optional.of(peakDays.last())));
      if (!peakDays.isEmpty()) {
        statsBuilder.daysPrePeak(Optional.of(Days.daysBetween(mCycle.startDate, peakDays.last()).getDays()));
        if (mCycle.endDate != null) {
          statsBuilder.daysPostPeak(Optional.of(Days.daysBetween(peakDays.last(), mCycle.endDate).getDays()));
        }
      }

      effectivePointOfChange(pointsOfChangeToward, pointsOfChangeAway)
          .map(pointOfChange -> Days.daysBetween(mCycle.startDate, pointOfChange).getDays())
          .ifPresent(daysBeforePoC -> statsBuilder.daysBeforePoC(Optional.of(daysBeforePoC)));

      return RenderableCycle.builder()
          .cycle(mCycle)
          .entries(renderableEntries)
          .stats(statsBuilder.build())
          .build();
    } finally {
      Timber.v("Rendered cycle starting %s in %d ms", mCycle.startDate, System.currentTimeMillis() - renderStartMs);
    }
  }

  private static Optional<LocalDate> effectivePointOfChange(TreeSet<LocalDate> toward, TreeSet<LocalDate> away) {
    if (toward.isEmpty() || toward.size() == away.size()) {
      return Optional.empty();
    }
    return Optional.of(toward.last());
  }

  public static class State {
    public Cycle cycle;
    public Optional<Cycle> previousCycle;
    @Deprecated  ChartEntry entry;
    public LocalDate entryDate;
    public Instructions instructions;
    public int entryNum;
    public Optional<LocalDate> firstPeakDay;
    public Optional<LocalDate> mostRecentPeakDay;
    public boolean isInMenstrualFlow;
    public boolean hasHadLegitFlow;
    public Flow todaysFlow;
    public boolean todayHasBlood;
    public Optional<LocalDate> firstPointOfChangeToward;
    public Optional<LocalDate> mostRecentPointOfChangeToward;
    public Optional<LocalDate> mostRecentPointOfChangeAway;
    public boolean todayHasMucus;
    public boolean hasHadAnyMucus;
    public boolean hadIntercourseYesterday;
    public Map<CountOfThreeReason, Integer> countsOfThree = new HashMap<>();

    public InstructionSet fertilityReasons = new InstructionSet();
    public InstructionSet infertilityReasons = new InstructionSet();
    public Map<AbstractInstruction, AbstractInstruction> suppressedFertilityReasons = new HashMap<>();

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
      return Optional.ofNullable(countsOfThree.get(reason));
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


    boolean shouldAskEssentialSameness() {
      if (instructions == null) {
        return false;
      }
      if (instructions.isActive(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS)
          && Optional.ofNullable(previousEntry).map(e -> e.observationEntry.intercourse).orElse(false)) {
        return true;
      }
      if (instructions.isActive(BasicInstruction.J) && isPrePeak() && (!isInMenstrualFlow || hasHadLegitFlow)) {
        return true;
      }
      return false;
    }

    boolean shouldAskDoublePeakQuestions() {
      return instructions.isActive(BasicInstruction.G_1) && isExactlyPostPeakPlus(3);
    }

    EntryModificationContext entryModificationContext() {
      EntryModificationContext modificationContext = new EntryModificationContext(cycle, entry);
      modificationContext.hasPreviousCycle = previousCycle.isPresent();
      modificationContext.previousCycleIsPregnancy = previousCycle.map(Cycle::isPregnancy).orElse(false);
      modificationContext.isFirstEntry = entryNum == 1;
      modificationContext.shouldAskEssentialSameness = shouldAskEssentialSameness();
      modificationContext.shouldAskDoublePeakQuestions = shouldAskDoublePeakQuestions();
      return modificationContext;
    }

    StickerSelectionContext stickerSelectionContext() {
      boolean hasInstructions = instructions != null;
      return new StickerSelectionContext(
          Optional.ofNullable(effectiveCountOfThree).map(p -> p.first).orElse(-1),
          PeakDayOffset.create(mostRecentPeakDay, entryDate),
          hasInstructions,
          entry.hasObservation(),
          todaysFlow != null || todayHasBlood,
          todayHasMucus,
          isInMenstrualFlow,
          hasInstructions && instructions.specialInstructions.size() + instructions.yellowStampInstructions.size() > 0,
          fertilityReasons,
          infertilityReasons);
    }
  }

  public enum CountOfThreeReason {
    UNUSUAL_BLEEDING, PEAK_DAY, CONSECUTIVE_DAYS_OF_MUCUS, PEAK_TYPE_MUCUS, POINT_OF_CHANGE, UNCERTAIN;
  }

  @AutoValue
  public abstract static class RenderableCycle {
    public abstract Cycle cycle();
    public abstract List<RenderableEntry> entries();
    public abstract CycleStats stats();

    public static Builder builder() {
      return new AutoValue_CycleRenderer_RenderableCycle.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder entries(List<RenderableEntry> entries);

      public abstract Builder stats(CycleStats stats);

      public abstract Builder cycle(Cycle cycle);

      public abstract RenderableCycle build();
    }
  }

  @AutoValue
  public abstract static class CycleStats implements Comparable<CycleStats> {
    public abstract LocalDate cycleStartDate();
    public abstract Integer daysWithAnObservation();
    public abstract Integer daysOfFlow();
    public abstract boolean isPregnancy ();
    public abstract Optional<Float> mcs();
    public abstract Optional<Integer> daysPrePeak();
    public abstract Optional<Integer> daysPostPeak();
    public abstract Optional<Integer> daysBeforePoC();

    @Override
    public int compareTo(CycleStats other) {
      return cycleStartDate().compareTo(other.cycleStartDate());
    }

    public static Builder builder() {
      return new AutoValue_CycleRenderer_CycleStats.Builder()
          .mcs(Optional.empty())
          .isPregnancy(false)
          .daysWithAnObservation(0)
          .daysPrePeak(Optional.empty())
          .daysPostPeak(Optional.empty())
          .daysBeforePoC(Optional.empty())
          .daysOfFlow(0);
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder cycleStartDate(LocalDate cycleStartDate);

      public abstract Builder daysWithAnObservation(Integer daysWithAnObservation);

      public abstract Builder isPregnancy(boolean isPregnancy);

      public abstract Builder mcs(Optional<Float> mcs);

      public abstract Builder daysPrePeak(Optional<Integer> days);

      public abstract Builder daysPostPeak(Optional<Integer> days);

      public abstract Builder daysBeforePoC(Optional<Integer> days);

      public abstract Builder daysOfFlow(Integer daysOfFlow);

      public abstract CycleStats build();
    }
  }

  @AutoValue
  public static abstract class RenderableEntry {
    public abstract boolean hasObservation();
    public abstract Optional<String> entrySummary();
    public abstract int entryNum();
    public abstract String dateSummary();
    public abstract String dateSummaryShort();
    public abstract String instructionSummary();
    public abstract String essentialSamenessSummary();
    public abstract IntercourseTimeOfDay intercourseTimeOfDay();
    public abstract String pocSummary();
    public abstract EntryModificationContext modificationContext();
    public abstract String trainingMarker();
    public abstract boolean canSelectYellowStamps();
    public abstract StickerSelectionContext stickerSelectionContext();
    public abstract StickerSelection expectedStickerSelection();
    public abstract Optional<StickerSelection> manualStickerSelection();
    public abstract Optional<MonitorReading> monitorReading();

    // TODO: add EoD / any time of day accounting for double peak Q's

    public static RenderableEntry fromState(State state) {
      String pocSummary;
      if (state.isPocTowardFertility()) {
        pocSummary = "POC↑";
      } else if (state.isPocAwayFromFertility()) {
        pocSummary = "POC↓";
      } else {
        pocSummary = "";
      }
      String essentialSamenessSummary;
      if (state.entryModificationContext().shouldAskEssentialSameness) {
        essentialSamenessSummary = state.entry.observationEntry.isEssentiallyTheSame ? "yes" : "no";
      } else {
        essentialSamenessSummary = "";
      }
      StickerSelectionContext stickerSelectionContext = state.stickerSelectionContext();
      return builder()
          .manualStickerSelection(Optional.ofNullable(state.entry.stickerSelection))
          .hasObservation(state.entry.hasObservation())
          .entryNum(state.entryNum)
          .dateSummary(DateUtil.toNewUiStr(state.entryDate))
          .dateSummaryShort(DateUtil.toPrintUiStr(state.entryDate))
          .entrySummary(state.entry.observationEntry.getListUiText())
          .intercourseTimeOfDay(Optional.ofNullable(state.entry.observationEntry.intercourseTimeOfDay)
              .orElse(IntercourseTimeOfDay.NONE))
          .pocSummary(pocSummary)
          .instructionSummary(state.getInstructionSummary())
          .modificationContext(state.entryModificationContext())
          .essentialSamenessSummary(essentialSamenessSummary)
          .trainingMarker(state.entry.marker)
          .canSelectYellowStamps(
              state.instructions.anyActive(BasicInstruction.yellowBasicInstructions)
              || !state.instructions.yellowStampInstructions.isEmpty()
              || state.instructions.specialInstructions.contains(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS))
          .stickerSelectionContext(stickerSelectionContext)
          .expectedStickerSelection(stickerSelectionContext.expectedSelection())
          .monitorReading(Optional.ofNullable(state.entry.measurementEntry)
              .map(me -> me.monitorReading)
              .flatMap(r -> r == MonitorReading.UNKNOWN ? Optional.empty() : Optional.of(r)))
          .build();
    }

    @NonNull
    @Override
    public String toString() {
      return String.format("%s: %s", dateSummary(), entrySummary());
    }

    public static Builder builder() {
      return new AutoValue_CycleRenderer_RenderableEntry.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder entrySummary(Optional<String> entrySummary);

      public abstract Builder entryNum(int entryNum);

      public abstract Builder dateSummary(String dateSummary);

      public abstract Builder instructionSummary(String instructionSummary);

      public abstract Builder essentialSamenessSummary(String essentialSamenessSummary);

      public abstract Builder intercourseTimeOfDay(IntercourseTimeOfDay intercourseTimeOfDay);

      public abstract Builder pocSummary(String pocSummary);

      public abstract Builder modificationContext(EntryModificationContext modificationContext);

      public abstract Builder trainingMarker(String trainingMarker);

      public abstract Builder dateSummaryShort(String dateSummaryShort);

      public abstract Builder hasObservation(boolean hasObservation);

      public abstract Builder manualStickerSelection(Optional<StickerSelection> manualStickerSelection);

      public abstract Builder canSelectYellowStamps(boolean canSelectYellowStamps);

      public abstract Builder expectedStickerSelection(StickerSelection stickerSelection);

      public abstract Builder stickerSelectionContext(StickerSelectionContext context);

      public abstract Builder monitorReading(Optional<MonitorReading> monitorReading);

      public abstract RenderableEntry build();
    }
  }

  @Parcel
  public static class EntryModificationContext {
    @NonNull
    public Cycle cycle;
    @NonNull
    public ChartEntry entry;
    public boolean hasPreviousCycle;
    public boolean previousCycleIsPregnancy;
    public boolean isFirstEntry;
    public boolean shouldAskDoublePeakQuestions;
    public boolean shouldAskEssentialSameness;

    @ParcelConstructor
    public EntryModificationContext(@NonNull Cycle cycle, @NonNull ChartEntry entry) {
      this.cycle = cycle;
      this.entry = entry;
    }
  }

  @Parcel
  public static class StickerSelectionContext {
    public final int countOfThree;
    public final PeakDayOffset peakDayOffset;
    public final boolean hasInstructions;
    public final boolean hasObservation;
    public final boolean hasBleeding;
    public final boolean hasMucus;
    public final boolean inFlow;
    public final boolean hasSpecialInstructions;
    public final InstructionSet fertilityReasons;
    public final InstructionSet infertilityReasons;

    @ParcelConstructor
    public StickerSelectionContext(int countOfThree, PeakDayOffset peakDayOffset, boolean hasInstructions, boolean hasObservation, boolean hasBleeding, boolean hasMucus, boolean inFlow, boolean hasSpecialInstructions, InstructionSet fertilityReasons, InstructionSet infertilityReasons) {
      this.countOfThree = countOfThree;
      this.peakDayOffset = peakDayOffset;
      this.hasInstructions = hasInstructions;
      this.hasObservation = hasObservation;
      this.hasBleeding = hasBleeding;
      this.hasMucus = hasMucus;
      this.inFlow = inFlow;
      this.hasSpecialInstructions = hasSpecialInstructions;
      this.fertilityReasons = fertilityReasons;
      this.infertilityReasons = infertilityReasons;
    }

    public StickerSelection expectedSelection() {
      return StickerSelection.create(getSticker(), getStickerText());
    }

    private Sticker getSticker() {
      return StickerSelector.select(this).sticker;
    }

    @Nullable
    private StickerText getStickerText() {
      if (!hasObservation) {
        return null;
      }
      if (hasBleeding) {
        return null;
      }
      if (peakDayOffset.isPeakDay()) {
        return StickerText.P;
      }
      if (fertilityReasons.isEmpty()) {
        return null;
      }
      if (countOfThree > 0) {
        return StickerText.fromString(String.valueOf(countOfThree));
      }
      return null;
    }
  }

  @Parcel
  public static class PeakDayOffset {
    final int offset;

    public static PeakDayOffset create(Optional<LocalDate> mostRecentPeakDay, LocalDate entryDate) {
      return new PeakDayOffset(
          mostRecentPeakDay.map(pd -> Days.daysBetween(pd, entryDate).getDays()).orElse(-1));
    }

    @ParcelConstructor
    public PeakDayOffset(int offset) {
      this.offset = offset;
    }

    boolean isPeakDay() {
      return offset == 0;
    }

    boolean isPrePeak() {
      return offset <= 0;
    }

    boolean isPostPeak() {
      return isPostPeakPlusExclusive(0);
    }

    boolean isPostPeakPlusExclusive(int v) {
      return offset > v;
    }
  }
}
