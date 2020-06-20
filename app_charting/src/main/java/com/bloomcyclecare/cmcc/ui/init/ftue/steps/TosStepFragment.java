package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;

public class TosStepFragment extends Fragment implements Step {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private TosStepViewModel mViewModel;
  private TosItemAdapter mAdapter;
  private Optional<VerificationError> mVerificationError = Optional.of(new VerificationError("Initializing"));

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_step_tos, container, false);

    StepperViewModel stepperViewModel = new ViewModelProvider(requireActivity()).get(StepperViewModel.class);
    TosStepViewModel.Factory factory = new TosStepViewModel.Factory(requireActivity().getApplication(), stepperViewModel);
    mViewModel = new ViewModelProvider(this, factory).get(TosStepViewModel.class);

    mAdapter = new TosItemAdapter(requireContext());

    RecyclerView recyclerView = view.findViewById(R.id.rv_tos_items);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setAdapter(mAdapter);

    Button exitButton = view.findViewById(R.id.button_exit);
    exitButton.setOnClickListener(this::promptExitApp);
    exitButton.setVisibility(View.GONE);

    Button revisitButton = view.findViewById(R.id.button_revisit_tos);
    revisitButton.setOnClickListener(v -> onSelected());
    revisitButton.setVisibility(View.GONE);

    mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      mAdapter.update(viewState.tosItems());
      mVerificationError = viewState.verificationError();

      if (viewState.showButtons()) {
        exitButton.setVisibility(View.VISIBLE);
        revisitButton.setVisibility(View.VISIBLE);
      } else {
        exitButton.setVisibility(View.GONE);
        revisitButton.setVisibility(View.GONE);
      }
    });

    return view;
  }

  @Nullable
  @Override
  public VerificationError verifyStep() {
    return mVerificationError.orElse(null);
  }

  @Override
  public void onSelected() {
    if (mVerificationError.isPresent()) {
      promptTermsOfUse();
    }
  }

  @Override
  public void onError(@NonNull VerificationError error) {
    Toast.makeText(requireContext(), error.getErrorMessage(), Toast.LENGTH_SHORT).show();
  }

  private void promptTermsOfUse() {
    promptForTermOfUse(Queues.newArrayDeque(Arrays.asList(TosItem.values())));
  }

  private void promptExitApp(View v) {
    Dialog dialog = new AlertDialog.Builder(requireContext())
        .setTitle("Confirm Exit")
        .setMessage("We're sorry to see you go but all terms must be agreed before using Charty McChrtChart.")
        .setPositiveButton("Confirm", (d, w) -> {
          requireActivity().finish();
          d.dismiss();
        })
        .setNegativeButton("Cancel", (d, w) -> {
          d.dismiss();
        })
        .create();
    dialog.show();
  }

  private void promptForTermOfUse(Deque<TosItem> remainingTerms) {
    TosItem tosItem = remainingTerms.poll();
    if (tosItem == null) {
      return;
    }
    Dialog dialog = new AlertDialog.Builder(requireContext())
        .setTitle(tosItem.summary)
        .setMessage(tosItem.content)
        .setPositiveButton("Agree", (d, w) -> {
          mViewModel.recordTosAgreement(tosItem, true);
          promptForTermOfUse(remainingTerms);
          d.dismiss();
        })
        .setNegativeButton("Decline", (d, w) -> {
          mViewModel.recordTosAgreement(tosItem, false);
          promptForTermOfUse(remainingTerms);
          d.dismiss();
        })
        .create();
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);
    dialog.show();
  }

  private static class TosItemViewHolder extends RecyclerView.ViewHolder {

    ImageView mTosImage;
    TextView mTosSummary;

    TosItemViewHolder(@NonNull View itemView) {
      super(itemView);
      mTosImage = itemView.findViewById(R.id.iv_tos_status);
      mTosSummary = itemView.findViewById(R.id.tv_tos_summary);
    }

    void bind(TosItem tosItem, Boolean agreed, Context context) {
      mTosSummary.setText(tosItem.summary);
      mTosImage.setImageDrawable(context.getDrawable(
          agreed ? R.drawable.ic_check_circle_black_24dp : R.drawable.ic_info_red_24dp));
    }
  }

  private static class TosItemAdapter extends RecyclerView.Adapter<TosItemViewHolder> {

    private final Context mContext;
    private Map<TosItem, Boolean> mValues = new HashMap<>();

    TosItemAdapter(Context context) {
      mContext = context;
    }

    void update(Map<TosItem, Boolean> values) {
      mValues = ImmutableMap.copyOf(values);
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TosItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      int layoutIdForListItem = R.layout.list_item_tos;
      LayoutInflater inflater = LayoutInflater.from(mContext);
      View view = inflater.inflate(layoutIdForListItem, parent, false);
      return new TosItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TosItemViewHolder holder, int position) {
      TosItem tosItem = TosItem.values()[position];
      holder.bind(tosItem, Optional.ofNullable(mValues.get(tosItem)).orElse(false), mContext);
    }

    @Override
    public int getItemCount() {
      return mValues.size();
    }
  }
}
