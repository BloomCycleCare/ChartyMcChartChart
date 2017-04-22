package com.roamingroths.cmcc.data;

import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.data.Observation.DischargeType;
import com.roamingroths.cmcc.data.Observation.Flow;
import com.roamingroths.cmcc.data.Observation.MucusModifier;
import com.roamingroths.cmcc.data.Observation.Occurrences;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ObservationBuilderTest {
  @Test
  public void fromString_heavy() throws Exception {
    Observation observation = createAndTestToString("H");
    assertEquals(Flow.H, observation.flow);
    assertNull(observation.mucusSummary);
    assertNull(observation.occurrences);
  }

  @Test
  public void fromString_heavyWithMucus() throws Exception {
    try {
      Observation observation = createAndTestToString("H10CAD");
      fail("Heavy flow cannot have a mucus observation");
    } catch (ObservationBuilder.InvalidObservationException expected) {
    }
  }

  @Test
  public void fromString_medium() throws Exception {
    Observation observation = createAndTestToString("M");
    assertEquals(Flow.M, observation.flow);
    assertNull(observation.mucusSummary);
    assertNull(observation.occurrences);
  }

  @Test
  public void fromString_light() throws Exception {
    Observation observation = createAndTestToString("L6AD");
    assertEquals("6", observation.mucusSummary.getCode());
    assertEquals(DischargeType.STICKY, observation.mucusSummary.mType);
    assertEquals(Occurrences.AD, observation.occurrences);
  }

  @Test
  public void fromString_lightMissingDischarge() throws Exception {
    try {
      createAndTestToString("LAD");
      fail("Missing discharge for L flow not allowed.");
    } catch (ObservationBuilder.InvalidObservationException expected) {
    }
  }

  @Test
  public void fromString_lightInvalidDischargeType() throws Exception {
    try {
      createAndTestToString("L0AD");
      fail("DischargeType DRY not allowed for for L flow.");
    } catch (ObservationBuilder.InvalidObservationException expected) {
    }
  }
  @Test
  public void fromString_withModifier() throws Exception {
    for (DischargeType type : DischargeType.values()) {
      boolean modifiersAllowed = ObservationBuilder.TYPES_ALLOWING_MODIFIERS.contains(type);
      for (MucusModifier modifier : MucusModifier.values()) {
        try {
          Observation observation = ObservationBuilder.fromString(
              type.getCode() + modifier.name() + "AD").build();
          if (modifiersAllowed) {
            assertEquals(type, observation.mucusSummary.mType);
            assertEquals(ImmutableSet.of(modifier), observation.mucusSummary.mModifiers);
          } else {
            fail(type.name() + " should not allow modifiers");
          }
        } catch (ObservationBuilder.InvalidObservationException maybeExpected) {
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
        ImmutableSet.of(MucusModifier.C, MucusModifier.P), observation.mucusSummary.mModifiers);
  }

  @Test
  public void fromString_expectNoInfoAfterFlow() throws Exception {
    String[] validObservations = {"H", "M"};
    for (String observation : validObservations) {
      ObservationBuilder.fromString(observation).build();
    }
    String[] invalidObservations = {"L", "VL"};
    for (String observation : invalidObservations) {
      try {
        ObservationBuilder.fromString(observation).build();
        fail();
      } catch (ObservationBuilder.InvalidObservationException expected) {
      }
    }
  }

  @Test
  public void fromString_missingOccurrences() throws Exception {
    try {
      createAndTestToString("10WL");
      fail("Missing required occurrences");
    } catch (ObservationBuilder.InvalidObservationException expected) {
      String expectedMessage = "Missing one of: " + ObservationBuilder.VALID_OCCURRENCES_STR;
      assertEquals(expectedMessage, expected.getMessage());
    }
  }

  @Test
  public void fromString_invalidOccurrences() throws Exception {
    try {
      createAndTestToString("10WLAP");
      fail("Missing required occurrences");
    } catch (ObservationBuilder.InvalidObservationException expected) {
      String expectedMessage =
          "Occurrence AP is not one of: " + ObservationBuilder.VALID_OCCURRENCES_STR;
      assertEquals(expectedMessage, expected.getMessage());
    }
  }

  @Test
  public void fromString_extraInfoFlow() throws Exception {
    try {
      createAndTestToString("HFoo");
      fail("Foo should not be allowed to follow H");
    } catch (ObservationBuilder.InvalidObservationException expected) {
    }
  }

  @Test
  public void fromString_extraInfoNonFlow() throws Exception {
    try {
      createAndTestToString("0ADFoo");
      fail("Foo should not be allowed to follow AD");
    } catch (ObservationBuilder.InvalidObservationException expected) {
    }
  }

  private Observation createAndTestToString(String observationStr) throws Exception {
    Observation observation = ObservationBuilder.fromString(observationStr).build();
    assertEquals(observationStr, observation.toString().replace(" ", ""));
    return observation;
  }
}