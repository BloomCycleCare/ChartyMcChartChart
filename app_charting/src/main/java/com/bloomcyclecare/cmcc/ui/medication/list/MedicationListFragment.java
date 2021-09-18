package com.bloomcyclecare.cmcc.ui.medication.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.WrappedMedication;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import io.reactivex.disposables.CompositeDisposable;

public class MedicationListFragment extends Fragment {

  private MainViewModel mMainViewModel;
  private MedicationListViewModel mMedicationListViewModel;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  @Override
  public void onDestroy() {
    mDisposables.clear();
    super.onDestroy();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    mMedicationListViewModel = new ViewModelProvider(this).get(MedicationListViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_medication_list, container, false);

    RecyclerView medicationRecyclerView = view.findViewById(R.id.recyclerview_medications);
    medicationRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
    MedicationListAdapter adapter = new MedicationListAdapter(requireContext());
    medicationRecyclerView.setAdapter(adapter);

    TextView noMedicationsView = view.findViewById(R.id.tv_no_mediations);

    mMedicationListViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      mMainViewModel.updateTitle(viewState.title);
      mMainViewModel.updateSubtitle(viewState.subtitle);
      adapter.updateMedications(viewState.medications);

      if (viewState.medications.isEmpty()) {
        noMedicationsView.setVisibility(View.VISIBLE);
        medicationRecyclerView.setVisibility(View.GONE);
      } else {
        noMedicationsView.setVisibility(View.GONE);
        medicationRecyclerView.setVisibility(View.VISIBLE);
      }
    });

    mDisposables.add(adapter.editClicks().subscribe(this::navigateToDetailView));

    FloatingActionButton fab = view.findViewById(R.id.medications_fab);
    fab.setOnClickListener(v -> navigateToDetailView(null));
    return view;
  }

  private void navigateToDetailView(@Nullable Medication medication) {
    MedicationListFragmentDirections.ActionEditMedication action =
        MedicationListFragmentDirections.actionEditMedication();
    if (medication != null) {
      action.setMedication(new WrappedMedication(medication));
    }
    Navigation.findNavController(requireView())
        .navigate(action);
  }
}
