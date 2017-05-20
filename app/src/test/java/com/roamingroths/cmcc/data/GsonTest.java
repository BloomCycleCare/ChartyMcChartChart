package com.roamingroths.cmcc.data;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.roamingroths.cmcc.utils.GsonUtil;

import org.joda.time.LocalDate;
import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class GsonTest {

  private static final ImmutableList<String> OBSERVATION_STRS = ImmutableList.of(
      "H", "VL10CKAD", "0X1", "10WLAD"
  );

  @Test
  public void chartEntry() throws Exception {
    for (String observationStr : OBSERVATION_STRS) {
      ChartEntry entry =
          new ChartEntry(LocalDate.now(), Observation.fromString(observationStr), true, false);
      Gson gson = GsonUtil.getGsonInstance();
      gson.fromJson(gson.toJson(entry), ChartEntry.class);
    }
  }
}