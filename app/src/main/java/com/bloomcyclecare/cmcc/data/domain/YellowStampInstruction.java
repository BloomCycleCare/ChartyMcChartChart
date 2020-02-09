package com.bloomcyclecare.cmcc.data.domain;

public enum YellowStampInstruction implements AbstractInstruction {
  YS_1_A(1, 'a', "Menstrual flow"),
  YS_1_B(1, 'b', "From the point of change through three full days past the Peak"),
  YS_1_C(1, 'c', "Any single day of change -- plus count of three"),
  YS_1_D(1, 'd', "Any unusual bleeding -- plus count of three"),
  YS_2_A(2, 'a', "Pre-Peak -- end of the day, alternate days"),
  YS_2_B(2, 'b', "Post-Peak (after fourth day) -- end of the day, alternate days"),
  YS_2_C(2, 'c', "Post-Peak (after fourth day) -- end of the day, every day"),
  YS_2_D(2, 'd', "Post-Peak (after fourth day) -- any time of day"),
  YS_3_A(3, 'a', "Discontinue use when period starts"),
  YS_3_B(3, 'b', "Discontinue pre-Peak yellow stamps in regular cycles when mucus cycle < 9 days");

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
