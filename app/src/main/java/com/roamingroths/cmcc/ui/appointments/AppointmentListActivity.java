package com.roamingroths.cmcc.ui.appointments;

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

import com.roamingroths.cmcc.R;

public class AppointmentListActivity extends AppCompatActivity {

  private SectionsPagerAdapter mPagerAdapter;
  private ViewPager mViewPager;
  private FloatingActionButton mFab;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_appointment_list);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Appointments");

    mFab = findViewById(R.id.floatingActionButton);

    mPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mPagerAdapter);

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
      return new AppointmentListFragment();
    }

    @Override
    public int getCount() {
      return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0:
          return "Consultations";
        case 1:
          return "Other";
        default:
          throw new IllegalArgumentException();
      }
    }
  }
}
