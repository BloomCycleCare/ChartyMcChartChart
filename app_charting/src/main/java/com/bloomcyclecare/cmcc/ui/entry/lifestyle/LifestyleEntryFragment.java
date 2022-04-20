package com.bloomcyclecare.cmcc.ui.entry.lifestyle;

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
import com.bloomcyclecare.cmcc.data.models.lifestyle.LifestyleEntry;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailViewModel;

import org.parceler.Parcels;

import timber.log.Timber;

public class LifestyleEntryFragment extends Fragment {

  private LifestyleEntryViewModel mViewModel;
  private EntryDetailViewModel mEntryViewModel;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_lifestyle, container, false);

    mEntryViewModel = new ViewModelProvider(requireActivity()).get(EntryDetailViewModel.class);

    LifestyleEntryViewModel.Factory factory = new LifestyleEntryViewModel.Factory(
        requireActivity().getApplication(), mEntryViewModel.lifestyleEntrySubject.blockingFirst());
    mViewModel = new ViewModelProvider(requireActivity(), factory).get(LifestyleEntryViewModel.class);

    mViewModel.updatedEntry().subscribe(mEntryViewModel.lifestyleEntrySubject);

    LinearLayoutCompat lifestyleItems = view.findViewById(R.id.lifestyle_items);
    lifestyleItems.addView(LifestyleSectionPain.inflate(inflater, requireContext(), mViewModel));

    return view;
  }
}
