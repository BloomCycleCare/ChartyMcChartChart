package com.bloomcyclecare.cmcc.ui.entry.list.vertical;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.entry.list.BaseCycleListFragment;
import com.bloomcyclecare.cmcc.ui.entry.list.EntryListViewModel;
import com.bloomcyclecare.cmcc.utils.DateUtil;

import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;

public class EntryListPageFragment extends BaseCycleListFragment {

  private EntryListViewModel mViewModel;
  private EntryListPageAdapter mPageAdapter;

  private ViewPager mViewPager;

  @NonNull
  @Override
  protected ViewMode initialViewModeFromArgs(Bundle bundle) {
    return EntryListPageFragmentArgs.fromBundle(bundle).getViewMode();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mPageAdapter = new EntryListPageAdapter(getFragmentManager());

    EntryListPageFragmentArgs args = EntryListPageFragmentArgs.fromBundle(requireArguments());
    mViewModel = new ViewModelProvider(getActivity(), new EntryListViewModel.Factory(getActivity().getApplication(), args.getViewMode()))
        .get(EntryListViewModel.class);

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
      mPageAdapter.update(viewState.renderableCycles.stream()
          .map(CycleRenderer.RenderableCycle::cycle)
          .collect(Collectors.toList()), viewState.viewMode);
      mPageAdapter.onPageActive(viewState.currentCycleIndex);
      if (mViewPager.getCurrentItem() != viewState.currentCycleIndex) {
        mViewPager.setCurrentItem(viewState.currentCycleIndex);
      }
    });

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
  }
}
