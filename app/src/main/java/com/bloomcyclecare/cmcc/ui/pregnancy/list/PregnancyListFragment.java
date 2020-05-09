package com.bloomcyclecare.cmcc.ui.pregnancy.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.pregnancy.detail.PregnancyDetailFragment;
import com.bloomcyclecare.cmcc.utils.RxPrompt;
import com.bloomcyclecare.cmcc.utils.SimpleArrayAdapter;

import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;

public class PregnancyListFragment extends Fragment {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private SimpleArrayAdapter<PregnancyListViewModel.PregnancyViewModel, ViewHolder> mAdapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_pregnancy_list, container, false);

    ListView pregnanciesView = view.findViewById(R.id.lv_pregnancies);
    mAdapter = new SimpleArrayAdapter<>(requireActivity(), R.layout.list_item_pregnancy, v -> new ViewHolder(v, this::viewDetails), c -> {});
    pregnanciesView.setAdapter(mAdapter);

    new ViewModelProvider(this)
        .get(PregnancyListViewModel.class)
        .viewState()
        .observe(getViewLifecycleOwner(), this::render);
    return view;
  }

  private void render(PregnancyListViewModel.ViewState viewState) {
    for (RxPrompt prompt : viewState.prompts) {
      mDisposables.add(prompt.doPrompt(requireActivity()));
    }
    mAdapter.updateData(viewState.viewModels);
  }

  private void viewDetails(PregnancyListViewModel.PregnancyViewModel model) {
    Intent intent = new Intent(requireContext(), PregnancyDetailFragment.class);
    intent.putExtra(PregnancyDetailFragment.Extras.PREGNANCY.name(), Parcels.wrap(model.pregnancy));
    intent.putExtra(PregnancyDetailFragment.Extras.CYCLE_INDEX.name(), model.cycleAscIndex);
    startActivity(intent);
  }

  private static class ViewHolder extends SimpleArrayAdapter.SimpleViewHolder<PregnancyListViewModel.PregnancyViewModel> {

    final TextView infoTextView;

    ViewHolder(View view, Consumer<PregnancyListViewModel.PregnancyViewModel> onClick) {
      super(view);
      infoTextView = view.findViewById(R.id.tv_pregnancy_info);
      infoTextView.setOnClickListener(v -> {
        onClick.accept(getCurrent());
      });
    }

    @Override
    protected void updateUI(PregnancyListViewModel.PregnancyViewModel data) {
      infoTextView.setText(data.getInfo());
    }
  }
}
