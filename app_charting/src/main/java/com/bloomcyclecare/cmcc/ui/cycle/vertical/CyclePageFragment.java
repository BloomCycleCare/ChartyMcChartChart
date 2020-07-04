package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.cycle.BaseCycleListFragment;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.bloomcyclecare.cmcc.utils.DateUtil;

import java.util.Optional;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;

public class CyclePageFragment extends BaseCycleListFragment {

  private MainViewModel mMainViewModel;
  private CyclePageViewModel mViewModel;
  private CyclePageAdapter mPageAdapter;

  private ViewPager mViewPager;

  @NonNull
  @Override
  protected ViewMode initialViewModeFromArgs(Bundle bundle) {
    return CyclePageFragmentArgs.fromBundle(bundle).getViewMode();
  }

  @NonNull
  @Override
  protected Optional<Exercise.ID> exerciseIdFromArgs(Bundle bundle) {
    return Optional.empty();
  }

  @NonNull
  @Override
  protected NavDirections toggleLayoutAction(ViewMode viewMode) {
    return CyclePageFragmentDirections.actionToggleLayout().setViewMode(viewMode);
  }

  @NonNull
  @Override
  protected NavDirections printAction(ViewMode viewMode) {
    return CyclePageFragmentDirections.actionPrint(viewMode);
  }

  @NonNull
  @Override
  protected NavDirections reinitAction() {
    return CyclePageFragmentDirections.actionReinitApp();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

    // Use getChildFragmentManager(): https://stackoverflow.com/a/25525714
    mPageAdapter = new CyclePageAdapter(getChildFragmentManager());

    CyclePageFragmentArgs args = CyclePageFragmentArgs.fromBundle(requireArguments());
    mViewModel = CyclePageViewModel.create(this, requireActivity(), cycleListViewModel());

    if (args.getDateToFocus() != null) {
      Timber.v("Focusing date: %s", args.getDateToFocus());
      mViewModel.focusDate(DateUtil.fromWireStr(args.getDateToFocus()));
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_entry_list_page, container, false);

    mViewPager = view.findViewById(R.id.view_pager);
    mViewPager.setAdapter(mPageAdapter);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

      @Override
      public void onPageSelected(int position) {
        mViewModel.currentPageUpdates.onNext(position);
        mPageAdapter.onPageActive(position);
      }

      @Override
      public void onPageScrollStateChanged(int state) {}
    });
    mViewPager.setOffscreenPageLimit(4);

    mViewModel.viewStates().observe(getViewLifecycleOwner(), viewState -> {
      mMainViewModel.updateTitle(viewState.title);
      mMainViewModel.updateSubtitle(viewState.subtitle);

      mPageAdapter.update(viewState.renderableCycles.stream()
          .map(CycleRenderer.RenderableCycle::cycle)
          .collect(Collectors.toList()), viewState.viewMode);
      mPageAdapter.onPageActive(viewState.currentCycleIndex);
      if (mViewPager.getCurrentItem() != viewState.currentCycleIndex) {
        mViewPager.setCurrentItem(viewState.currentCycleIndex);
      }

      requireActivity().invalidateOptionsMenu();
    });

    return view;
  }
}
