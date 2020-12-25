package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.instructions.AbstractInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.utils.DecisionTree;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class StickerSelector {

  private static final ImmutableMap<Sticker, DecisionTree.Node> LEAF_NODES = ImmutableMap.<Sticker, DecisionTree.Node>builder()
      .put(Sticker.RED, new DecisionTree.LeafNode(Sticker.RED))
      .put(Sticker.GREEN, new DecisionTree.LeafNode(Sticker.GREEN))
      .put(Sticker.GREEN_BABY, new DecisionTree.LeafNode(Sticker.GREEN_BABY))
      .put(Sticker.YELLOW, new DecisionTree.LeafNode(Sticker.YELLOW))
      .put(Sticker.YELLOW_BABY, new DecisionTree.LeafNode(Sticker.YELLOW_BABY))
      .put(Sticker.WHITE_BABY, new DecisionTree.LeafNode(Sticker.WHITE_BABY))
      .put(Sticker.GREY, new DecisionTree.LeafNode(Sticker.GREY))
      .build();

  static final String BLEEDING_POSITIVE_REASON = "has bleeding";
  static final String BLEEDING_NEGATIVE_REASON = "doesn't have bleeding";
  static final String BLEEDING_POSITIVE_EXPLANATION = "Observation had either H, M, L, VL, B or R";
  static final String BLEEDING_NEGATIVE_EXPLANATION = "No signs of bleeding present in observation (i.e., H, M, L, VL, B or R)";

  static final String MUCUS_POSITIVE_REASON = "has mucus";
  static final String MUCUS_NEGATIVE_REASON = "doesn't have mucus";
  static final String MUCUS_POSITIVE_EXPLANATION = "Observation had either 6, 8 or 10";
  static final String MUCUS_NEGATIVE_EXPLANATION = "No signs of mucus present in observation (i.e., 6, 8 or 10)";

  static final String FERTILE_POSITIVE_REASON = "is fertile";
  static final String FERTILE_NEGATIVE_REASON = "isn't fertile";
  static final String FERTILE_POSITIVE_EXPLANATION = "Active fertility instructions: ";
  static final String FERTILE_NEGATIVE_EXPLANATION = "No fertility instructions (D.1 - D.6) are active";

  static final String INFERTILE_POSITIVE_REASON = "has active special instructions";
  static final String INFERTILE_NEGATIVE_REASON = "doesn't have active special instructions";
  static final String INFERTILE_POSITIVE_EXPLANATION = "Active special instructions: ";
  static final String INFERTILE_NEGATIVE_EXPLANATION = "No special instructions apply to warrant yellow stamps";

  static final String FLOW_POSITIVE_REASON = "is in menstrual flow";
  static final String FLOW_NEGATIVE_REASON = "isn't in menstrual flow";
  static final String FLOW_POSITIVE_EXPLANATION = "Observation was part of uninterrupted flow at start of cycle";
  static final String FLOW_NEGATIVE_EXPLANATION = "Observation was not part of uninterrupted flow at the start of cycle";

  private static final BiPredicate<Boolean, CycleRenderer.StickerSelectionContext> ALWAYS_LOG = (b, c) -> true;
  private static final BiPredicate<Boolean, CycleRenderer.StickerSelectionContext> ONLY_LOG_NEGATIVE = (b, c) -> !b;

  private static final DecisionTree.Node TREE = new DecisionTree.ParentNode(
      "Has observation and instructions",
      DecisionTree.Criteria.and(
          DecisionTree.Criteria.create(
              c -> c.hasObservation,
              c -> "has observation",
              c -> "doesn't have observation",
              c -> "",
              c -> ""
          ),
          DecisionTree.Criteria.create(
              c -> c.hasInstructions,
              c -> "has instructions",
              c -> "doesn't have instructions",
              c -> "",
              c -> ""
          )
      ),
      ONLY_LOG_NEGATIVE,
      new DecisionTree.ParentNode(
          "In flow or is fertile",
          DecisionTree.Criteria.or(
              DecisionTree.Criteria.create(
                  c -> c.inFlow,
                  c -> FLOW_POSITIVE_REASON,
                  c -> FLOW_NEGATIVE_REASON,
                  c -> FLOW_POSITIVE_EXPLANATION,
                  c -> FLOW_NEGATIVE_EXPLANATION
              ),
              DecisionTree.Criteria.create(
                  c -> !c.fertilityReasons.isEmpty() || (c.fertilityReasons.isEmpty() && c.infertilityReasons.isEmpty()),
                  c -> FERTILE_POSITIVE_REASON,
                  c -> FERTILE_NEGATIVE_REASON,
                  c -> FERTILE_POSITIVE_EXPLANATION + Joiner.on(", ").join(c.fertilityReasons.stream().map(AbstractInstruction::description).collect(Collectors.toList())),
                  c -> FERTILE_NEGATIVE_EXPLANATION
              )
          ),
          ALWAYS_LOG,
          new DecisionTree.ParentNode(
              "Has bleeding",
              DecisionTree.Criteria.create(
                  c -> c.hasBleeding,
                  c -> BLEEDING_POSITIVE_REASON,
                  c -> BLEEDING_NEGATIVE_REASON,
                  c -> BLEEDING_POSITIVE_EXPLANATION,
                  c -> BLEEDING_NEGATIVE_EXPLANATION
              ),
              ALWAYS_LOG,
              LEAF_NODES.get(Sticker.RED),
              new DecisionTree.ParentNode(
                  "Has mucus",
                  DecisionTree.Criteria.create(
                      c -> c.hasMucus,
                      c -> MUCUS_POSITIVE_REASON,
                      c -> MUCUS_NEGATIVE_REASON,
                      c -> MUCUS_POSITIVE_EXPLANATION,
                      c -> MUCUS_NEGATIVE_EXPLANATION
                  ),
                  ALWAYS_LOG,
                  new DecisionTree.ParentNode(
                      "Has infertility reasons",
                      DecisionTree.Criteria.create(
                          c -> !c.infertilityReasons.isEmpty(),
                          c -> INFERTILE_POSITIVE_REASON,
                          c -> INFERTILE_NEGATIVE_REASON,
                          c -> INFERTILE_POSITIVE_EXPLANATION + Joiner.on(", ").join(c.infertilityReasons.stream().map(AbstractInstruction::description).collect(Collectors.toList())),
                          c -> INFERTILE_NEGATIVE_EXPLANATION
                      ),
                      (b, c) -> c.hasSpecialInstructions,
                      LEAF_NODES.get(Sticker.YELLOW_BABY),
                      LEAF_NODES.get(Sticker.WHITE_BABY)
                  ),
                  LEAF_NODES.get(Sticker.GREEN_BABY)
              )
          ),
          new DecisionTree.ParentNode(
              "Has mucus",
              DecisionTree.Criteria.create(
                  c -> c.hasMucus,
                  c -> MUCUS_POSITIVE_REASON,
                  c -> MUCUS_NEGATIVE_REASON,
                  c -> MUCUS_POSITIVE_EXPLANATION,
                  c -> MUCUS_NEGATIVE_EXPLANATION
              ),
              ALWAYS_LOG,
              LEAF_NODES.get(Sticker.YELLOW),
              LEAF_NODES.get(Sticker.GREEN)
          )
      ),
      LEAF_NODES.get(Sticker.GREY)
  );

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

    Set<DecisionTree.ParentNode> ancestors = DecisionTree.ancestors(LEAF_NODES.get(expectedResult.sticker));
    DecisionTree.Node currentNode = LEAF_NODES.get(selection);
    Optional<DecisionTree.ParentNode> parentNode = currentNode.parent();
    boolean pathDir = false;
    while (parentNode.isPresent() && (
        LEAF_NODES.containsValue(currentNode) ||
        !ancestors.contains((DecisionTree.ParentNode) currentNode))) {
      if (currentNode == parentNode.get().branchTrue) {
        pathDir = true;
      } else if (currentNode == parentNode.get().branchFalse) {
        pathDir = false;
      } else {
        throw new IllegalStateException(
            String.format("Node %s is not a child of %s",
                currentNode.description(), parentNode.get().description()));
      }
      currentNode = parentNode.get();
      parentNode = currentNode.parent();
    }
    if (!parentNode.isPresent()) {
      throw new IllegalStateException("All leaf nodes should have at least one parent! node: " + currentNode);
    }
    return CheckResult.incorrect(
        expectedResult.sticker,
        String.format("Can't be %s, today %s",
            selection.name(),
            ((DecisionTree.ParentNode) currentNode).critera.getReason(!pathDir, context)),
        ((DecisionTree.ParentNode) currentNode).critera.getExplanation(!pathDir, context),
        String.format("Today %s", Joiner.on(", ").join(expectedResult.matchedCriteria)));
  }

  public static class CheckResult {
    Sticker expected;
    Optional<String> errorMessage = Optional.empty();
    Optional<String> errorExplanation = Optional.empty();
    Optional<String> hint = Optional.empty();

    static CheckResult ok(Sticker expected) {
      CheckResult result = new CheckResult();
      result.expected = expected;
      return result;
    }

    static CheckResult incorrect(Sticker expected, String errorMessage, String errorExplanation, String hint) {
      CheckResult result = new CheckResult();
      result.expected = expected;
      result.errorMessage = Optional.of(errorMessage);
      result.errorExplanation = Optional.of(errorExplanation);
      result.hint = Optional.of(hint);
      return result;
    }

    public boolean ok() {
      return !errorMessage.isPresent();
    }
  }
}
