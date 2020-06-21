package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.models.charting.DemoCycles;
import com.bloomcyclecare.cmcc.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.models.instructions.BasicInstruction;
import com.google.common.collect.ImmutableList;

import org.junit.Test;

public class PostPeakYellowStampRendererTest extends BaseRendererTest {

  @Test
  public void testYellowPostPeakK2() throws Exception {
    runPostPeakTest(BasicInstruction.K_2);
  }

  @Test
  public void testYellowPostPeakK3() throws Exception {
    runPostPeakTest(BasicInstruction.K_3);
  }

  @Test
  public void testYellowPostPeakK4() throws Exception {
    runPostPeakTest(BasicInstruction.K_4);
  }

  private void runPostPeakTest(BasicInstruction postPeakInstruction) throws Exception {
    Instructions instructions =
        createInstructions(ImmutableList.<BasicInstruction>builder()
            .addAll(BASIC_INSTRUCTIONS.activeItems)
            .add(postPeakInstruction)
            .build(), ImmutableList.of(), ImmutableList.of());
    TrainingCycle cycle = TrainingCycle.withInstructions(instructions)
        .addEntriesFrom(DemoCycles.POST_PEAK_YELLOW_STAMPS);
    runTest(cycle);
  }
}