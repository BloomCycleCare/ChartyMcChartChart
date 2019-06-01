package com.roamingroths.cmcc.ui.instructions;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.entities.Instructions;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstructionsListActivity extends AppCompatActivity {

  private SectionsPagerAdapter mPagerAdapter;
  private ViewPager mViewPager;
  private InstructionsListViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_appointment_list);

    mViewModel = ViewModelProviders.of(this).get(InstructionsListViewModel.class);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Instructions");

    mPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mPagerAdapter);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

      @Override
      public void onPageSelected(int position) {
        if (position == 0) {
          toolbar.setTitle("New Instructions");
        } else if (position == 1) {
          toolbar.setTitle("Current Instructions");
        } else {
          toolbar.setTitle("Previous Instructions");
        }
      }

      @Override
      public void onPageScrollStateChanged(int state) {}
    });

    final AtomicBoolean initialLoad = new AtomicBoolean(false);
    mViewModel.instructionsStream().observe(this, instructions -> {
      mPagerAdapter.updateInstructions(instructions);
      if (initialLoad.compareAndSet(false, true)) {
        mViewPager.setCurrentItem(1);
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      onBackPressed();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final List<Instructions> mInstructions = new ArrayList<>();

    SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    void updateInstructions(List<Instructions> instructions) {
      mInstructions.clear();
      mInstructions.addAll(instructions);
      notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
      Fragment f = new InstructionsListFragment();
      Bundle args = new Bundle();
      args.putParcelable(Instructions.class.getCanonicalName(), Parcels.wrap(mInstructions.get(position)));
      f.setArguments(args);
      return f;
    }

    @Override
    public int getCount() {
      return mInstructions.size();
    }
  }
}
