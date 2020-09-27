package com.bloomcyclecare.cmcc.data.serialization;

import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.instructions.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import org.apache.commons.codec.binary.Base32;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

public class InstructionsSerializer {
  private static final int NUM_INSTRUCTIONS =
      BasicInstruction.values().length + YellowStampInstruction.values().length + SpecialInstruction.values().length;

  private static final Base32 BASE_32 = new Base32();

  public static String encode(Instructions instructions, boolean spaceSeparator) {
    BitSet bitSet = new BitSet(NUM_INSTRUCTIONS);

    int offset = 0;
    offset += fillBitSet(bitSet, offset, BasicInstruction.class, instructions::isActive);
    offset += fillBitSet(bitSet, offset, YellowStampInstruction.class, instructions::isActive);
    offset += fillBitSet(bitSet, offset, SpecialInstruction.class, instructions::isActive);
    if (offset != NUM_INSTRUCTIONS) {
      throw new IllegalStateException();
    }

    StringBuilder encoded = new StringBuilder(BASE_32.encodeToString(bitSet.toByteArray()));
    while (encoded.length() < 16) {
      encoded.append('=');
    }

    String out = encoded.toString().replace('=', '0');
    if (spaceSeparator) {
      out = Joiner.on(" ").join(Splitter.fixedLength(4).split(out));
    }
    return out;
  }

  public static Instructions decode(String encoded) {
    String sanitized = encoded.replaceAll("\\s+","").replace('0', '=');
    byte[] bytes = BASE_32.decode(sanitized);

    BitSet bitSet = BitSet.valueOf(bytes);
    if (bitSet.size() < NUM_INSTRUCTIONS) {
      BitSet newSet = new BitSet(NUM_INSTRUCTIONS);
      newSet.or(bitSet);
      bitSet = newSet;
    }

    int offset = 0;
    List<BasicInstruction> basicInstructions = new ArrayList<>();
    List<YellowStampInstruction> yellowStampInstructions = new ArrayList<>();
    List<SpecialInstruction> specialInstructions = new ArrayList<>();
    offset += fillActiveInstructions(basicInstructions, bitSet, offset, BasicInstruction.class);
    offset += fillActiveInstructions(yellowStampInstructions, bitSet, offset, YellowStampInstruction.class);
    offset += fillActiveInstructions(specialInstructions, bitSet, offset, SpecialInstruction.class);
    if (offset != NUM_INSTRUCTIONS) {
      throw new IllegalStateException();
    }

    return new Instructions(LocalDate.now(), basicInstructions, specialInstructions, yellowStampInstructions);
  }

  private static <I extends Enum<I>> int fillBitSet(BitSet bitSet, int offset, Class<I> enumType, Function<I, Boolean> fn) {
    I[] values = enumType.getEnumConstants();
    for (int i = 0; i < values.length; i++) {
      bitSet.set(offset + i, fn.apply(values[i]));
    }
    return values.length;
  }

  private static <I extends Enum<I>> int fillActiveInstructions(List<I> out, BitSet bitSet, int offset, Class<I> enumType) {
    I[] values = enumType.getEnumConstants();
    for (int i=0; i < values.length; i++) {
      if (bitSet.get(i + offset)) {
        out.add(values[i]);
      }
    }
    return values.length;
  }
}
