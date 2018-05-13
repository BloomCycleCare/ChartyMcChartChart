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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.logic.chart.ChartEntry;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.logic.chart.Entry;
import com.roamingroths.cmcc.logic.chart.ObservationEntry;
import com.roamingroths.cmcc.logic.chart.SymptomEntry;
import com.roamingroths.cmcc.logic.chart.WellnessEntry;
import com.roamingroths.cmcc.providers.ChartEntryProvider;
import com.roamingroths.cmcc.providers.CycleProvider;
import com.roamingroths.cmcc.ui.entry.EntrySaveResult;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import static com.roamingroths.cmcc.ui.entry.detail.ObservationEntryFragment.OK_RESPONSE;

public class EntryDetailActivity extends AppCompatActivity implements EntryFragment.EntryListener {

  public enum Extras {
    CURRENT_CYCLE, CHART_ENTRY, EXPECT_UNUSUAL_BLEEDING, HAS_PREVIOUS_CYCLE, IS_FIRST_ENTRY
  }

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

    Intent intent = getIntent();
    ChartEntry chartEntry = intent.getParcelableExtra(Extras.CHART_ENTRY.name());
    mDate = chartEntry.entryDate;
    mCycle = intent.getParcelableExtra(Extras.CURRENT_CYCLE.name());
    boolean hasPreviousCycle = intent.getBooleanExtra(Extras.HAS_PREVIOUS_CYCLE.name(), false);
    boolean expectUnusualBleeding = intent.getBooleanExtra(Extras.EXPECT_UNUSUAL_BLEEDING.name(), false);
    boolean isFirstEntry = intent.getBooleanExtra(Extras.IS_FIRST_ENTRY.name(), false);

    mUser = FirebaseAuth.getInstance().getCurrentUser();

    updateMaps(chartEntry);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(getTitle(mCycle, mDate));
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter =
        new SectionsPagerAdapter(getSupportFragmentManager(), expectUnusualBleeding, hasPreviousCycle, isFirstEntry);

    // Set up the ViewPager with the sections adapter.
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    TabLayout tabLayout = findViewById(R.id.tabs);
    tabLayout.setupWithViewPager(mViewPager);

    if (DEBUG) Log.v(TAG, "onCreate: Finish");
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_entry_detail, menu);
    return true;
  }

  private Function<EntryFragment.ValidationIssue, ObservableSource<Boolean>> addressIssue() {
    return new Function<EntryFragment.ValidationIssue, ObservableSource<Boolean>>() {
      @Override
      public ObservableSource<Boolean> apply(final EntryFragment.ValidationIssue issue) throws Exception {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
          @Override
          public void subscribe(final ObservableEmitter<Boolean> e) throws Exception {
            AlertDialog.Builder builder = new AlertDialog.Builder(EntryDetailActivity.this);
            builder.setTitle(issue.title);
            builder.setMessage(issue.message);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                e.onNext(true);
                dialog.dismiss();
                e.onComplete();
              }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                e.onNext(true);
                dialog.dismiss();
                e.onNext(false);
              }
            });
            builder.create().show();
          }
        });
      }
    };
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

      LayoutInflater inflater = getLayoutInflater();
      View dialogView = inflater.inflate(R.layout.dialog_save_entry, null);

      final TextView summary = dialogView.findViewById(R.id.save_summary);
      summary.setText(getSaveMessage(getChartEntry()));

      final ProgressBar progressBar = dialogView.findViewById(R.id.save_progress_bar);
      progressBar.setVisibility(View.GONE);
      final TextView progressSummary = dialogView.findViewById(R.id.save_progress_summary);
      progressSummary.setVisibility(View.GONE);

      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
          //set message, title, and icon
          .setTitle("Save Entry?")
          .setIcon(R.drawable.ic_assignment_black_24dp)
          .setPositiveButton("Save", null)
          .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
          });
      dialogBuilder.setView(dialogView);

      final AlertDialog dialog = dialogBuilder.create();
      dialog.show(); // Must be called before asking for the button (https://goo.gl/KhLsRy)
      final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
      final Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
      positiveButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          positiveButton.setVisibility(View.GONE);
          negativeButton.setVisibility(View.GONE);
          summary.setVisibility(View.GONE);
          progressBar.setVisibility(View.VISIBLE);
          progressSummary.setVisibility(View.VISIBLE);
          progressSummary.setText("Saving changes");
          Queue<EntryFragment.ValidationIssue> issues = new ConcurrentLinkedQueue<>();
          for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            EntryFragment fragment = mSectionsPagerAdapter.getCachedItem(mViewPager, i);
            issues.addAll(fragment.validateEntry(mEntries.get(fragment.getClazz())));
          }
          Observable.fromIterable(issues)
              .flatMap(addressIssue())
              .toList()
              .flatMapMaybe(new Function<List<Boolean>, MaybeSource<EntrySaveResult>>() {
                @Override
                public MaybeSource<EntrySaveResult> apply(List<Boolean> shouldProceedValues) throws Exception {
                  if (!shouldProceedValues.isEmpty()
                      && !Collections2.filter(shouldProceedValues, Predicates.equalTo(false)).isEmpty()) {
                    return Maybe.empty();
                  }
                  return doSave(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                      progressSummary.setText(s);
                    }
                  }).toMaybe();
                }
              })
              .subscribe(new Consumer<EntrySaveResult>() {
                @Override
                public void accept(EntrySaveResult result) throws Exception {
                  Intent returnIntent = new Intent();
                  returnIntent.putExtra(EntrySaveResult.class.getName(), result);
                  returnIntent.putExtra(ChartEntry.class.getName(), getChartEntry());
                  setResult(OK_RESPONSE, returnIntent);
                  dialog.dismiss();
                  finish();
                }
              }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                  // TODO: make this better
                  Log.e(TAG, "Error saving entry.", throwable);
                  dialog.dismiss();
                  finish();
                }
              });
        }
      });
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

  private static String getTitle(Cycle cycle, LocalDate entryDate) {
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

  private Single<EntrySaveResult> doSave(final Consumer<String> updateConsumer) {
    if (DEBUG) Log.v(TAG, "Checking for updates to entry on cycleToShow: " + mCycle.id);
    final ChartEntry entry = updateEntryMapFromUIs();

    Completable putDone = MyApplication.chartEntryProvider().flatMapCompletable(new Function<ChartEntryProvider, CompletableSource>() {
      @Override
      public CompletableSource apply(ChartEntryProvider chartEntryProvider) throws Exception {
        return chartEntryProvider.putEntry(mCycle, entry);
      }
    });

    if (DEBUG) Log.v(TAG, "Done putting entries");
    ObservationEntryFragment observationEntryFragment =
        (ObservationEntryFragment) mSectionsPagerAdapter.getCachedItem(mViewPager, 0);

    if (observationEntryFragment.shouldSplitCycle()) {
      if (DEBUG) Log.v(TAG, "Splitting cycleToShow");
      final Single<LocalDate> firstEntryDate = observationEntryFragment.getEntryFromUiRx().map(new Function<ObservationEntry, LocalDate>() {
        @Override
        public LocalDate apply(ObservationEntry observationEntry) throws Exception {
          return observationEntry.getDate();
        }
      });
      Single<EntrySaveResult> splitCycle = MyApplication.cycleProvider().flatMap(new Function<CycleProvider, SingleSource<EntrySaveResult>>() {
        @Override
        public Single<EntrySaveResult> apply(CycleProvider cycleProvider) throws Exception {
          return cycleProvider.splitCycleRx(mUser, mCycle, firstEntryDate, updateConsumer);
        }
      });
      return putDone.andThen(splitCycle);
    } else if (observationEntryFragment.shouldJoinCycle()) {
      if (DEBUG) Log.v(TAG, "Joining cycleToShow with previous");
      Single<EntrySaveResult> combineCyles = MyApplication.cycleProvider().flatMap(new Function<CycleProvider, SingleSource<EntrySaveResult>>() {
        @Override
        public Single<EntrySaveResult> apply(CycleProvider cycleProvider) throws Exception {
          return cycleProvider.combineCycleRx(mUser, mCycle, updateConsumer);
        }
      });
      return putDone.andThen(combineCyles);
    }
    return putDone.andThen(Single.just(EntrySaveResult.forCycle(mCycle)));
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final boolean mExpectUnusualBleeding;
    private final boolean mHasPreviousCycle;
    private final boolean mIsFirstEntry;

    public SectionsPagerAdapter(FragmentManager fm, boolean expectUnusualBleeding, boolean hasPreviousCycle, boolean isFirstEntry) {
      super(fm);
      mExpectUnusualBleeding = expectUnusualBleeding;
      mHasPreviousCycle = hasPreviousCycle;
      mIsFirstEntry = isFirstEntry;
    }

    public EntryFragment getCachedItem(ViewGroup container, int position) {
      return (EntryFragment) instantiateItem(container, position);
    }

    @Override
    public EntryFragment getItem(int position) {
      Bundle args = new Bundle();
      args.putParcelable(EntryFragment.Extras.CURRENT_CYCLE.name(), mCycle);

      EntryFragment fragment;
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      switch (position) {
        case 0:
          args.putBoolean(ObservationEntryFragment.Extras.EXPECT_UNUSUAL_BLEEDING.name(), mExpectUnusualBleeding);
          args.putBoolean(ObservationEntryFragment.Extras.HAS_PREVIOUS_CYCLE.name(), mHasPreviousCycle);
          args.putBoolean(ObservationEntryFragment.Extras.IS_FIRST_ENTRY.name(), mIsFirstEntry);
          args.putParcelable(EntryFragment.Extras.EXISTING_ENTRY.name(), getExistingEntry(ObservationEntry.class));

          fragment = new ObservationEntryFragment();
          fragment.setArguments(args);
          if (DEBUG) Log.v(TAG, "Creating new instance of ObservationEntryFragment");
          return fragment;
        case 1:
          args.putParcelable(EntryFragment.Extras.EXISTING_ENTRY.name(), getExistingEntry(WellnessEntry.class));

          fragment = new WellnessEntryFragment();
          fragment.setArguments(args);
          if (DEBUG) Log.v(TAG, "Creating new instance of WellnessEntryFragment");
          return fragment;
        case 2:
          args.putParcelable(EntryFragment.Extras.EXISTING_ENTRY.name(), getExistingEntry(SymptomEntry.class));

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
