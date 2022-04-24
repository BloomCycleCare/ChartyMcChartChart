package com.bloomcyclecare.cmcc.ui.entry.wellbeing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailViewModel;
import com.bloomcyclecare.cmcc.ui.entry.wellbeing.sections.EnergySection;
import com.bloomcyclecare.cmcc.ui.entry.wellbeing.sections.MedicationSection;
import com.bloomcyclecare.cmcc.ui.entry.wellbeing.sections.PainSection;

public class WellbeingEntryFragment extends Fragment {

  private WellbeingEntryViewModel mViewModel;
  private EntryDetailViewModel mEntryViewModel;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_lifestyle, container, false);

    mEntryViewModel = new ViewModelProvider(requireActivity()).get(EntryDetailViewModel.class);

    WellbeingEntryViewModel.Factory factory = new WellbeingEntryViewModel.Factory(
        requireActivity().getApplication(), mEntryViewModel.wellbeingEntrySubject.blockingFirst());
    mViewModel = new ViewModelProvider(requireActivity(), factory).get(WellbeingEntryViewModel.class);

    mViewModel.updatedEntry().subscribe(mEntryViewModel.wellbeingEntrySubject);

    LinearLayoutCompat lifestyleItems = view.findViewById(R.id.lifestyle_items);
    lifestyleItems.addView(PainSection.inflate(inflater, requireContext(), mViewModel));
    lifestyleItems.addView(EnergySection.inflate(inflater, requireContext(), mViewModel));
    if (mViewModel.shouldShowMedicationSection()) {
      lifestyleItems.addView(MedicationSection.inflate(inflater, requireContext(), mViewModel));
    }

    return view;
  }
}
