package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

public enum InitCycleType {
  UNKNOWN("", ""),
  TYPICAL(
      "First Day of Last Period",
      "Something here saying select the first day of your last period"
  ),
  POST_PARTUM(
      "Test Date",
      "Something here saying select the date of the positive pregnancy test"
  ),
  PREGNANT(
    "Delivery Date",
    "Something here saying select the delivery date"
  );

  final String dialogTitle;
  final String promptText;

  InitCycleType(String dialogTitle,String promptText){
    this.dialogTitle = dialogTitle;
    this.promptText = promptText;
  }
}
