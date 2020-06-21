package com.bloomcyclecare.cmcc.ui.training;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class ExerciseListFragment extends Fragment {

  private final ExerciseListAdapter mAdapter = new ExerciseListAdapter(this::onExerciseClick);
  private MainViewModel mMainViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
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

  @Override
  public void onResume() {
    super.onResume();
    mMainViewModel.updateSubtitle("Choose an exercise");
  }

  private void onExerciseClick(Exercise exercise) {
    Timber.i("Click %s", exercise.id().name());

    ExerciseListFragmentDirections.ActionChooseExercise action =
        ExerciseListFragmentDirections.actionChooseExercise()
        .setViewMode(ViewMode.TRAINING)
        .setExerciseIdOrdinal(exercise.id().ordinal())
        .setLandscapeMode(false);

    NavHostFragment.findNavController(this).navigate(action);
  }

  private void render(ExerciseListViewModel.ViewState viewState) {
    mAdapter.updateData(viewState.exercises());
  }
}
