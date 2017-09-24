package com.roamingroths.cmcc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;

public class EntryDetailActivity extends AppCompatActivity {

  private static final String UI_DATE_FORMAT = "EEE, d MMM yyyy";

  public static final int CREATE_REQUEST = 1;
  public static final int MODIFY_REQUEST = 2;

  public static final int CANCEL_RESPONSE = 1;

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_detail);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(getTitle(getIntent()));
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getIntent());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
    tabLayout.setupWithViewPager(mViewPager);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_entry_detail, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
      startActivity(startSettingsActivity);
      return true;
    }

    if (id == R.id.action_save) {
      //chartEntryFragment.onSave();
      return true;
    }
    if (id == R.id.action_delete) {
      new AlertDialog.Builder(this)
          //set message, title, and icon
          .setTitle("Delete")
          .setMessage("Do you want to permanently delete this entry?")
          .setIcon(R.drawable.ic_delete_forever_black_24dp)
          .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {
              dialog.dismiss();
              //chartEntryFragment.onDelete();

            }
          })
          .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
          })
          .create().show();
    }

    if (id == android.R.id.home) {
      if (mSectionsPagerAdapter.anyDirty()) {
        new AlertDialog.Builder(this)
            //set message, title, and icon
            .setTitle("Discard Changes")
            .setMessage("Some of your changes have not been saved. Do you wish to discard them?")
            .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                //your deleting code
                setResult(CANCEL_RESPONSE, null);
                onBackPressed();
                dialog.dismiss();
                finish();
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
              }
            })
            .create().show();
      } else {
        setResult(CANCEL_RESPONSE, null);
        onBackPressed();
      }
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final SparseArray<EntryFragment> registeredFragments = new SparseArray<>();
    private final Intent intent;

    public SectionsPagerAdapter(FragmentManager fm, Intent intent) {
      super(fm);
      this.intent = intent;
    }

    public boolean anyDirty() {
      for (int i = 0; i < getCount(); i++) {
        if (getItem(i).isDirty()) {
          Log.v("FOO", "Index: " + i);
          return true;
        }
      }
      return false;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      Log.v("EntryDetailActivity", "instantiateItem: " + position);
      EntryFragment fragment = (EntryFragment) super.instantiateItem(container, position);
      registeredFragments.put(position, fragment);
      return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      Log.v("EntryDetailActivity", "destroyItem: " + position);
      registeredFragments.remove(position);
      super.destroyItem(container, position, object);
    }

    @Override
    public EntryFragment getItem(int position) {
      Log.v("EntryDetailActivity", "getItem: " + position);
      EntryFragment cachedFragment = registeredFragments.get(position);
      if (cachedFragment != null) {
        return cachedFragment;
      }

      Bundle args = new Bundle();
      args.putString(Extras.ENTRY_DATE_STR, intent.getStringExtra(Extras.ENTRY_DATE_STR));
      args.putParcelable(Cycle.class.getName(), getCycle(intent));

      EntryFragment fragment;
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      switch (position) {
        case 0:
          args.putBoolean(
              Extras.EXPECT_UNUSUAL_BLEEDING,
              intent.getBooleanExtra(Extras.EXPECT_UNUSUAL_BLEEDING, false));

          fragment = new ChartEntryFragment();
          fragment.setArguments(args);
          Log.v("EntryDetailActivity", "Creating new instance of ChartEntryFragment");
          return fragment;
        case 1:
          fragment = new WellnessEntryFragment();
          fragment.setArguments(args);
          Log.v("EntryDetailActivity", "Creating new instance of WellnessEntryFragment");
          return fragment;
        case 2:
          fragment = new SymptomEntryFragment();
          fragment.setArguments(args);
          Log.v("EntryDetailActivity", "Creating new instance of WellnessEntryFragment");
          return fragment;
      }
      return null;
    }

    @Override
    public int getCount() {
      // Show 3 total pages.
      return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0:
          return "Chart Entry";
        case 1:
          return "Wellness";
        case 2:
          return "Symptoms";
      }
      return null;
    }
  }

  private static Cycle getCycle(Intent intent) {
    if (!intent.hasExtra(Cycle.class.getName())) {
      throw new IllegalStateException();
    }
    return intent.getParcelableExtra(Cycle.class.getName());
  }

  private static String getEntryDateStr(Intent intent) {
    if (!intent.hasExtra(Extras.ENTRY_DATE_STR)) {
      throw new IllegalStateException("Missing entry date");
    }
    String entryDateStr = intent.getStringExtra(Extras.ENTRY_DATE_STR);
    Preconditions.checkState(!Strings.isNullOrEmpty(entryDateStr));
    return entryDateStr;
  }

  private static String getTitle(Intent intent) {
    LocalDate entryDate = Preconditions.checkNotNull(DateUtil.fromWireStr(getEntryDateStr(intent)));
    Cycle cycle = getCycle(intent);
    int daysBetween = Days.daysBetween(cycle.startDate, entryDate).getDays();
    return "Day #" + (daysBetween + 1);
  }
}
