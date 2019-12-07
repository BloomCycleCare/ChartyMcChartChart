package com.roamingroths.cmcc.ui.entry.detail;

import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.domain.ClarifyingQuestion;

import io.reactivex.subjects.Subject;

class ClarifyingQuestionViewHolder extends RecyclerView.ViewHolder {

  private final TextView mTitleView;
  private final Switch mValueSwitch;

  ClarifyingQuestionViewHolder(@NonNull View itemView) {
    super(itemView);

    mTitleView = itemView.findViewById(R.id.tv_question_title);
    mValueSwitch = itemView.findViewById(R.id.switch_answer);
  }

  void bind(ClarifyingQuestion clarifyingQuestion, Boolean isChecked, Subject<ClarifyingQuestionUpdate> updateSubject) {
    mTitleView.setText(clarifyingQuestion.title);
    mValueSwitch.setChecked(isChecked);
    updateSubject.onNext(new ClarifyingQuestionUpdate(clarifyingQuestion, isChecked));

    RxCompoundButton
        .checkedChanges(mValueSwitch)
        .map(v -> new ClarifyingQuestionUpdate(clarifyingQuestion, v))
    .subscribe(updateSubject);
  }
}
