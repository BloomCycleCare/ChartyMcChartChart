package com.bloomcyclecare.cmcc.data.domain;

import com.google.common.collect.ImmutableSet;

public enum BasicInstruction implements AbstractInstruction {
  A('A', null, "Always keep to the observation routine."),
  B('B', null, "Chart at the end of your day, every day, and record the most fertile sign of the day."),
  C('C', null, "Avoid genital contact"),
  // D: Days of fertility (use to achieve pregnancy)
  D_1('D', 1, "The menstrual flow"),
  D_2('D', 2, "From the beginning of mucus until 3 full days post peak."),
  D_3('D', 3, "1 or 2 days of non-Peak mucus pre-Peak"),
  D_4('D', 4, "3 or more days of non-Peak mucus pre-Peak -- plus count of 3"),
  D_5('D', 5, "Any single day of Peak mucus -- plus count 3"),
  D_6('D', 6, "Any unusual bleeding -- plus count 3"),
  // E: Days of infertility (use to avoid pregnancy)
  E_1('E', 1, "Dry days pre-Peak -- end of the day, alternate days"),
  E_2('E', 2, "Dry days pre-Peak -- end of the day, every day"),
  E_3('E', 3, "4th day post-Peak -- always end of the day"),
  E_4('E', 4, "Dry days post-Peak (after 4th day) -- end of the day, alternate days"),
  E_5('E', 5, "Dry days post-Peak (after 4th day) -- end of the day, every day"),
  E_6('E', 6, "Dry days post-Peak (after 4th day) -- any time of the day"),
  E_7('E', 7, "Dry days on L, VL or B days of bleeding at end of the menstrual flow -- end of the day"),
  F('F', null, "Seminal fluid instruction"),
  // G: "Double" Peak
  G_1('G', 1, "On P+3 ask double peak questions"),
  G_2('G', 2, "If post-Peak phase is greater than 16 days in duration and system used properly to avoid pregnancy, anticipate missed period form of double peak"),
  G_3('G', 3, "When anticipating double Peak, keep to the end of infertile days and continue good observations"),
  H('H', null, "When in doubt, consider yourself of peak fertility and count 3"),

  // TODO: I

  J('J', null, "Essential samness quesiton -- Is today essentially the same as yesterday? -- yes or no"),

  K_1('K', 1, "Pre-Peak -- end of the day, alternate days"),
  K_2('K', 2, "Post-Peak (after the fourth day) -- end of the day, alternate days"),
  K_3('K', 3, "Post-Peak (after the fourth day) -- end of the day, every day"),
  K_4('K', 4, "Post-Peak (after the fourth day) -- any time of day"),
  K_5('K', 5, "Discontinue use when period starts"),
  K_6('K', 6, "Discontinue pre-Peak Y.S. in regular cycles when mucus cycle < 9 days"),

  // TODO: L

  M('M', null, "End of day instructions apply through the first normal menstrual cycle"),

  // TODO: N, O
  ;

  private final char section;
  private final Integer subSection;
  private final String description;

  BasicInstruction(char section, Integer subSection, String description) {
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
    if (subSection == null) {
      return "";
    }
    return subSection.toString();
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public String toString() {
    return String.format("%s%s %s", section(), subsection(), description);
  }

  public static ImmutableSet<BasicInstruction> postPeakYellowBasicInstructions = ImmutableSet.of(K_2, K_3, K_4);
  public static ImmutableSet<BasicInstruction> suppressableByPrePeakYellow = ImmutableSet.of(D_2, D_3, D_4, D_5);
  public static ImmutableSet<BasicInstruction> suppressableByPostPeakYellow = ImmutableSet.of(D_5);
}
