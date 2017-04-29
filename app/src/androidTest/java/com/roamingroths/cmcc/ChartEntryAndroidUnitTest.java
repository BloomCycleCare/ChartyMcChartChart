package com.roamingroths.cmcc;

import android.os.Parcel;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Observation;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChartEntryAndroidUnitTest {

  private static final ImmutableList<String> OBSERVATION_STRS = ImmutableList.of(
      "H", "VL10CKAD", "0X1", "10WLAD"
  );

  @Test
  public void chartEntry_ParcelableWriteRead() throws Exception {
    for (String observationStr : OBSERVATION_STRS) {
      ChartEntry entry = new ChartEntry(Observation.fromString(observationStr), true, true);

      Parcel parcel = Parcel.obtain();
      entry.writeToParcel(parcel, entry.describeContents());

      parcel.setDataPosition(0);

      ChartEntry createdFromParcel = ChartEntry.CREATOR.createFromParcel(parcel);
      assertEquals(entry, createdFromParcel);
    }
  }
}
