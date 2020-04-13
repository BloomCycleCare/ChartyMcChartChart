package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;

public class TrainingCycles {

  private static final String REGULAR_CYCLES_TITLE = "Regular Cycles";

  public static TrainingCycle REGULAR_CYCLES_A = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .withTitle(REGULAR_CYCLES_TITLE)
      .withSubtitle("A")
      .addEntry(TrainingEntry.forText("H"))
      .addEntry(TrainingEntry.forText("H"))
      .addEntry(TrainingEntry.forText("M"))
      .addEntry(TrainingEntry.forText("M"))
      .addEntry(TrainingEntry.forText("L0AD"))
      .addEntry(TrainingEntry.forText("L0AD"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("2AD"))
      .addEntry(TrainingEntry.forText("4X1", "A"));

  public static TrainingCycle REGULAR_CYCLES_B = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .withTitle(REGULAR_CYCLES_TITLE)
      .withSubtitle("B")
      .addEntry(TrainingEntry.forText("H"))
      .addEntry(TrainingEntry.forText("M"))
      .addEntry(TrainingEntry.forText("L0AD"))
      .addEntry(TrainingEntry.forText("L0AD"))
      .addEntry(TrainingEntry.forText("VL0AD"))
      .addEntry(TrainingEntry.forText("2X1"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("8CX1"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("2X1"))
      .addEntry(TrainingEntry.forText("2X1"))
      .addEntry(TrainingEntry.forText("8KX2"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("0AD", "B"));

  public static TrainingCycle REGULAR_CYCLES_C = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .withTitle(REGULAR_CYCLES_TITLE)
      .withSubtitle("C")
      .addEntry(TrainingEntry.forText("H"))
      .addEntry(TrainingEntry.forText("H"))
      .addEntry(TrainingEntry.forText("M"))
      .addEntry(TrainingEntry.forText("M", "C"));

  public static TrainingCycle REGULAR_CYCLES_D = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .withTitle(REGULAR_CYCLES_TITLE)
      .withSubtitle("D")
      .addEntry(TrainingEntry.forText("H"))
      .addEntry(TrainingEntry.forText("H"))
      .addEntry(TrainingEntry.forText("M"))
      .addEntry(TrainingEntry.forText("M"))
      .addEntry(TrainingEntry.forText("L0AD"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("6PCX2"))
      .addEntry(TrainingEntry.forText("0AD"))
      .addEntry(TrainingEntry.forText("4X1"))
      .addEntry(TrainingEntry.forText("8CX1"))
      .addEntry(TrainingEntry.forText("8CX2"))
      .addEntry(TrainingEntry.forText("10KLAD"))
      .addEntry(TrainingEntry.forText("10WLX2"))
      .addEntry(TrainingEntry.forText("0AD", "D"));

  public static List<TrainingCycle> REGULAR_CYCLES = ImmutableList.of(
      REGULAR_CYCLES_A, REGULAR_CYCLES_B, REGULAR_CYCLES_C, REGULAR_CYCLES_D);
}
