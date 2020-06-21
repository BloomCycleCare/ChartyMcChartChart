package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.models.charting.DemoCycles;

import org.junit.Test;

public class PrePeakYellowStampRendererTest extends BaseRendererTest {

  @Test
  public void testYellowPrePeakExample() throws Exception {
    runTest(DemoCycles.PRE_PEAK_YELLOW_STAMPS);
  }

  @Test
  public void testYellowPrePeakBreastFeeding() throws Exception {
    runTest(DemoCycles.BREASTFEEDING_PRE_PEAK_YELLOW_STAMPS);
  }
}
