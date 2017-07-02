package com.roamingroths.cmcc;

import android.content.Context;

import com.google.common.collect.ImmutableMap;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.Observation;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.mockito.Mockito.verify;

/**
 * Created by parkeroth on 7/1/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChartEntryViewHolderTest {

  private static final LocalDate CYCLE_START_DATE = LocalDate.parse("2017-01-01");
  private static final String CYCLE_ID = "fake-cycle-id";
  private static final Cycle CYCLE = new Cycle(CYCLE_ID, null, null, CYCLE_START_DATE, null);
  private static final boolean PREPEAK_YELLOW_ENABLED = true;
  private static final boolean PREPEAK_YELLOW_DISABLED = false;
  private static final boolean POSTPEAK_YELLOW_ENABLED = true;
  private static final boolean POSTPEAK_YELLOW_DISABLED = false;

  @Mock
  Context mContext;
  @Captor
  ArgumentCaptor<Integer> mBackgroundCaptor;

  @Test
  public void testB1A() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L 0AD"), Expectations.redSticker());
    entries.put(Entry.forText("2x1"), Expectations.greenSticker());
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());

    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("6cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8kx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klAD"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klAD").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("10cx1"), Expectations.whiteSticker().withBaby().withPeakText("1"));

    entries.put(Entry.forText("0AD"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("2x1"), Expectations.greenSticker());
    runTest(entries.build(), new Preferences(PREPEAK_YELLOW_DISABLED, POSTPEAK_YELLOW_DISABLED));
  }

  @Test
  public void testB1B() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L0AD"), Expectations.redSticker());
    entries.put(Entry.forText("VL2x1"), Expectations.redSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klad").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby().withPeakText("1"));

    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    runTest(entries.build(), new Preferences(PREPEAK_YELLOW_DISABLED, POSTPEAK_YELLOW_DISABLED));
  }

  @Test
  public void testB1C() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L0AD"), Expectations.redSticker());
    entries.put(Entry.forText("2x2"), Expectations.greenSticker());
    entries.put(Entry.forText("4x1"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());

    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby());
    entries.put(Entry.forText("2x2"), Expectations.greenSticker().withBaby());
    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby());

    entries.put(Entry.forText("4x1"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10kad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10wlx2").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));

    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    runTest(entries.build(), new Preferences(PREPEAK_YELLOW_DISABLED, POSTPEAK_YELLOW_DISABLED));
  }
  // TODO: B1D
  // TODO: B1E
  // TODO: B1F

  // TODO: B2

  @Test
  public void testB7A() throws Exception {
    // Double Peak
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("L2AD"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L2AD"), Expectations.redSticker());
    entries.put(Entry.forText("2AD").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2AD"), Expectations.greenSticker());

    entries.put(Entry.forText("2AD").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2AD").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2AD"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8kad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cx1").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));

    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("4ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("10cklx1"), Expectations.whiteSticker().withBaby());

    entries.put(Entry.forText("10ckad").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("2x3").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad").intercourse(), Expectations.greenSticker().withIntercourse());

    entries.put(Entry.forText("4x1").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    runTest(entries.build(), new Preferences(PREPEAK_YELLOW_DISABLED, POSTPEAK_YELLOW_DISABLED));
  }

  // TODO: B7B - "Missed Period" form of "Double Peak"

  @Test
  public void testPrePeakYellowExample() throws Exception {
    // Double Peak
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("L2ad"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L6cx1"), Expectations.redSticker());
    entries.put(Entry.forText("L6cx2"), Expectations.redSticker());
    entries.put(Entry.forText("VL6cx2"), Expectations.redSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());

    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("8kx2").pointOfChange(), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10kx3"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klx1"), Expectations.whiteSticker().withBaby());

    entries.put(Entry.forText("10klx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10wlad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10wlad").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());

    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    runTest(entries.build(), new Preferences(PREPEAK_YELLOW_ENABLED, POSTPEAK_YELLOW_DISABLED));
  }

  @Test
  public void testPostPeakYellowExample() throws Exception {
    // Double Peak
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("L2ad"), Expectations.redSticker());
    entries.put(Entry.forText("VL0ad"), Expectations.redSticker());
    entries.put(Entry.forText("0ad").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad").intercourse(), Expectations.greenSticker().withIntercourse());

    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cgx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cklx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cklad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("4x2"), Expectations.greenSticker().withBaby());

    entries.put(Entry.forText("10cx1").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("8cgx2"), Expectations.yellowSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("8gyx1"), Expectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());

    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("4ad"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    runTest(entries.build(), new Preferences(PREPEAK_YELLOW_DISABLED, POSTPEAK_YELLOW_ENABLED));
  }

  private void runTest(
      ImmutableMap<Entry, Expectations> entries, Preferences preferences) throws Exception {
    ChartEntryList entryList = ChartEntryList.builder(CYCLE, preferences).build();
    for (Map.Entry<Entry, Expectations> anEntry : entries.entrySet()) {
      LocalDate entryDate = CYCLE_START_DATE.plusDays(entryList.size());
      entryList.addEntry(anEntry.getKey().asChartEntry(entryDate));
    }
    int i = entryList.size() - 1;
    for (Map.Entry<Entry, Expectations> anEntry : entries.entrySet()) {
      int entryNum = entryList.size() - i;
      ChartEntryViewHolder holder = Mockito.mock(ChartEntryViewHolder.class);
      Expectations expectations = anEntry.getValue();
      entryList.bindViewHolder(holder, i, mContext);
      try {
        checkBackgroundColor(holder, expectations);
        checkBaby(holder, expectations);
        checkPeakText(holder, expectations);
        checkIntercourse(holder, expectations);
      } catch (Exception e) {
        throw new Exception("For entry #" + entryNum + ": " + e.getMessage());
      } finally {
        i--;
      }
    }
  }

  private void checkIntercourse(
      ChartEntryViewHolder holder, Expectations expectations) throws Exception {
    ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
    verify(holder).setIntercourse(captor.capture());

    boolean expected = expectations.shouldHaveIntercourse;
    boolean captured = captor.getValue();
    if (captured == expected) {
      return;
    }
    if (expected) {
      throw new Exception("Expected intercourse but not present");
    } else {
      throw new Exception("Did not expect intercourse but was present");
    }
  }

  private void checkPeakText(
      ChartEntryViewHolder holder, Expectations expectations) throws Exception {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(holder).setPeakDayText(captor.capture());

    String expected = expectations.peakText;
    String captured = captor.getValue();
    if (captured.equals(expected)) {
      return;
    }
    throw new Exception("Expected " + expected + " but was " + captured);
  }

  private void checkBaby(
      ChartEntryViewHolder holder, Expectations expectations) throws Exception {
    ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
    verify(holder).setShowBaby(captor.capture());

    boolean expected = expectations.shouldHaveBaby;
    boolean captured = captor.getValue();
    if (expected == captured) {
      return;
    }
    if (expected) {
      throw new Exception("Expected baby was not present");
    } else {
      throw new Exception("Did not expect baby was present");
    }
  }

  private void checkBackgroundColor(
      ChartEntryViewHolder holder, Expectations expectations) throws Exception {
    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
    verify(holder).setBackgroundColor(captor.capture());

    int expected = expectations.backgroundResourceId;
    int captured = captor.getValue();
    if (captured == expected) {
      return;
    }
    String capturedStr = getColorStr(captured);
    String expectedStr = getColorStr(expected);
    throw new Exception("Expected " + expectedStr + " but was " + capturedStr);
  }

  private String getColorStr(int colorResource) {
    switch (colorResource) {
      case R.color.entryGreen:
        return "GREEN";
      case R.color.entryWhite:
        return "WHITE";
      case R.color.entryYellow:
        return "YELLOW";
      case R.color.entryRed:
        return "RED";
      default:
        throw new IllegalArgumentException("Unkown color resource");
    }
  }

  private static class Entry {

    private final String observationText;
    private boolean peakDay = false;
    private boolean intercourse = false;
    private boolean pointOfChange = false;

    private Entry(String observationText) {
      this.observationText = observationText;
    }

    public static Entry forText(String observationText) {
      return new Entry(observationText);
    }

    public Entry peakDay() {
      peakDay = true;
      return this;
    }

    public Entry intercourse() {
      intercourse = true;
      return this;
    }

    public Entry pointOfChange() {
      pointOfChange = true;
      return this;
    }

    public ChartEntry asChartEntry(LocalDate date) throws Observation.InvalidObservationException {
      Observation observation = Observation.fromString(observationText);
      return new ChartEntry(date, observation, peakDay, intercourse, false, pointOfChange);
    }
  }

  private static class Expectations {

    public final int backgroundResourceId;
    public boolean shouldHaveBaby = false;
    public boolean shouldHaveIntercourse = false;
    public String peakText = "";

    private Expectations(int backgroundResourceId) {
      this.backgroundResourceId = backgroundResourceId;
    }

    public static Expectations redSticker() {
      return new Expectations(R.color.entryRed);
    }

    public static Expectations greenSticker() {
      return new Expectations(R.color.entryGreen);
    }

    public static Expectations yellowSticker() {
      return new Expectations(R.color.entryYellow);
    }

    public static Expectations whiteSticker() {
      return new Expectations(R.color.entryWhite);
    }

    public Expectations withPeakText(String text) {
      peakText = text;
      return this;
    }

    public Expectations withBaby() {
      shouldHaveBaby = true;
      return this;
    }

    public Expectations withIntercourse() {
      shouldHaveIntercourse = true;
      return this;
    }
  }
}
