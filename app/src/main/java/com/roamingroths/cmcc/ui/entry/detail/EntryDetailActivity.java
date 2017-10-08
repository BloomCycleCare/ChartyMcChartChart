package com.roamingroths.cmcc.ui.entry.detail;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.Extras;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.crypto.CyrptoExceptions;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.data.EntryProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.roamingroths.cmcc.ui.entry.detail.ChartEntryFragment.OK_RESPONSE;

public class EntryDetailActivity extends AppCompatActivity implements EntryFragment.EntryListener {

  private static final boolean DEBUG = true;
  private static final String TAG = EntryDetailActivity.class.getSimpleName();

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
  private Cycle mCycle;
  private CycleProvider mCycleProvider;
  private String mUserId;

  private Map<Class<? extends Entry>, Entry> mExistingEntries = new HashMap<>();
  private Map<Class<? extends Entry>, Entry> mEntries = new HashMap<>();

  @Override
  public void onEntryUpdated(Entry entry, Class<? extends Entry> clazz) {
    if (DEBUG) Log.v(TAG, "Received entry for " + clazz.toString());
    mEntries.put(clazz, entry);
  }

  @Override
  public void onExistingEntryLoaded(Entry entry, Class<? extends Entry> clazz) {
    if (DEBUG) Log.v(TAG, "Received existing entry for " + clazz.toString());
    mExistingEntries.put(clazz, entry);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_detail);

    mUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    mCycle = getCycle(getIntent());
    mCycleProvider = CycleProvider.forDb(FirebaseDatabase.getInstance());

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(getTitle(getIntent()));
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getIntent(), mCycle);

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
      updateEntryMapFromUIs();
      Queue<EntryFragment.ValidationIssue> issues = new ConcurrentLinkedQueue<>();
      for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
        EntryFragment fragment = mSectionsPagerAdapter.getCachedItem(mViewPager, i);
        issues.addAll(fragment.validateEntry(mEntries.get(fragment.getClazz())));
      }
      addressValidationIssues(issues);
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
      if (anyPagesDirty()) {
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

  private boolean anyPagesDirty() {
    updateEntryMapFromUIs();
    MapDifference<Class<? extends Entry>, Entry> difference =
        Maps.difference(mExistingEntries, mEntries);
    return !difference.areEqual();
  }

  private void updateEntryMapFromUIs() {
    for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
      EntryFragment fragment = mSectionsPagerAdapter.getCachedItem(mViewPager, i);
      try {
        Entry entry = fragment.getEntryFromUi();
        if (entry != null) {
          Class<? extends Entry> clazz = fragment.getClazz();
          if (DEBUG) Log.v(TAG, "Updating entry for " + clazz.toString());
          mEntries.put(clazz, entry);
        }
      } catch (Exception e) {
      }
    }
  }

  private void addressValidationIssues(final Queue<EntryFragment.ValidationIssue> issues) {
    if (issues.isEmpty()) {
      doSave();
      return;
    }
    EntryFragment.ValidationIssue issue = issues.remove();
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(issue.title);
    builder.setMessage(issue.message);
    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        addressValidationIssues(issues);
      }
    });
    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });
    builder.create().show();
  }

  private void doSave() {
    final Callbacks.Callback<Cycle> cycleCallback = new Callbacks.HaltingCallback<Cycle>() {
      @Override
      public void acceptData(final Cycle cycleForEntries) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(Cycle.class.getName(), cycleForEntries);
        setResult(OK_RESPONSE, returnIntent);
        finish();
      }
    };

    if (DEBUG) Log.v(TAG, "Checking for updates to entry on cycle: " + mCycle.id);
    final AtomicInteger numSaves = new AtomicInteger(mSectionsPagerAdapter.getCount());
    for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
      final EntryFragment fragment = mSectionsPagerAdapter.getCachedItem(mViewPager, i);
      Class<? extends Entry> clazz = fragment.getClazz();
      if (!mEntries.containsKey(clazz)) {
        if (DEBUG) Log.v(TAG, "Skipping " + clazz + " no entry");
        numSaves.decrementAndGet();
        continue;
      }
      if (mEntries.containsKey(clazz) && mExistingEntries.containsKey(clazz)
          && mEntries.get(clazz).equals(mExistingEntries.get(clazz))) {
        if (DEBUG) Log.v(TAG, "Skipping " + clazz + " no change");
        numSaves.decrementAndGet();
        continue;
      }

      if (DEBUG) Log.v(TAG, "Putting " + clazz);
      final EntryProvider provider =
          mSectionsPagerAdapter.getCachedItem(mViewPager, i).getEntryProvider();

      new AsyncTask<Void, Integer, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          try {
            provider.putEntry(mCycle.id, mEntries.get(fragment.getClazz()), Listeners.completionListener(cycleCallback, new Runnable() {
              @Override
              public void run() {
                if (numSaves.decrementAndGet() == 0) {
                  if (DEBUG) Log.v(TAG, "Done putting entries");
                  ChartEntryFragment chartEntryFragment =
                      (ChartEntryFragment) mSectionsPagerAdapter.getCachedItem(mViewPager, 0);

                  if (chartEntryFragment.shouldSplitCycle()) {
                    if (DEBUG) Log.v(TAG, "Splitting cycle");
                    try {
                      mCycleProvider.splitCycle(
                          mUserId, mCycle, chartEntryFragment.getEntryFromUi(), cycleCallback);
                    } catch (Exception e) {
                      cycleCallback.handleError(DatabaseError.fromException(e));
                    }
                  } else if (chartEntryFragment.shouldJoinCycle()) {
                    if (DEBUG) Log.v(TAG, "Joining cycle with previous");
                    joinCycle(cycleCallback);
                  } else {
                    cycleCallback.acceptData(mCycle);
                  }
                }
              }
            }));
          } catch (CyrptoExceptions.CryptoException ce) {
            cycleCallback.handleError(DatabaseError.fromException(ce));
          }
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void joinCycle(final Callbacks.Callback<Cycle> newCycleCallback) {
    if (Strings.isNullOrEmpty(mCycle.previousCycleId)) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("No Previous Cycle");
      builder.setMessage("Please add cycle before this entry to proceed.");
      builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          dialog.dismiss();
        }
      });
      builder.create().show();
    } else {
      mCycleProvider.combineCycles(mCycle, mUserId, newCycleCallback);
    }
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final Intent intent;
    private final Cycle mCycle;

    public SectionsPagerAdapter(FragmentManager fm, Intent intent, Cycle cycle) {
      super(fm);
      this.intent = intent;
      mCycle = cycle;
    }

    public EntryFragment getCachedItem(ViewGroup container, int position) {
      return (EntryFragment) instantiateItem(container, position);
    }

    @Override
    public EntryFragment getItem(int position) {
      Bundle args = new Bundle();
      args.putString(Extras.ENTRY_DATE_STR, intent.getStringExtra(Extras.ENTRY_DATE_STR));
      args.putParcelable(Cycle.class.getName(), mCycle);

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
          if (DEBUG) Log.v(TAG, "Creating new instance of ChartEntryFragment");
          return fragment;
        case 1:
          fragment = new WellnessEntryFragment();
          fragment.setArguments(args);
          if (DEBUG) Log.v(TAG, "Creating new instance of WellnessEntryFragment");
          return fragment;
        case 2:
          fragment = new SymptomEntryFragment();
          fragment.setArguments(args);
          if (DEBUG) Log.v(TAG, "Creating new instance of SymptomEntryFragment");
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
}
