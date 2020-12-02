package com.bloomcyclecare.cmcc.utils;

import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

public class DecisionTree {

  public static Set<Node> ancestors(Node node) {
    Set<Node> out = new HashSet<>();
    Node currentNode = node;
    while (currentNode.parent().isPresent()) {
      currentNode = currentNode.parent().get();
      out.add(currentNode);
    }
    return out;
  }

  public interface Node {

    Sticker select(CycleRenderer.StickerSelectionContext context, List<String> matchedCriteria);

    void setParent(ParentNode node);

    Optional<ParentNode> parent();

    String description();
  }

  public static class Criteria {
    private final Predicate<CycleRenderer.StickerSelectionContext> predicate;
    private final Function<CycleRenderer.StickerSelectionContext, String> positiveReason;
    private final Function<CycleRenderer.StickerSelectionContext, String> negativeReason;
    private final Function<CycleRenderer.StickerSelectionContext, String> positiveExplanation;
    private final Function<CycleRenderer.StickerSelectionContext, String> negativeExplanation;

    public static Criteria create(
        Predicate<CycleRenderer.StickerSelectionContext> predicate,
        Function<CycleRenderer.StickerSelectionContext, String> positiveReason,
        Function<CycleRenderer.StickerSelectionContext, String> negativeReason,
        Function<CycleRenderer.StickerSelectionContext, String> positiveExplanation,
        Function<CycleRenderer.StickerSelectionContext, String> negativeExplanation) {
      return new Criteria(predicate, positiveReason, negativeReason, positiveExplanation, negativeExplanation);
    }

    public static Criteria and(Criteria criteriaA, Criteria criteriaB) {
      return new And(criteriaA, criteriaB);
    }

    public static Criteria or(Criteria criteriaA, Criteria criteriaB) {
      return new Or(criteriaA, criteriaB);
    }

    Criteria(
        Predicate<CycleRenderer.StickerSelectionContext> predicate,
        Function<CycleRenderer.StickerSelectionContext, String> positiveReason,
        Function<CycleRenderer.StickerSelectionContext, String> negativeReason,
        Function<CycleRenderer.StickerSelectionContext, String> positiveExplanation,
        Function<CycleRenderer.StickerSelectionContext, String> negativeExplanation) {
      this.predicate = predicate;
      this.positiveReason = positiveReason;
      this.negativeReason = negativeReason;
      this.positiveExplanation = positiveExplanation;
      this.negativeExplanation = negativeExplanation;
    }

    public String getExplanation(boolean outcome, CycleRenderer.StickerSelectionContext context) {
      if (outcome) {
        return positiveExplanation.apply(context);
      }
      return negativeExplanation.apply(context);
    }

    public String getReason(boolean outcome, CycleRenderer.StickerSelectionContext context) {
      if (outcome) {
        return positiveReason.apply(context);
      }
      return negativeReason.apply(context);
    }
  }

  public static class And extends Criteria {
    And(Criteria criteriaA, Criteria criteriaB) {
      super(
          c -> criteriaA.predicate.test(c) && criteriaB.predicate.test(c),
          c -> String.format("%s AND %s", criteriaA.positiveReason.apply(c), criteriaB.positiveReason.apply(c)),
          c -> {
            if (criteriaA.predicate.test(c)) {
              return criteriaB.negativeReason.apply(c);
            } else if (criteriaB.predicate.test(c)) {
              return criteriaA.negativeReason.apply(c);
            } else {
              return String.format("%s AND %s", criteriaA.negativeReason.apply(c), criteriaB.negativeReason.apply(c));
            }
          },
          c -> String.format("%s AND %s", criteriaA.positiveExplanation.apply(c), criteriaB.positiveExplanation.apply(c)),
          c -> {
            if (criteriaA.predicate.test(c)) {
              return criteriaB.negativeExplanation.apply(c);
            } else if (criteriaB.predicate.test(c)) {
              return criteriaA.negativeExplanation.apply(c);
            } else {
              return String.format("%s AND %s", criteriaA.negativeExplanation.apply(c), criteriaB.negativeExplanation.apply(c));
            }
          });
    }
  }

  public static class Or extends Criteria {
    Or(Criteria criteriaA, Criteria criteriaB) {
      super(
          c -> criteriaA.predicate.test(c) || criteriaB.predicate.test(c),
          c -> {
            if (criteriaA.predicate.test(c)) {
              return criteriaA.positiveReason.apply(c);
            } else if (criteriaB.predicate.test(c)) {
              return criteriaB.positiveReason.apply(c);
            } else {
              throw new IllegalStateException();
            }
          },
          c -> String.format("Neither %s NOR %s", criteriaA.positiveReason.apply(c), criteriaB.positiveReason.apply(c)),
          c -> {
            if (criteriaA.predicate.test(c)) {
              return criteriaA.positiveExplanation.apply(c);
            } else if (criteriaB.predicate.test(c)) {
              return criteriaB.positiveExplanation.apply(c);
            } else {
              throw new IllegalStateException();
            }
          },
          c -> String.format("%s AND %s", criteriaA.negativeExplanation.apply(c), criteriaB.negativeExplanation.apply(c)));
    }
  }

  public static class ParentNode implements Node {

    public Criteria critera;
    public Node branchTrue;
    public Node branchFalse;
    private final boolean logPositiveMatch;

    @Nullable
    private ParentNode parent;

    public ParentNode(
        Criteria criteria,
        boolean logPositiveMatch,
        Node branchTrue,
        Node branchFalse) {
      this.parent = null;
      this.branchTrue = branchTrue;
      this.branchFalse = branchFalse;
      this.critera = criteria;
      this.logPositiveMatch = logPositiveMatch;

      branchTrue.setParent(this);
      branchFalse.setParent(this);
    }

    public Sticker select(CycleRenderer.StickerSelectionContext context, List<String> matchedCriteria) {
      if (critera.predicate.test(context)) {
        if (logPositiveMatch) {
          matchedCriteria.add(critera.positiveReason.apply(context));
        }
        return branchTrue.select(context, matchedCriteria);
      }
      matchedCriteria.add(critera.negativeReason.apply(context));
      return branchFalse.select(context, matchedCriteria);
    }

    public String explanation(CycleRenderer.StickerSelectionContext context) {
      if (critera.predicate.test(context)) {
        return critera.positiveExplanation.apply(context);
      }
      return critera.negativeExplanation.apply(context);
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
      return "foo";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ParentNode that = (ParentNode) o;
      return branchTrue.equals(that.branchTrue) &&
          branchFalse.equals(that.branchFalse);
    }

    @Override
    public int hashCode() {
      return Objects.hash(branchTrue, branchFalse);
    }
  }

  public static class LeafNode implements Node {
    private final Sticker sticker;
    private ParentNode parent = null;

    public LeafNode(Sticker sticker) {
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
