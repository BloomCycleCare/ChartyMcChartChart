package com.roamingroths.cmcc.ui.entry.detail;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.Extras;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.logic.ObservationEntry;
import com.roamingroths.cmcc.logic.SymptomEntry;
import com.roamingroths.cmcc.logic.WellnessEntry;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.roamingroths.cmcc.ui.entry.detail.ObservationEntryFragment.OK_RESPONSE;

public class EntryDetailActivity extends AppCompatActivity implements EntryFragment.EntryListener {

  private static final boolean DEBUG = true;
  private static final String TAG = EntryDetailActivity.class.getSimpleName();

  private static final Joiner ON_NEW_LINE = Joiner.on('\n');

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
  private ChartEntryProvider mChartEntryProvider;
  private FirebaseUser mUser;
  private LocalDate mDate;

  private Map<Class<? extends Entry>, Entry> mExistingEntries = new HashMap<>();
  private Map<Class<? extends Entry>, Entry> mEntries = new HashMap<>();

  @Override
  public void onEntryUpdated(Entry entry, Class<? extends Entry> clazz) {
    Preconditions.checkNotNull(entry);
    if (DEBUG) Log.v(TAG, "Received entry for " + clazz.toString());
    mEntries.put(clazz, entry);
  }

  private void updateMaps(ChartEntry container) {
    mExistingEntries.put(ObservationEntry.class, container.observationEntry);
    mExistingEntries.put(WellnessEntry.class, container.wellnessEntry);
    mExistingEntries.put(SymptomEntry.class, container.symptomEntry);

    mEntries.put(ObservationEntry.class, container.observationEntry);
    mEntries.put(WellnessEntry.class, container.wellnessEntry);
    mEntries.put(SymptomEntry.class, container.symptomEntry);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_detail);

    if (DEBUG) Log.v(TAG, "onCreate: Start");

    ChartEntry container = getEntryContainer(getIntent());
    mDate = container.entryDate;

    mUser = FirebaseAuth.getInstance().getCurrentUser();
    mCycle = getCycle(getIntent());
    mCycleProvider = MyApplication.getProviders().forCycle();
    mChartEntryProvider = MyApplication.getProviders().forChartEntry();

    updateMaps(container);

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

    if (DEBUG) Log.v(TAG, "onCreate: Finish");
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
      ChartEntry container = updateEntryMapFromUIs();

      new AlertDialog.Builder(this)
          //set message, title, and icon
          .setTitle("Save Entry?")
          .setMessage(getSaveMessage(getChartEntry()))
          .setIcon(R.drawable.ic_assignment_black_24dp)
          .setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {
              Queue<EntryFragment.ValidationIssue> issues = new ConcurrentLinkedQueue<>();
              for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
                EntryFragment fragment = mSectionsPagerAdapter.getCachedItem(mViewPager, i);
                issues.addAll(fragment.validateEntry(mEntries.get(fragment.getClazz())));
              }
              addressValidationIssues(issues);
            }
          })
          .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
          })
          .create().show();
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
                setResult(0, null);
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
        setResult(0, null);
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

  private static ChartEntry getEntryContainer(Intent intent) {
    if (!intent.hasExtra(ChartEntry.class.getName())) {
      throw new IllegalStateException();
    }
    return intent.getParcelableExtra(ChartEntry.class.getName());
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

  private ChartEntry updateEntryMapFromUIs() {
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
    return getChartEntry();
  }

  private ChartEntry getChartEntry() {
    return new ChartEntry(
        mDate,
        (ObservationEntry) mEntries.get(ObservationEntry.class),
        (WellnessEntry) mEntries.get(WellnessEntry.class),
        (SymptomEntry) mEntries.get(SymptomEntry.class));
  }

  private void addressValidationIssues(final Queue<EntryFragment.ValidationIssue> issues) {
    if (issues.isEmpty()) {
      doSave().subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<EntrySaveResult>() {
        @Override
        public void accept(EntrySaveResult result) throws Exception {
          Intent returnIntent = new Intent();
          returnIntent.putExtra(EntrySaveResult.class.getName(), result);
          returnIntent.putExtra(ChartEntry.class.getName(), getChartEntry());
          setResult(OK_RESPONSE, returnIntent);
          finish();
        }
      }, new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) throws Exception {
          Log.e(TAG, "Error saving entry.", throwable);
        }
      });
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

  private Maybe<EntrySaveResult> doSave() {
    if (DEBUG) Log.v(TAG, "Checking for updates to entry on cycle: " + mCycle.id);
    ChartEntry entry = updateEntryMapFromUIs();
    Completable putDone = mChartEntryProvider.putEntry(mCycle, entry);

    if (DEBUG) Log.v(TAG, "Done putting entries");
    ObservationEntryFragment observationEntryFragment =
        (ObservationEntryFragment) mSectionsPagerAdapter.getCachedItem(mViewPager, 0);

    if (observationEntryFragment.shouldSplitCycle()) {
      if (DEBUG) Log.v(TAG, "Splitting cycle");
      Single<LocalDate> firstEntryDate = observationEntryFragment.getEntryFromUiRx().map(new Function<ObservationEntry, LocalDate>() {
        @Override
        public LocalDate apply(ObservationEntry observationEntry) throws Exception {
          return observationEntry.getDate();
        }
      });
      return putDone
          .andThen(mCycleProvider.splitCycleRx(mUser, mCycle, firstEntryDate))
          .toMaybe();
    } else if (observationEntryFragment.shouldJoinCycle()) {
      if (DEBUG) Log.v(TAG, "Joining cycle with previous");
      return putDone.andThen(canJoin()).flatMapMaybe(new Function<Boolean, MaybeSource<EntrySaveResult>>() {
        @Override
        public MaybeSource<EntrySaveResult> apply(Boolean canJoin) throws Exception {
          if (!canJoin) {
            return Maybe.empty();
          }
          return mCycleProvider.combineCycleRx(mUser, mCycle).toMaybe();
        }
      });
    }
    return putDone.andThen(Maybe.just(new EntrySaveResult(mCycle)));
  }

  private Single<Boolean> canJoin() {
    if (!Strings.isNullOrEmpty(mCycle.previousCycleId)) {
      return Single.just(true);
    }
    return Single.create(new SingleOnSubscribe<Boolean>() {
      @Override
      public void subscribe(final SingleEmitter<Boolean> e) throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(EntryDetailActivity.this);
        builder.setTitle("No Previous Cycle");
        builder.setMessage("Please add cycle before this entry to proceed.");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            e.onSuccess(false);
          }
        });
        builder.create().show();
      }
    });
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
          args.putParcelable(Entry.class.getSimpleName(), getExistingEntry(ObservationEntry.class));

          fragment = new ObservationEntryFragment();
          fragment.setArguments(args);
          if (DEBUG) Log.v(TAG, "Creating new instance of ObservationEntryFragment");
          return fragment;
        case 1:
          args.putParcelable(Entry.class.getSimpleName(), getExistingEntry(WellnessEntry.class));
          fragment = new WellnessEntryFragment();
          fragment.setArguments(args);
          if (DEBUG) Log.v(TAG, "Creating new instance of WellnessEntryFragment");
          return fragment;
        case 2:
          args.putParcelable(Entry.class.getSimpleName(), getExistingEntry(SymptomEntry.class));
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
          return "Observation";
        case 1:
          return "Wellness";
        case 2:
          return "Symptoms";
      }
      return null;
    }
  }

  private Entry getExistingEntry(Class<? extends Entry> clazz) {
    return mExistingEntries.get(clazz);
  }

  private String getSaveMessage(ChartEntry container) {
    List<String> lines = new ArrayList<>();

    int dayNum = 1 + Days.daysBetween(mCycle.startDate, container.entryDate).getDays();
    lines.add("Day #" + dayNum + " Summary\n");
    lines.addAll(container.getSummaryLines());
    return ON_NEW_LINE.join(lines);
  }
}
