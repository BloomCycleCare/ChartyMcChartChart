package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.CompositeDisposable;

public class TosStepFragment extends Fragment implements Step {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private StepperViewModel mStepperViewModel;
  private TosItemAdapter mAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_step_tos, container, false);
    mStepperViewModel = new ViewModelProvider(requireActivity()).get(StepperViewModel.class);
    mAdapter = new TosItemAdapter(requireContext());

    RecyclerView recyclerView = view.findViewById(R.id.rv_tos_items);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    recyclerView.setAdapter(mAdapter);

    mStepperViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      mAdapter.update(viewState.tosItems());
    });

    return view;
  }

  @Nullable
  @Override
  public VerificationError verifyStep() {
    return null;
  }

  @Override
  public void onSelected() {
    mDisposables.add(mStepperViewModel
        .currentViewState()
        .flatMap(viewState -> {
          if (!viewState.tosItems().isEmpty()) {
            return Single.just(true);
          }
          return promptTermsOfUse();
        })
        .subscribe(allAgreed -> {
          if (allAgreed) {
            mStepperViewModel.recordTosComplete();
          }
        }));
  }

  @Override
  public void onError(@NonNull VerificationError error) {}

  private Single<Boolean> promptTermsOfUse() {
    return Single.create(emitter -> promptForTermOfUse(
        Queues.newArrayDeque(Arrays.asList(TosItem.values())), emitter));
  }

  private void promptForTermOfUse(Deque<TosItem> remainingTerms, SingleEmitter<Boolean> emitter) {
    if (remainingTerms.isEmpty()) {
      emitter.onSuccess(true);
      return;
    }
    TosItem tosItem = remainingTerms.poll();
    Dialog dialog = new AlertDialog.Builder(requireContext())
        .setTitle(tosItem.summary)
        .setMessage(tosItem.content)
        .setPositiveButton("Agree", (d, w) -> {
          promptForTermOfUse(remainingTerms, emitter);
          d.dismiss();
        })
        .setNegativeButton("Cancel", (d, w) -> {
          emitter.onSuccess(false);
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

    void bind(TosItem tosItem, Boolean agreed) {
      mTosSummary.setText(tosItem.summary);
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
      holder.bind(tosItem, Optional.ofNullable(mValues.get(tosItem)).orElse(false));
    }

    @Override
    public int getItemCount() {
      return mValues.size();
    }
  }
}
