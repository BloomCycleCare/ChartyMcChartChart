package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

public enum TosItem {
  MEDICAL_AGREEMENT(
      "Medical Agreement",
      "I acknowledge that CMCC is not a medical tool. The content does not substitute for professional medical advice, diagnosis, or treatment. Always seek the advice of a qualified medical professional or healthcare provider for any questions you have regarding a medical condition."
  ),
  CRMS_AGREEMENET(
      "CRMS Agreement",
        "I agree to only use CMCC to track my cycle using the Creighton Model FertilityCare System with the supervision of a qualified FertilityCare Practitioner (FCP) or a FertilityCare Practitioner Intern (FCPI)."
  ),
  PRACTITIONER_AGREEMENT(
      "FCP/FCPI Agreement",
        "I acknowledge that the FertilityCare System is highly effective only when used appropriately with the guidance of my FCP/FCPI."
  ),
  RESPONSIBILITY_AGREEMENT(
      "Responsibility Agreement",
        "I acknowledge that I am responsible for all of my choices and decisions and any resulting events."
  ),
  TESTING_AGREEMENT(
  "Testing Agreement",
       "I acknowledge that CMCC is in a testing phase."
  );

  public final String summary;
  public final String content;

  TosItem(String summary, String content) {
    this.summary = summary;
    this.content = content;
  }
}
