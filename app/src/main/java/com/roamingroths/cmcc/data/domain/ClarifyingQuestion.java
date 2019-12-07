package com.roamingroths.cmcc.data.domain;

public enum ClarifyingQuestion {
  ESSENTIAL_SAMENESS("Essentially the same?", "Some description here"),
  POINT_OF_CHANGE("Point of change?", "Some description here"),
  UNUSUAL_BLEEDING("Unusual bleeding?", "Some description here"),
  UNUSUAL_BUILDUP("Unusual buildup?", "Some description here"),
  UNUSUAL_STRESS("Unusual stress?", "Some description here");

  public final String title;
  public final String message;

  ClarifyingQuestion(String title, String message) {
    this.title = title;
    this.message = message;
  }
}
