package com.bloomcyclecare.cmcc.ui.instructions;

import android.os.Bundle;
import android.util.Log;
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
import timber.log.Timber;

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

    Instructions instructions = Parcels.unwrap(requireArguments().getParcelable(Instructions.class.getCanonicalName()));
    InstructionsCrudViewModel.Factory factory = new InstructionsCrudViewModel.Factory(requireActivity().getApplication(), instructions);
    mViewModel = new ViewModelProvider(this, factory).get(InstructionsCrudViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_instruction_edit, container, false);

    TextView startDate = view.findViewById(R.id.tv_start_date);
    startDate.setText("Unspecified");
    TextView status = view.findViewById(R.id.tv_status);
    status.setText("Unspecified");

    Fragment fragment = new InstructionSelectionFragment();
    fragment.setArguments(requireArguments());

    getChildFragmentManager().beginTransaction().replace(R.id.instruction_selection_container, fragment).commit();
    getChildFragmentManager().executePendingTransactions();

    mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      Timber.i("Updating ViewState for instructions starting %s", viewState.instructions.startDate);

      startDate.setText(viewState.startDateStr);
      status.setText(viewState.statusStr);

      /*LocalDate currentStartDate = viewState.instructions.startDate;
      startDate.setOnClickListener(view1 -> new DatePickerDialog(getContext(), (datePicker, year, month, day) -> {
        mViewModel.updateStartDate(new LocalDate(year, month+1, day), (instructionsToRemove) -> Single.create(emitter -> {
          new AlertDialog.Builder(getActivity())
              .setTitle("Remove Instructions?")
              .setMessage(String.format("This change would overlap %d existing instructions. Should they be dropped?", instructionsToRemove.size()))
              .setPositiveButton("Yes", (d, i) -> {
                emitter.onSuccess(true);
                d.dismiss();
              })
              .setNegativeButton("No", (d, i) -> {
                emitter.onSuccess(false);
                d.dismiss();
              })
              .show();
        })).subscribe();
      }, currentStartDate.getYear(), currentStartDate.getMonthOfYear()-1, currentStartDate.getDayOfMonth()).show());*/

    });

    return view;
  }

  @Override
  public void onDestroy() {
    mViewModel.onCleared();
    super.onDestroy();
  }
}
