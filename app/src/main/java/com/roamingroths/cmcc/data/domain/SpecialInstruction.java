package com.roamingroths.cmcc.data.domain;

public enum SpecialInstruction implements AbstractInstruction {
  BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS("Yellow stamps for seminal fluid while breastfeeding."),
  ;

  public String description;

  SpecialInstruction(String description) {
    this.description = description;
  }

  @Override
  public String section() {
    return null;
  }

  @Override
  public String subsection() {
    return null;
  }

  @Override
  public String description() {
    return description;
  }
}
