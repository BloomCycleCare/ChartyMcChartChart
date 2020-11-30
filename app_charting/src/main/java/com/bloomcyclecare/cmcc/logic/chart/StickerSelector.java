package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.instructions.AbstractInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class StickerSelector {

  private static final ImmutableMap<Sticker, Node> LEAF_NODES = ImmutableMap.<Sticker, Node>builder()
      .put(Sticker.RED, new LeafNode(Sticker.RED))
      .put(Sticker.GREEN, new LeafNode(Sticker.GREEN))
      .put(Sticker.GREEN_BABY, new LeafNode(Sticker.GREEN_BABY))
      .put(Sticker.YELLOW, new LeafNode(Sticker.YELLOW))
      .put(Sticker.YELLOW_BABY, new LeafNode(Sticker.YELLOW_BABY))
      .put(Sticker.WHITE_BABY, new LeafNode(Sticker.WHITE_BABY))
      .build();

  static final String BLEEDING_POSITIVE_EXPLANATION = "Observation had either H, M, L, VL, B or R";
  static final String BLEEDING_NEGATIVE_EXPLANATION = "No signs of bleeding present in observation (i.e., H, M, L, VL, B or R)";

  static final String MUCUS_POSITIVE_EXPLANATION = "Observation had either 6, 8 or 10";
  static final String MUCUS_NEGATIVE_EXPLANATION = "No signs of mucus present in observation (i.e., 6, 8 or 10)";

  static final String FERTILE_POSITIVE_EXPLANATION = "Active fertility instructions: ";
  static final String FERTILE_NEGATIVE_EXPLANATION = "No fertility instructions (D.1 - D.6) are active";

  static final String INFERTILE_POSITIVE_EXPLANATION = "Active special instructions: ";
  static final String INFERTILE_NEGATIVE_EXPLANATION = "No special instructions apply to warrant yellow stamps";

  private static final Node TREE = new ParentNode(
      c -> !c.fertilityReasons.isEmpty(), "is", "isn't", "fertile",
      c -> FERTILE_POSITIVE_EXPLANATION + Joiner.on(", ").join(c.fertilityReasons.stream().map(AbstractInstruction::description).collect(Collectors.toList())),
      c -> FERTILE_NEGATIVE_EXPLANATION,
      new ParentNode(c -> c.hasBleeding, "has", "doesn't have", "bleeding",
          c -> BLEEDING_POSITIVE_EXPLANATION,
          c -> BLEEDING_NEGATIVE_EXPLANATION,
          LEAF_NODES.get(Sticker.RED),
          new ParentNode(
              c -> c.hasMucus, "has", "doesn't have", "mucus",
              c -> MUCUS_POSITIVE_EXPLANATION,
              c -> MUCUS_NEGATIVE_EXPLANATION,
              new ParentNode(
                  c -> !c.infertilityReasons.isEmpty(), "has", "doesn't have", "active special instructions",
                  c -> INFERTILE_POSITIVE_EXPLANATION + Joiner.on(", ").join(c.infertilityReasons.stream().map(AbstractInstruction::description).collect(Collectors.toList())),
                  c -> INFERTILE_NEGATIVE_EXPLANATION,
                  LEAF_NODES.get(Sticker.YELLOW_BABY),
                  LEAF_NODES.get(Sticker.WHITE_BABY)
              ),
              LEAF_NODES.get(Sticker.GREEN_BABY)
          )
      ),
      new ParentNode(
          c -> c.hasMucus, "has", "doesn't have", "mucus",
          c -> MUCUS_POSITIVE_EXPLANATION,
          c -> MUCUS_NEGATIVE_EXPLANATION,
          LEAF_NODES.get(Sticker.YELLOW),
          LEAF_NODES.get(Sticker.GREEN)
      )
  );

  /*private static final Node TREE = new ParentNode(
      c -> c.hasBleeding, "has", "doesn't have", "bleeding",
      c -> BLEEDING_POSITIVE_EXPLANATION,
      c -> BLEEDING_NEGATIVE_EXPLANATION,
      LEAF_NODES.get(Sticker.RED),
      new ParentNode(
          c -> c.hasMucus, "has", "doesn't have", "mucus",
          c -> MUCUS_POSITIVE_EXPLANATION,
          c -> MUCUS_NEGATIVE_EXPLANATION,
          new ParentNode(
              c -> !c.infertilityReasons.isEmpty(), "has", "doesn't have", "active special instructions",
              c -> INFERTILE_POSITIVE_EXPLANATION + Joiner.on(", ").join(c.infertilityReasons.stream().map(AbstractInstruction::description).collect(Collectors.toList())),
              c -> INFERTILE_NEGATIVE_EXPLANATION,
              new ParentNode(
                  c -> !c.fertilityReasons.isEmpty(), "is", "isn't", "fertile",
                  c -> FERTILE_POSITIVE_EXPLANATION + Joiner.on(", ").join(c.fertilityReasons.stream().map(AbstractInstruction::description).collect(Collectors.toList())),
                  c -> FERTILE_NEGATIVE_EXPLANATION,
                  LEAF_NODES.get(Sticker.YELLOW_BABY),
                  LEAF_NODES.get(Sticker.YELLOW)
              ),
              LEAF_NODES.get(Sticker.WHITE_BABY)
          ),
          new ParentNode(
              c -> !c.fertilityReasons.isEmpty(), "is", "isn't", "fertile",
              c -> FERTILE_POSITIVE_EXPLANATION + Joiner.on(", ").join((Iterable<?>) c.fertilityReasons.stream().map(AbstractInstruction::description).collect(Collectors.toList())),
              c -> FERTILE_NEGATIVE_EXPLANATION,
              LEAF_NODES.get(Sticker.GREEN_BABY),
              LEAF_NODES.get(Sticker.GREEN)
          )
      )
  );*/

  public static SelectResult select(CycleRenderer.StickerSelectionContext context) {
    SelectResult result = new SelectResult();
    result.matchedCriteria = new ArrayList<>();
    result.sticker = TREE.select(context, result.matchedCriteria);
    return result;
  }

  public static class SelectResult {
    public Sticker sticker;
    public List<String> matchedCriteria;
  }

  public static CheckResult check(Sticker selection, CycleRenderer.StickerSelectionContext context) {
    SelectResult expectedResult = select(context);
    if (selection.equals(expectedResult.sticker)) {
      return CheckResult.ok(expectedResult.sticker);
    }

    Set<Node> ancestors = ancestors(LEAF_NODES.get(expectedResult.sticker));
    Node currentNode = LEAF_NODES.get(selection);

    ParentNode parentNode = null;
    boolean pathDir = false;
    while (!ancestors.contains(currentNode) && currentNode.parent().isPresent()) {
      parentNode = currentNode.parent().get();
      if (currentNode == parentNode.branchTrue) {
        pathDir = true;
      } else if (currentNode == parentNode.branchFalse) {
        pathDir = false;
      } else {
        throw new IllegalStateException(
            String.format("Node %s is not a child of %s",
                currentNode.description(), parentNode.description()));
      }
      currentNode = currentNode.parent().get();
    }

    return CheckResult.incorrect(
        expectedResult.sticker,
        String.format("Can't be %s, today %s %s", selection.name(), parentNode.getQualifier(!pathDir), parentNode.description()),
        parentNode.explanation(context));
  }

  public static class CheckResult {
    Sticker expected;
    Optional<String> errorMessage = Optional.empty();
    Optional<String> errorExplanation = Optional.empty();

    static CheckResult ok(Sticker expected) {
      CheckResult result = new CheckResult();
      result.expected = expected;
      return result;
    }

    static CheckResult incorrect(Sticker expected, String errorMessage, String errorExplanation) {
      CheckResult result = new CheckResult();
      result.expected = expected;
      result.errorMessage = Optional.of(errorMessage);
      result.errorExplanation = Optional.of(errorExplanation);
      return result;
    }

    public boolean ok() {
      return !errorMessage.isPresent();
    }
  }

  private static Set<Node> ancestors(Node node) {
    Set<Node> out = new HashSet<>();
    Node currentNode = node;
    while (currentNode.parent().isPresent()) {
      currentNode = currentNode.parent().get();
      out.add(currentNode);
    }
    return out;
  }

  private interface Node {

    Sticker select(CycleRenderer.StickerSelectionContext context, List<String> matchedCriteria);

    void setParent(ParentNode node);

    Optional<ParentNode> parent();

    String description();
  }

  private static class ParentNode implements Node {
    private final Predicate<CycleRenderer.StickerSelectionContext> predicate;
    private final Function<CycleRenderer.StickerSelectionContext, String> positiveExplanation;
    private final Function<CycleRenderer.StickerSelectionContext, String> negativeExplanation;
    private final String positiveQualifier;
    private final String negativeQualifier;
    private final String criteria;

    public Node branchTrue;
    public Node branchFalse;

    @Nullable
    private ParentNode parent;

    private ParentNode(
        Predicate<CycleRenderer.StickerSelectionContext> predicate,
        String positiveQualifier,
        String negativeQualifier,
        String criteria,
        Function<CycleRenderer.StickerSelectionContext, String> positiveExplanation,
        Function<CycleRenderer.StickerSelectionContext, String> negativeExplanation,
        Node branchTrue,
        Node branchFalse) {
      this.parent = null;
      this.predicate = predicate;
      this.positiveExplanation = positiveExplanation;
      this.negativeExplanation = negativeExplanation;
      this.criteria = criteria;
      this.branchTrue = branchTrue;
      this.branchFalse = branchFalse;
      this.negativeQualifier = negativeQualifier;
      this.positiveQualifier = positiveQualifier;

      branchTrue.setParent(this);
      branchFalse.setParent(this);
    }

    public Sticker select(CycleRenderer.StickerSelectionContext context, List<String> matchedCriteria) {
      boolean outcome = predicate.test(context);
      matchedCriteria.add(String.format("%s %s", outcome ? positiveQualifier : negativeQualifier, criteria));
      if (outcome) {
        return branchTrue.select(context, matchedCriteria);
      }
      return branchFalse.select(context, matchedCriteria);
    }

    public String getQualifier(boolean branch) {
      if (branch) {
        return positiveQualifier;
      }
      return negativeQualifier;
    }

    public String explanation(CycleRenderer.StickerSelectionContext context) {
      if (predicate.test(context)) {
        return positiveExplanation.apply(context);
      }
      return negativeExplanation.apply(context);
    }

    @Override
    public void setParent(ParentNode node) {
      if (parent != null) {
        throw new IllegalStateException(
            String.format("Node %s already had parent %s", description(), parent.description()));
      }
      parent = node;
    }

    @Nullable
    @Override
    public Optional<ParentNode> parent() {
      return Optional.ofNullable(parent);
    }

    @Override
    public String description() {
      return criteria;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ParentNode that = (ParentNode) o;
      return criteria.equals(that.criteria) &&
          branchTrue.equals(that.branchTrue) &&
          branchFalse.equals(that.branchFalse);
    }

    @Override
    public int hashCode() {
      return Objects.hash(criteria, branchTrue, branchFalse);
    }
  }

  private static class LeafNode implements Node {
    private final Sticker sticker;
    private ParentNode parent = null;

    private LeafNode(Sticker sticker) {
      this.sticker = sticker;
    }

    @Override
    public Sticker select(CycleRenderer.StickerSelectionContext context, List<String> matchedCriteria) {
      return sticker;
    }

    @Override
    public void setParent(ParentNode node) {
      if (parent != null) {
        throw new IllegalStateException(
            String.format("Node %s already had parent %s", description(), parent.description()));
      }
      parent = node;
    }

    @Nullable
    @Override
    public Optional<ParentNode> parent() {
      return Optional.ofNullable(parent);
    }

    @Override
    public String description() {
      return String.format("LEAF: %s", sticker.name());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LeafNode leafNode = (LeafNode) o;
      return sticker == leafNode.sticker;
    }

    @Override
    public int hashCode() {
      return Objects.hash(sticker);
    }
  }
}
