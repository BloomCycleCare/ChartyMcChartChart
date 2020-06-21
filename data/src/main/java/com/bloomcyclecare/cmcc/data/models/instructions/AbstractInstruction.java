package com.bloomcyclecare.cmcc.data.models.instructions;

public interface AbstractInstruction {
  String section();
  String subsection();
  String description();

  static String summary(AbstractInstruction instruction) {
    return String.format("%s (%s.%s)", instruction.description(), instruction.section(), instruction.subsection());
  }
}
