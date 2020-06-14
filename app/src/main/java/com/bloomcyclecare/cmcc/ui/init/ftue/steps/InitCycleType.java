package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

public enum InitCycleType {
  UNKNOWN("", ""),
  POST_PARTUM(
      "Test Date",
      "Something here saying select the date of the positive pregnancy test"
  ),
  PREGNANT(
    "Delivery Date",
    "Something here saying select the delivery date"
  ),
  OTHER(
    "First Day of Last Period",
    "Something here saying select the first day of your last period"
  );

  final String dialogTitle;
  final String promptText;

  InitCycleType(String dialogTitle,String promptText){
    this.dialogTitle = dialogTitle;
    this.promptText = promptText;
  }
}
