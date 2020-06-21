package com.bloomcyclecare.cmcc.ui.instructions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.entities.Instructions;

import org.parceler.Parcels;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class InstructionsPageFragment extends Fragment {

  InstructionsPageViewModel mViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Instructions instructions = Parcels.unwrap(requireArguments().getParcelable(Instructions.class.getCanonicalName()));
    InstructionsPageViewModel.Factory factory = new InstructionsPageViewModel.Factory(requireActivity().getApplication(), instructions);
    mViewModel = new ViewModelProvider(this, factory).get(InstructionsPageViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_instruction_page, container, false);

    TextView startDate = view.findViewById(R.id.tv_start_date);
    startDate.setText("TBD");
    TextView status = view.findViewById(R.id.tv_status);
    status.setText("TBD");

    Fragment fragment = new InstructionSelectionFragment();
    fragment.setArguments(requireArguments());

    getChildFragmentManager().beginTransaction().replace(R.id.instruction_selection_container, fragment).commit();
    getChildFragmentManager().executePendingTransactions();

    mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      startDate.setText(viewState.startDateStr);
      status.setText(viewState.statusStr);
    });

    return view;
  }

  @Override
  public void onDestroy() {
    mViewModel.onCleared();
    super.onDestroy();
  }
}
