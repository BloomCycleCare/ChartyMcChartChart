package com.bloomcyclecare.cmcc.utils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HeatMap {

  private final Map<Integer, Integer> mValues;
  private final ImmutableList<String> mColors;
  private final String mEmptyColor;
  private final int mMinValue;
  private final int mValueRange;

  public HeatMap(Map<Integer, Integer> values, String emptyColor, List<String> colorScale) {
    mEmptyColor = emptyColor;
    mValues = values;
    Set<Integer> distinctValues = new HashSet<>(values.values());
    final List<String> colors = new ArrayList<>(colorScale);
    while (colors.size() > distinctValues.size()) {
      colors.remove(colors.size() - 1);
    }
    mColors = ImmutableList.copyOf(colors);
    mMinValue = values.values().stream().min(Integer::compareTo).orElse(-1);
    int maxValue = values.values().stream().max(Integer::compareTo).orElse(-1);
    mValueRange = maxValue - mMinValue;
  }

  public Integer getValue(int index) {
    return mValues.get(index);
  }

  public String getColor(int index) {
    Integer value = mValues.get(index);
    if (mColors.isEmpty() || value == null) {
      return mEmptyColor;
    }
    // https://stackoverflow.com/questions/929103/convert-a-number-range-to-another-range-maintaining-ratio
    int colorIndex = ((value - mMinValue) * (mColors.size() - 1)) / mValueRange;
    return mColors.get(colorIndex);
  }
}
