package com.bloomcyclecare.cmcc.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class HeatMapTest {

  @Test
  public void emptyValuesAndColors() {
    HeatMap heatMap = new HeatMap(ImmutableMap.of(), "empty", ImmutableList.of());
    for (int i=0; i<100; i++) {
      assertThat(heatMap.getColor(i)).isEqualTo("empty");
    }
  }

  @Test
  public void emptyValuesAndSomeColors() {
    HeatMap heatMap = new HeatMap(ImmutableMap.of(), "empty", ImmutableList.of("A", "B"));
    for (int i=0; i<100; i++) {
      assertThat(heatMap.getColor(i)).isEqualTo("empty");
    }
  }

  @Test
  public void someValuesAndNoColors() {
    HeatMap heatMap = new HeatMap(ImmutableMap.of(
        1, 5,
        2, 3), "empty", ImmutableList.of());
    for (int i=0; i<100; i++) {
      assertThat(heatMap.getColor(i)).isEqualTo("empty");
    }
  }

  @Test
  public void smallTest() {
    String emptyColor = "EMPTY";
    List<String> colors = ImmutableList.of("A", "B", "C", "D", "E");
    Map<Integer, Integer> values = new HashMap<>();
    values.put(3, 1);
    values.put(4, 2);
    values.put(5, 3);
    values.put(6, 2);
    values.put(7, 1);

    Map<Integer, String> expected = new HashMap<>();
    expected.put(3, "A");
    expected.put(4, "B");
    expected.put(5, "C");
    expected.put(6, "B");
    expected.put(7, "A");

    HeatMap heatMap = new HeatMap(values, emptyColor, colors);
    Map<Integer, String> got = new HashMap<>();
    for (int i=0; i<50; i++) {
      got.put(i, heatMap.getColor(i));
    }

    assertThat(Maps.difference(got, expected).entriesDiffering()).isEmpty();
  }

  @Test
  public void largeTest() {
    String emptyColor = "EMPTY";
    List<String> colors = ImmutableList.of("A", "B", "C", "D", "E");
    Map<Integer, Integer> values = new HashMap<>();
    values.put(3, 1);
    values.put(4, 2);
    values.put(5, 3);
    values.put(6, 9);
    values.put(7, 6);
    values.put(8, 12);
    values.put(9, 1);
    values.put(10, 5);
    values.put(11, 2);

    Map<Integer, String> expected = new HashMap<>();
    expected.put(3, "A");
    expected.put(4, "A");
    expected.put(5, "A");
    expected.put(6, "C");
    expected.put(7, "B");
    expected.put(8, "E");
    expected.put(9, "A");
    expected.put(10, "B");
    expected.put(11, "A");

    HeatMap heatMap = new HeatMap(values, emptyColor, colors);
    Map<Integer, String> got = new HashMap<>();
    for (int i=0; i<50; i++) {
      got.put(i, heatMap.getColor(i));
    }
    assertThat(Maps.difference(got, expected).entriesDiffering()).isEmpty();
  }
}
