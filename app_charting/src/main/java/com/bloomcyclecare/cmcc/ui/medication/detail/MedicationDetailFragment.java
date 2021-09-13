package com.bloomcyclecare.cmcc.ui.medication.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;

public class MedicationDetailFragment extends Fragment {

  private MainViewModel mMainViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    MedicationDetailFragmentArgs args = MedicationDetailFragmentArgs.fromBundle(requireArguments());
    if (args.getMedication() == null) {
      mMainViewModel.updateTitle("New Medication");
      mMainViewModel.updateSubtitle("");
    } else {
      mMainViewModel.updateTitle("Edit Medication");
      mMainViewModel.updateSubtitle("");
    }

    View view = inflater.inflate(R.layout.fragment_medication_detail, container, false);
    return view;
  }
}
