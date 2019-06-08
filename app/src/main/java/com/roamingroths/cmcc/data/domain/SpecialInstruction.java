package com.roamingroths.cmcc.data.domain;

public enum SpecialInstruction {
  BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS("Yellow stamps for seminal fluid while breastfeeding.");

  public String description;

  SpecialInstruction(String description) {
    this.description = description;
  }
}
