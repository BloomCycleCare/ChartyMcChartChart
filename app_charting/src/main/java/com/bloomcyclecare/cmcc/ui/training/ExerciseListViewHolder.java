package com.bloomcyclecare.cmcc.ui.training;

import android.view.View;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

class ExerciseListViewHolder extends RecyclerView.ViewHolder {

  private final TextView mTitle;

  private Exercise mBoundExercise;

  ExerciseListViewHolder(@NonNull View itemView, Consumer<Exercise> clickConsumer) {
    super(itemView);
    mTitle = itemView.findViewById(R.id.tv_exercise_title);
    itemView.setOnClickListener(v -> clickConsumer.accept(mBoundExercise));
  }

  void bind(Exercise exercise) {
    mBoundExercise = exercise;
    mTitle.setText(exercise.id().name());
  }
}
