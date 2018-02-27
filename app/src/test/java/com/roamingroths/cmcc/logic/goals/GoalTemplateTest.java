package com.roamingroths.cmcc.logic.goals;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.roamingroths.cmcc.logic.goals.GoalTemplate.RelativeChange;
import static com.roamingroths.cmcc.logic.goals.GoalTemplate.Type;
import static org.junit.Assert.assertThat;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class GoalTemplateTest {

  private static final ImmutableSet<GoalTemplate> TEMPLATES = ImmutableSet.<GoalTemplate>builder()
      .add(GoalTemplate.builder("drink", Type.DRINK).withRelativeChange(RelativeChange.MORE).withObject("water").build())
      .add(GoalTemplate.builder("drink", Type.DRINK).withRelativeChange(RelativeChange.LESS).withObject("alcohol").build())
      .add(GoalTemplate.builder("drink", Type.DRINK).withRelativeChange(RelativeChange.LESS).withObject("caffeine").build())
      .add(GoalTemplate.builder("eat", Type.EAT).withRelativeChange(RelativeChange.MORE).withObject("fiber").build())
      .add(GoalTemplate.builder("eat", Type.EAT).withRelativeChange(RelativeChange.LESS).withObject("gluten").build())
      .add(GoalTemplate.builder("eat", Type.EAT).withRelativeChange(RelativeChange.LESS).withObject("dairy").build())
      .add(GoalTemplate.builder("eat", Type.EAT).withRelativeChange(RelativeChange.LESS).withObject("sugar").build())
      .add(GoalTemplate.builder("journal", Type.GENERAL).build())
      .add(GoalTemplate.builder("meditate", Type.GENERAL).build())
      .build();
  private static final GoalTemplateFactory FACTORY = new GoalTemplateFactory(TEMPLATES);

  @Test
  public void testToString_emptyInput() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("", 3));
    assertThat(out, IsIterableContainingInOrder.contains(
        "drink less ...", "drink more ...", "eat less ...", "eat more ...", "journal ...", "meditate ..."));
  }

  @Test
  public void testToString_singleAction() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("dr", 3));
    assertThat(out, IsIterableContainingInOrder.contains("drink less alcohol", "drink less caffeine", "drink more water"));
  }

  @Test
  public void testToString_partialSingleAction() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("j n", 3));
    assertThat(out, IsIterableContainingInOrder.contains("j nine times per ..."));
  }

  @Test
  public void testToString_relativePartial() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("drink l", 3));
    assertThat(out, IsIterableContainingInOrder.contains("drink less ...", "drink less alcohol", "drink less caffeine"));
  }

  @Test
  public void testToString_relativeFull() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("drink less x", 3));
    assertThat(out, IsIterableContainingInOrder.contains("drink less x"));
  }

  @Test
  public void testToString_invalidPartial() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("drink x", 3));
    assertThat(out, IsIterableContainingInOrder.contains("drink x"));
  }

  @Test
  public void testToString_unknownAction() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("foo bar", 3));
    assertThat(out, IsIterableContainingInOrder.contains("foo bar", "foo bar once per ...", "foo bar twice per ...", "foo bar three times per ..."));
  }

  @Test
  public void testToString_unknownActionWithSpecialInstances() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("foo bar and something once per week", 3));
    assertThat(out, IsIterableContainingInOrder.contains("foo bar and something once per week"));
  }

  @Test
  public void testToString_unknownActionWithNumericInstances() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("foo bar with something else 1 time per day", 3));
    assertThat(out, IsIterableContainingInOrder.contains("foo bar with something else 1 time per day"));
  }

  @Test
  public void testToString_unknownActionWithWordInstances() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("foo bar four times", 3));
    assertThat(out, IsIterableContainingInOrder.contains(
        "foo bar four times per day",
        "foo bar four times per week",
        "foo bar four times per month"));
  }

  @Test
  public void testToString_unknownActionWithPartialInstances() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("foo bar f", 3));
    assertThat(out, IsIterableContainingInOrder.contains(
        "foo bar four times per ...", "foo bar five times per ...", "foo bar fourteen times per ...", "foo bar fifteen times per ..."));
  }

  @Test
  public void testToString_unknownActionWithPartialPeriod() throws Exception {
    List<String> out = toStringList(FACTORY.fromInput("foo bar four times per d", 3));
    assertThat(out, IsIterableContainingInOrder.contains("foo bar four times per day"));
  }

  /*@Test
  public void testToString_relativeDecrease() throws Exception {
    GoalTemplate goalTemplate = new GoalTemplate("eat", GoalTemplate.RelativeChange.LESS, "apples", GoalTemplate.Type.EAT);
    assertEquals("eat less apples", goalTemplate.toString());
  }

  @Test
  public void testToString_relativeNoObject() throws Exception {
    GoalTemplate goalTemplate = new GoalTemplate("eat", GoalTemplate.RelativeChange.LESS, "", GoalTemplate.Type.EAT);
    assertEquals("eat less ...", goalTemplate.toString());
  }

  @Test
  public void testToString_general() throws Exception {
    GoalTemplate goalTemplate = new GoalTemplate("journal", "");
    assertEquals("journal", goalTemplate.toString());
  }

  @Test
  public void testToString_generalWithObject() throws Exception {
    GoalTemplate goalTemplate = new GoalTemplate("ride", "bike");
    assertEquals("ride bike", goalTemplate.toString());
  }*/

  private static List<String> toStringList(Iterable<GoalTemplate> templates) {
    List<GoalTemplate> templateList = Lists.newArrayList(templates);
    Collections.sort(templateList);
    return Lists.newArrayList(Iterables.transform(templateList, Functions.toStringFunction()));
  }
}