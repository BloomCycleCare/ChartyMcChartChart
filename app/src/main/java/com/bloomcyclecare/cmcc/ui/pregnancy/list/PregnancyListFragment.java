package com.bloomcyclecare.cmcc.ui.pregnancy.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.utils.RxPrompt;
import com.bloomcyclecare.cmcc.utils.SimpleArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
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
    NavHostFragment.findNavController(this).navigate(
        PregnancyListFragmentDirections.actionEditPregnancy()
            .setPregnancy(model.pregnancy.wrap()));
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
