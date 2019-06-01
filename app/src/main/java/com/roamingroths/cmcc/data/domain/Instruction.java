package com.roamingroths.cmcc.data.domain;

public enum Instruction {
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

  // TODO: K, L, M, N, O
  ;

  public final char section;
  public final Integer subSection;
  public final String description;

  Instruction(char section, Integer subSection, String description) {
    this.section = section;
    this.subSection = subSection;
    this.description = description;
  }
}
