package com.roamingroths.cmcc.data.domain;

public enum SpecialInstruction {
  BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS("Yellow stamps for seminal fluid while breastfeeding."),
  PRE_PEAK_YELLOW_STAMPS("Yellow stamps for days of mucus PRE peak"),
  POST_PEAK_YELLOW_STAMPS("Yellow stamps for days of mucus POST peak")
  ;

  public String description;

  SpecialInstruction(String description) {
    this.description = description;
  }
}
