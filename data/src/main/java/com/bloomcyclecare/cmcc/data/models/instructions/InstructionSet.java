package com.bloomcyclecare.cmcc.data.models.instructions;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

@Parcel
@NotThreadSafe
public class InstructionSet extends ForwardingSet<AbstractInstruction> implements Set<AbstractInstruction> {

  private final transient Set<AbstractInstruction> delegate;
  public final Set<BasicInstruction> basicInstructions = new HashSet<>();
  public final Set<YellowStampInstruction> yellowStampInstructions = new HashSet<>();
  public final Set<SpecialInstruction> specialInstructions = new HashSet<>();

  public InstructionSet() {
    Set<AbstractInstruction> scan = new HashSet<>();
    scan = Sets.union(scan, basicInstructions);
    scan = Sets.union(scan, yellowStampInstructions);
    scan = Sets.union(scan, specialInstructions);
    delegate = scan;
  }

  public static InstructionSet of(AbstractInstruction... instructions) {
    InstructionSet set = new InstructionSet();
    set.addAll(Arrays.asList(instructions));
    return set;
  }

  @Override
  public boolean addAll(Collection<? extends AbstractInstruction> collection) {
    boolean updated = false;
    for (AbstractInstruction i : collection) {
      updated |= add(i);
    }
    return updated;
  }

  @Override
  public boolean add(@NotNull AbstractInstruction i) {
    if (i instanceof BasicInstruction) {
      return basicInstructions.add((BasicInstruction) i);
    }
    if (i instanceof YellowStampInstruction) {
      return yellowStampInstructions.add((YellowStampInstruction) i);
    }
    if (i instanceof SpecialInstruction) {
      return specialInstructions.add((SpecialInstruction) i);
    }
    throw new IllegalArgumentException();
  }

  @Override
  public boolean remove(Object o) {
    if (o instanceof BasicInstruction) {
      return basicInstructions.remove(o);
    }
    if (o instanceof YellowStampInstruction) {
      return yellowStampInstructions.remove(o);
    }
    if (o instanceof SpecialInstruction) {
      return specialInstructions.remove(o);
    }
    throw new IllegalArgumentException();
  }

  @ParcelConstructor
  public InstructionSet(Set<BasicInstruction> basicInstructions, Set<YellowStampInstruction> yellowStampInstructions, Set<SpecialInstruction> specialInstructions) {
    this();
    this.basicInstructions.addAll(basicInstructions);
    this.yellowStampInstructions.addAll(yellowStampInstructions);
    this.specialInstructions.addAll(specialInstructions);
  }

  @Override
  protected Set<AbstractInstruction> delegate() {
    return delegate;
  }
}
