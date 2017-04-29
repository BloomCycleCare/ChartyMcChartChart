package com.roamingroths.cmcc.data;

import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.data.DischargeSummary.DischargeType;
import com.roamingroths.cmcc.data.DischargeSummary.MucusModifier;

import org.junit.Test;

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
    assertEquals(Flow.H, observation.flow);
    assertNull(observation.dischargeSummary);
    assertNull(observation.occurrences);
  }

  @Test
  public void fromString_heavyWithMucus() throws Exception {
    try {
      Observation observation = createAndTestToString("H10CAD");
      fail("Heavy flow cannot have a mucus observation");
    } catch (Observation.InvalidObservationException expected) {
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
    assertEquals("6", observation.dischargeSummary.getCode());
    assertEquals(DischargeSummary.DischargeType.STICKY, observation.dischargeSummary.mType);
    assertEquals(Occurrences.AD, observation.occurrences);
  }

  @Test
  public void fromString_lightMissingDischarge() throws Exception {
    try {
      createAndTestToString("LAD");
      fail("Missing discharge for L flow not allowed.");
    } catch (Observation.InvalidObservationException expected) {
    }
  }

  @Test
  public void fromString_lightInvalidDischargeType() throws Exception {
    try {
      createAndTestToString("L0AD");
      fail("DischargeType DRY not allowed for for L flow.");
    } catch (Observation.InvalidObservationException expected) {
    }
  }
  @Test
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
  }

  @Test
  public void fromString_withModifiers() throws Exception {
    Observation observation = createAndTestToString("8CPX3");
    assertEquals(
        ImmutableSet.of(DischargeSummary.MucusModifier.C, DischargeSummary.MucusModifier.P), observation.dischargeSummary.mModifiers);
  }

  @Test
  public void fromString_expectNoInfoAfterFlow() throws Exception {
    String[] validObservations = {"H", "M"};
    for (String observation : validObservations) {
      Observation.fromString(observation);
    }
    String[] invalidObservations = {"L", "VL"};
    for (String observation : invalidObservations) {
      try {
        Observation.fromString(observation);
        fail();
      } catch (Observation.InvalidObservationException expected) {
      }
    }
  }

  @Test
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
  }

  @Test
  public void fromString_extraInfoFlow() throws Exception {
    try {
      createAndTestToString("HFoo");
      fail("Foo should not be allowed to follow H");
    } catch (Observation.InvalidObservationException expected) {
    }
  }

  @Test
  public void fromString_extraInfoNonFlow() throws Exception {
    try {
      createAndTestToString("0ADFoo");
      fail("Foo should not be allowed to follow AD");
    } catch (Observation.InvalidObservationException expected) {
    }
  }

  private Observation createAndTestToString(String observationStr) throws Exception {
    Observation observation = Observation.fromString(observationStr);
    assertEquals(observationStr, observation.toString().replace(" ", ""));
    return observation;
  }
}