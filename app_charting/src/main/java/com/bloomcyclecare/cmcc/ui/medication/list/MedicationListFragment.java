package com.bloomcyclecare.cmcc.ui.medication.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

public class MedicationListFragment extends Fragment {

  private MainViewModel mMainViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
    mMainViewModel.updateTitle("Your Medications");
    mMainViewModel.updateSubtitle("");

    View view = inflater.inflate(R.layout.fragment_medication_list, container, false);

    FloatingActionButton fab = view.findViewById(R.id.medications_fab);
    fab.setOnClickListener(v -> {
      Navigation.findNavController(requireView())
          .navigate(MedicationListFragmentDirections.actionAddMedication());
    });
    return view;
  }
}
