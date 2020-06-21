package com.bloomcyclecare.cmcc.ui.entry;

import com.bloomcyclecare.cmcc.data.models.observation.ClarifyingQuestion;
import com.google.common.base.Objects;

import androidx.annotation.Nullable;

class ClarifyingQuestionUpdate {

  final ClarifyingQuestion question;
  final Boolean answer;

  ClarifyingQuestionUpdate(ClarifyingQuestion question, Boolean answer) {
    this.question = question;
    this.answer = answer;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof ClarifyingQuestionUpdate) {
      ClarifyingQuestionUpdate that = (ClarifyingQuestionUpdate) obj;
      return Objects.equal(this.question, that.question) && Objects.equal(this.answer, that.answer);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(question, answer);
  }

  static ClarifyingQuestionUpdate emptyUpdate(ClarifyingQuestion question) {
    return new ClarifyingQuestionUpdate(question, null);
  }
}
