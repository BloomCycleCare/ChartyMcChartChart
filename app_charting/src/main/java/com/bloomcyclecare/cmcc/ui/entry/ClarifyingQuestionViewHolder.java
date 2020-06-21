package com.bloomcyclecare.cmcc.ui.entry;

import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.observation.ClarifyingQuestion;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
