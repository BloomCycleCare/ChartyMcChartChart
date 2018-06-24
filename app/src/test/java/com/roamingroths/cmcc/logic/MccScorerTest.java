package com.roamingroths.cmcc.logic;

import com.google.common.base.Optional;
import com.roamingroths.cmcc.logic.chart.ChartEntry;
import com.roamingroths.cmcc.logic.chart.MccScorer;
import com.roamingroths.cmcc.logic.chart.Observation;
import com.roamingroths.cmcc.logic.chart.ObservationEntry;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MccScorerTest {

  private static final LocalDate TODAY = LocalDate.now();

  @Test
  public void test_table_14_2_A() throws Exception {
    List<String> observations = new ArrayList<>();
    observations.add("0AD");
    observations.add("0AD");
    observations.add("2AD");
    observations.add("0AD");
    observations.add("6CKx1");
    observations.add("8CKx2");
    observations.add("10KLx2");
    observations.add("10KLx3");
    observations.add("10WLx1");
    assertEquals(9.3, getScore(observations, 9), 0.1);
  }

  @Test
  public void test_table_14_2_B() throws Exception {
    List<String> observations = new ArrayList<>();
    observations.add("0AD");
    observations.add("0AD");
    observations.add("2x1");
    observations.add("0AD");
    observations.add("4x1");
    observations.add("2AD");
    observations.add("4x1");
    observations.add("4x1");
    observations.add("10CKx1");
    assertEquals(1.7, getScore(observations, 9), 0.1);
  }

  @Test
  public void test_table_14_2_C() throws Exception {
    List<String> observations = new ArrayList<>();
    observations.add("10Cx1");
    observations.add("10CKx2");
    observations.add("10Kx1");
    observations.add("10KLAD");
    observations.add("10Cx1");
    observations.add("8CKAD");
    observations.add("10SLAd");
    observations.add("10KLx2");
    observations.add("10Kx1");
    assertEquals(11.0, getScore(observations, 9), 0.1);
  }

  @Test
  public void test_fig_14_1_B() throws Exception {
    List<String> observations = new ArrayList<>();
    observations.add("H");
    observations.add("H");
    observations.add("H");
    observations.add("M");
    observations.add("M");
    observations.add("L0AD");
    observations.add("L0AD");

    observations.add("0AD");
    observations.add("0AD");
    observations.add("10KLx3");
    observations.add("10KLx2");
    observations.add("0AD");
    observations.add("6CLx1");
    observations.add("8KLx1");

    observations.add("8Cx2");
    observations.add("10CLx2");
    observations.add("6Cx1");
    observations.add("2AD");
    observations.add("0AD");
    observations.add("0AD");
    observations.add("0AD");

    assertEquals(10.3, getScore(observations, 16), 0.1);
  }

  private float getScore(List<String> observations, int peakDayNum) throws Exception {
    LocalDate start = LocalDate.now();

    List<ChartEntry> entries = new ArrayList<>();
    LocalDate curDate = start;
    for (String str : observations) {
      Observation observation = Observation.fromString(str);
      ObservationEntry observationEntry = new ObservationEntry(curDate, observation, false, false, false, false, false, null);
      ChartEntry entry = new ChartEntry(curDate, observationEntry, null, null);
      entries.add(entry);
      curDate = curDate.plusDays(1);
    }
    LocalDate peakDay = start.plusDays(peakDayNum - 1);
    return MccScorer.getScore(entries, Optional.of(peakDay));
  }
}
