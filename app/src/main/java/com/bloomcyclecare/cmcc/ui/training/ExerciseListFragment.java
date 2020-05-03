package com.bloomcyclecare.cmcc.ui.training;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.ui.entry.list.grid.EntryGridPageFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class ExerciseListFragment extends Fragment {

  private final ExerciseListAdapter mAdapter = new ExerciseListAdapter(this::onExerciseClick);
  private TrainingViewModel mActivityViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mActivityViewModel = new ViewModelProvider(getActivity()).get(TrainingViewModel.class);
    mActivityViewModel.completeTransition("Training Exercises", "please select an exercise below");
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_exercise_list, container, false);

    RecyclerView recyclerView = view.findViewById(R.id.recyclerview_exercise_list);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setAdapter(mAdapter);

    mAdapter.updateData(Exercise.COLLECTION);

    ExerciseListViewModel exerciseListViewModel = ViewModelProviders.of(this).get(ExerciseListViewModel.class);
    exerciseListViewModel.viewState().observe(getViewLifecycleOwner(), this::render);

    return view;
  }

  private void onExerciseClick(Exercise exercise) {
    Timber.i("Click %s", exercise.id().name());
    Bundle args = new Bundle();
    args.putInt(Exercise.ID.class.getCanonicalName(), exercise.id().ordinal());
    args.putInt(ViewMode.class.getCanonicalName(), ViewMode.TRAINING.ordinal());

    Fragment fragment = new EntryGridPageFragment();
    fragment.setArguments(args);
    mActivityViewModel.transitionToFragment(fragment);
    mActivityViewModel.completeTransition("Training Exercise", exercise.id().name());
  }

  private void render(ExerciseListViewModel.ViewState viewState) {
    mAdapter.updateData(viewState.exercises());
  }
}
