package com.roamingroths.cmcc.ui.entry.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.domain.ClarifyingQuestion;
import com.roamingroths.cmcc.utils.RxUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

class ClarifyingQuestionAdapter extends RecyclerView.Adapter<ClarifyingQuestionViewHolder> {

  private final Context mContext;
  private final List<ClarifyingQuestionUpdate> mInitialValues = new ArrayList<>();
  private final Subject<ClarifyingQuestionUpdate> mUpdateStream = PublishSubject.create();

  ClarifyingQuestionAdapter(Context mContext) {
    this.mContext = mContext;
  }

  void updateQuestions(List<ClarifyingQuestionUpdate> updatedValues) {
    Set<ClarifyingQuestion> questions = new HashSet<>(mInitialValues.size());
    for (ClarifyingQuestionUpdate u : mInitialValues) {
      questions.add(u.question);
    }
    boolean newEntryPresent = false;
    for (ClarifyingQuestionUpdate u : updatedValues) {
      newEntryPresent |= !questions.remove(u.question);
    }
    if (!newEntryPresent && questions.isEmpty()) {
      return;
    }
    mInitialValues.clear();
    mInitialValues.addAll(updatedValues);
    notifyDataSetChanged();
  }

  public Observable<List<ClarifyingQuestionUpdate>> updates() {
    return RxUtil
        .aggregateLatest(mUpdateStream.toFlowable(BackpressureStrategy.BUFFER), u -> u.question)
        .toObservable();
  }

  @NonNull
  @Override
  public ClarifyingQuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    int layoutIdForListItem = R.layout.list_item_clarifying_question;
    LayoutInflater inflater = LayoutInflater.from(mContext);

    View view = inflater.inflate(layoutIdForListItem, parent, false);
    return new ClarifyingQuestionViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ClarifyingQuestionViewHolder holder, int position) {
    ClarifyingQuestionUpdate initialValue = mInitialValues.get(position);
    holder.bind(initialValue.question, initialValue.answer, mUpdateStream);
  }

  @Override
  public int getItemCount() {
    return mInitialValues.size();
  }
}
