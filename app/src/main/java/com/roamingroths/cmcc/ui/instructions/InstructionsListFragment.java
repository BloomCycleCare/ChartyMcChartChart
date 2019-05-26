package com.roamingroths.cmcc.ui.instructions;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.roamingroths.cmcc.R;

/**
 * Created by parkeroth on 11/13/17.
 */

public class InstructionsListFragment extends Fragment {

  private static boolean DEBUG = true;
  private static String TAG = InstructionsListFragment.class.getSimpleName();

  InstructionsCrudViewModel mViewModel;

  public InstructionsListFragment() {
    if (DEBUG) Log.v(TAG, "Construct");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = ViewModelProviders.of(this).get(InstructionsCrudViewModel.class);
    mViewModel.initialize(getArguments());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_instruction_edit, container, false);

    TextView startDate = view.findViewById(R.id.tv_start_date);
    TextView endDate = view.findViewById(R.id.tv_start_date);
    mViewModel.viewState().observe(this, viewState -> {
      if (!startDate.getText().equals(viewState.startDateStr)) {
        startDate.setText(viewState.startDateStr);
      }
      if (!endDate.getText().equals(viewState.endDateStr)) {
        endDate.setText(viewState.endDateStr);
      }
    });

    return view;
  }
}
