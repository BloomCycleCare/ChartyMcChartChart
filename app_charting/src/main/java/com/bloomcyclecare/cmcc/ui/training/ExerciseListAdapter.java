package com.bloomcyclecare.cmcc.ui.training;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.Exercise;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

class ExerciseListAdapter extends RecyclerView.Adapter<ExerciseListViewHolder> {

  private final List<Exercise> mExercises = new ArrayList<>();
  private final Consumer<Exercise> mClickConsumer;

  ExerciseListAdapter(Consumer<Exercise> clickConsumer) {
    this.mClickConsumer = clickConsumer;
  }

  void updateData(List<Exercise> exercises) {
    mExercises.clear();
    mExercises.addAll(exercises);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ExerciseListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View view = inflater.inflate(R.layout.list_item_exercise, parent, false);
    return new ExerciseListViewHolder(view, mClickConsumer);
  }

  @Override
  public void onBindViewHolder(@NonNull ExerciseListViewHolder holder, int position) {
    holder.bind(mExercises.get(position));
  }

  @Override
  public int getItemCount() {
    return mExercises.size();
  }
}
