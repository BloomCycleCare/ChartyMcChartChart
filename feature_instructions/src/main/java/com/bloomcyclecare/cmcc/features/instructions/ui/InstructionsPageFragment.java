package com.bloomcyclecare.cmcc.features.instructions.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.features.instructions.R;

import org.parceler.Parcels;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;

public class InstructionsPageFragment extends Fragment {

  CompositeDisposable mDisposables = new CompositeDisposable();
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
    TextView summary = view.findViewById(R.id.tv_summary);
    summary.setText("TBD");
    configureOnSummaryClick(summary);

    Fragment fragment = new InstructionSelectionFragment();
    fragment.setArguments(requireArguments());

    getChildFragmentManager().beginTransaction().replace(R.id.instruction_selection_container, fragment).commit();
    getChildFragmentManager().executePendingTransactions();

    mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      startDate.setText(viewState.startDateStr);
      status.setText(viewState.statusStr);
      summary.setText(viewState.summaryStr);
    });

    return view;
  }

  private void configureOnSummaryClick(TextView textView) {
    Context context = requireContext();
    textView.setOnClickListener(v -> {
      mDisposables.add(mViewModel.currentSummary().subscribe(summaryStr -> {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
          android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
              .getSystemService(context.CLIPBOARD_SERVICE);
          clipboard.setText(summaryStr);
        } else {
          android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
              .getSystemService(context.CLIPBOARD_SERVICE);
          android.content.ClipData clip = android.content.ClipData.newPlainText("label", summaryStr);
          clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(context, "Coppied to clipboard", Toast.LENGTH_SHORT).show();
      }));
    });
  }

  @Override
  public void onDestroy() {
    mViewModel.onCleared();
    super.onDestroy();
  }
}
