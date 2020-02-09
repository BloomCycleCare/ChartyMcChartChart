package com.bloomcyclecare.cmcc.logic;

import com.bloomcyclecare.cmcc.data.domain.DischargeType;
import com.bloomcyclecare.cmcc.data.domain.Flow;
import com.bloomcyclecare.cmcc.data.domain.MucusModifier;
import com.bloomcyclecare.cmcc.data.domain.Observation;
import com.bloomcyclecare.cmcc.data.domain.Occurrences;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ObservationTest {
  @Test
  public void fromString_heavy() throws Exception {
    Observation observation = createAndTestToString("H");
    assertThat(observation.flow).isEqualTo(Flow.H);
    assertThat(observation.occurrences).isNull();
    assertThat(observation.dischargeSummary).isNull();
  }

  @Test
  public void fromString_heavyWithBlackOrBrown() throws Exception {
    Observation observation = createAndTestToString("HB");
    assertThat(observation.flow).isEqualTo(Flow.H);
    assertThat(observation.occurrences).isNull();
    assertThat(observation.dischargeSummary).isNotNull();
    assertThat(observation.dischargeSummary.mModifiers).containsExactly(MucusModifier.B);
  }

  @Test
  public void fromString_mediumWithBlackOrBrown() throws Exception {
    Observation observation = createAndTestToString("MB");
    assertThat(observation.flow).isEqualTo(Flow.M);
    assertThat(observation.occurrences).isNull();
    assertThat(observation.dischargeSummary).isNotNull();
    assertThat(observation.dischargeSummary.mModifiers).containsExactly(MucusModifier.B);
  }

  @Test
  public void fromString_heavyWithRedIsInvalid() throws Exception {
    try {
      createAndTestToString("HR");
      fail("Only B can follow H or M");
    } catch (ObservationParser.InvalidObservationException expected) {}
  }

  @Test
  public void fromString_heavyWithMucus() throws Exception {
    try {
      createAndTestToString("H10CAD");
      fail("Heavy flow cannot have a mucus observation");
    } catch (ObservationParser.InvalidObservationException expected) {
    }
  }

  @Test
  public void fromString_medium() throws Exception {
    Observation observation = createAndTestToString("M");
    assertEquals(Flow.M, observation.flow);
    assertNull(observation.dischargeSummary);
    assertNull(observation.occurrences);
  }

  @Test
  public void fromString_light() throws Exception {
    Observation observation = createAndTestToString("L6AD");
    assertThat(observation.dischargeSummary.getCode()).hasValue("6");
    assertEquals(DischargeType.STICKY, observation.dischargeSummary.mType);
    assertEquals(Occurrences.AD, observation.occurrences);
  }

  @Test
  public void fromString_lightMissingDischarge() throws Exception {
    try {
      createAndTestToString("LAD");
      fail("Missing discharge for L flow not allowed.");
    } catch (ObservationParser.InvalidObservationException expected) {
    }
  }

  /*j@Test
  public void fromString_withModifier() throws Exception {
    for (DischargeType type : DischargeSummary.DischargeType.values()) {
      boolean modifiersAllowed = Observation.TYPES_ALLOWING_MODIFIERS.contains(type);
      for (MucusModifier modifier : DischargeSummary.MucusModifier.values()) {
        try {
          Observation observation = Observation.fromString(
              type.getCode() + modifier.name() + "AD");
          if (modifiersAllowed) {
            assertEquals(type, observation.dischargeSummary.mType);
            assertEquals(ImmutableSet.of(modifier), observation.dischargeSummary.mModifiers);
          } else {
            fail(type.name() + " should not allow modifiers");
          }
        } catch (Observation.InvalidObservationException maybeExpected) {
          if (modifiersAllowed) {
            fail();
          }
        }
      }
    }
  }*/

  @Test
  public void fromString_withModifiers() throws Exception {
    Observation observation = createAndTestToString("8CPX3");
    assertEquals(
        ImmutableSet.of(MucusModifier.C, MucusModifier.P), observation.dischargeSummary.mModifiers);
  }

  @Test
  public void fromString_expectNoInfoAfterFlow() throws Exception {
    String[] validObservations = {"H", "M"};
    for (String observation : validObservations) {
      ObservationParser.parse(observation);
    }
    String[] invalidObservations = {"L", "VL"};
    for (String observation : invalidObservations) {
      try {
        ObservationParser.parse(observation);
        fail();
      } catch (ObservationParser.InvalidObservationException expected) {
      }
    }
  }

  /*@Test
  public void fromString_missingOccurrences() throws Exception {
    try {
      createAndTestToString("10WL");
      fail("Missing required occurrences");
    } catch (Observation.InvalidObservationException expected) {
      String expectedMessage = "Missing one of: " + Observation.VALID_OCCURRENCES_STR;
      assertEquals(expectedMessage, expected.getMessage());
    }
  }

  @Test
  public void fromString_invalidOccurrences() throws Exception {
    try {
      createAndTestToString("10WLAP");
      fail("Missing required occurrences");
    } catch (Observation.InvalidObservationException expected) {
      String expectedMessage =
          "Occurrence AP is not one of: " + Observation.VALID_OCCURRENCES_STR;
      assertEquals(expectedMessage, expected.getMessage());
    }
  }*/

  @Test
  public void fromString_extraInfoFlow() throws Exception {
    try {
      createAndTestToString("HFoo");
      fail("Foo should not be allowed to follow H");
    } catch (ObservationParser.InvalidObservationException expected) {
    }
  }

  @Test
  public void fromString_extraInfoNonFlow() throws Exception {
    try {
      createAndTestToString("0ADFoo");
      fail("Foo should not be allowed to follow AD");
    } catch (ObservationParser.InvalidObservationException expected) {
    }
  }

  private Observation createAndTestToString(String observationStr) throws Exception {
    Observation observation = ObservationParser.parse(observationStr).orNull();
    assertEquals(observationStr, observation.toString().replace(" ", ""));
    return observation;
  }
}