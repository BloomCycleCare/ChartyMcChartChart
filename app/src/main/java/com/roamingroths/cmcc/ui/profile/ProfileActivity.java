package com.roamingroths.cmcc.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.roamingroths.cmcc.R;

public class ProfileActivity extends AppCompatActivity {

  private View mBackgroundDimmer;
  private SectionsPagerAdapter mPagerAdapter;
  private ViewPager mViewPager;
  private FloatingActionsMenu mFam;
  private FloatingActionButton mFabGeneral;
  private FloatingActionButton mFabDiet;
  private FloatingActionButton mFabMedication;

  private void showOrHideFab(int position) {
    if (position == 1) {
      mFam.setVisibility(View.VISIBLE);
    } else {
      mFam.setVisibility(View.GONE);
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

    mBackgroundDimmer = findViewById(R.id.background_dimmer);
    mFam = findViewById(R.id.floatingActionButton);
    mFam.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
      @Override
      public void onMenuExpanded() {
        mBackgroundDimmer.setVisibility(View.VISIBLE);
      }

      @Override
      public void onMenuCollapsed() {
        mBackgroundDimmer.setVisibility(View.GONE);
      }
    });

    mFabGeneral = findViewById(R.id.add_general);
    mFabGeneral.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(ProfileActivity.this, GoalDetailActivity.class));
        mFam.collapse();
      }
    });
    mFabDiet = findViewById(R.id.add_diet);
    mFabDiet.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mFam.collapse();
      }
    });
    mFabMedication = findViewById(R.id.add_medication);
    mFabMedication.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mFam.collapse();
      }
    });

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
