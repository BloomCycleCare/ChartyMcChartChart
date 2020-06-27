package com.bloomcyclecare.cmcc.practitioner.ui.applink;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.features.instructions.ui.InstructionSelectionFragment;
import com.bloomcyclecare.cmcc.practitioner.R;

import androidx.fragment.app.Fragment;

public class AppLinkFragment extends Fragment {
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_main, container, false);

    Fragment fragment = new InstructionSelectionFragment();
    fragment.setArguments(requireArguments());
    getChildFragmentManager().beginTransaction().replace(R.id.instruction_selection_container, fragment).commit();
    getChildFragmentManager().executePendingTransactions();

    return view;
  }
}
