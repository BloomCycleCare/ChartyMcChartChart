package com.bloomcyclecare.cmcc.models.observation;

public enum ClarifyingQuestion {
  ESSENTIAL_SAMENESS("Essentially the same?", "Is today essentially the same as yesterday?"),
  POINT_OF_CHANGE("Point of change?", "You have indicated today is not essentially the same. Is it a point of change?"),
  UNUSUAL_BLEEDING("Unusual bleeding?", "Is this unusual bleeding?"),
  UNUSUAL_BUILDUP("Unusual buildup?", "Did you observe an unusual buildup?"),
  UNUSUAL_STRESS("Unusual stress?", "Were you under unusual amounts of stress?");

  public final String title;
  public final String message;

  ClarifyingQuestion(String title, String message) {
    this.title = title;
    this.message = message;
  }
}
