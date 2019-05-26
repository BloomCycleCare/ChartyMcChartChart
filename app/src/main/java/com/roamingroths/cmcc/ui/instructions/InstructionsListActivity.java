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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.entities.Instructions;

import java.util.ArrayList;
import java.util.List;

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

    mViewModel.instructions().observe(
        this, instructions -> mPagerAdapter.updateInstructions(instructions));
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
    }

    @Override
    public Fragment getItem(int position) {
      return new InstructionsListFragment();
    }

    @Override
    public int getCount() {
      return mInstructions.size();
    }
  }
}
