package com.bloomcyclecare.cmcc.data.models.instructions;

public enum SpecialInstruction implements AbstractInstruction {
  BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS("Yellow stamps for seminal fluid while breastfeeding."),
  ;

  private final String description;

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
