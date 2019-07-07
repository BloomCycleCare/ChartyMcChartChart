package com.roamingroths.cmcc.data.domain;

public enum YellowStampInstruction implements AbstractInstruction {
  YS_1_A(1, 'a', ""),
  YS_1_B(1, 'b', ""),
  YS_1_C(1, 'c', ""),
  YS_1_D(1, 'd', ""),
  YS_2_A(2, 'a', ""),
  YS_2_B(2, 'b', ""),
  YS_2_C(2, 'c', ""),
  YS_2_D(2, 'd', ""),
  YS_3_A(3, 'a', ""),
  YS_3_B(3, 'b', "");

  private final int section;
  private final char subSection;
  private final String description;

  YellowStampInstruction(int section, char subSection, String description) {
    this.section = section;
    this.subSection = subSection;
    this.description = description;
  }

  @Override
  public String section() {
    return String.valueOf(section);
  }

  @Override
  public String subsection() {
    return String.valueOf(subSection);
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public String toString() {
    return description;
  }
}
