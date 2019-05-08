package com.roamingroths.cmcc.ui.profile;

import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.ui.goals.list.GoalListFragment;

public class ProfileActivity extends AppCompatActivity {

  private SectionsPagerAdapter mPagerAdapter;
  private ViewPager mViewPager;
  private FloatingActionButton mFabNewGoal;

  private void showOrHideFab(int position) {
    if (position == 1) {
      mFabNewGoal.setVisibility(View.VISIBLE);
    } else {
      mFabNewGoal.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profile);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Profile");

    mFabNewGoal = findViewById(R.id.fab_new_goal);

    mPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mPagerAdapter);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        showOrHideFab(position);
      }

      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

      @Override
      public void onPageScrollStateChanged(int state) {}
    });
    showOrHideFab(mViewPager.getCurrentItem());

    TabLayout tabLayout = findViewById(R.id.tabs);
    tabLayout.setupWithViewPager(mViewPager);
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

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      switch (position) {
        case 0:
          return new ProfileFragment();
        case 1:
          return new GoalListFragment();
        default:
          throw new IllegalArgumentException();
      }
    }

    @Override
    public int getCount() {
      return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0:
          return "Info";
        case 1:
          return "Goals";
        default:
          throw new IllegalArgumentException();
      }
    }
  }
}
