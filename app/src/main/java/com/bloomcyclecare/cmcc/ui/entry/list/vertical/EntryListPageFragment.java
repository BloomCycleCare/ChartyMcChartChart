package com.bloomcyclecare.cmcc.ui.entry.list.vertical;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.entry.list.EntryListViewModel;

import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

public class EntryListPageFragment extends Fragment {

  public enum Args {
    VIEW_MODE;
  }

  private EntryListViewModel mViewModel;
  private EntryListPageAdapter mPageAdapter;

  private TextView mErrorView;
  private ProgressBar mProgressBar;
  private ViewPager mViewPager;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mPageAdapter = new EntryListPageAdapter(getFragmentManager());

    mViewModel = ViewModelProviders.of(getActivity()).get(EntryListViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_entry_list_page, container, false);

    mErrorView = view.findViewById(R.id.refresh_error);
    mProgressBar = view.findViewById(R.id.progress_bar);

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

    mViewModel.viewStates().observe(this, viewState -> {
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
}
