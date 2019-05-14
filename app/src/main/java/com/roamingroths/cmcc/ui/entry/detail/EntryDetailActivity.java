package com.roamingroths.cmcc.ui.entry.detail;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.Entry;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.entities.SymptomEntry;
import com.roamingroths.cmcc.data.entities.WellnessEntry;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.ui.entry.EntrySaveResult;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.roamingroths.cmcc.ui.entry.detail.ObservationEntryFragment.OK_RESPONSE;

public class EntryDetailActivity extends AppCompatActivity implements EntryFragment.EntryListener {

  private static final boolean DEBUG = true;
  private static final String TAG = EntryDetailActivity.class.getSimpleName();

  private static final Joiner ON_NEW_LINE = Joiner.on('\n');

  /**
   * The {@link PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  private ChartEntryRepo mEntryRepo;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;
  private EntryContext mEntryContext;

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

    mEntryRepo = new ChartEntryRepo(MyApplication.cast(getApplication()).db());

    if (DEBUG) Log.v(TAG, "onCreate: Start");

    Intent intent = getIntent();
    mEntryContext = Parcels.unwrap(intent.getParcelableExtra(EntryContext.class.getCanonicalName()));

    updateMaps(mEntryContext.chartEntry);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(getTitle(mEntryContext.currentCycle, mEntryContext.chartEntry.entryDate));
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), mEntryContext);

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
          .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
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
          mDisposables.add(Observable.fromIterable(issues)
              .flatMap(addressIssue())
              .filter(shouldProceed -> !shouldProceed)
              .toList()
              .flatMapMaybe(unresolvedIssues -> unresolvedIssues.isEmpty()
                  ? doSave(progressSummary::setText).toMaybe()
                  : Maybe.empty())
              .subscribeOn(Schedulers.computation())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(result -> {
                Intent returnIntent = new Intent();
                returnIntent.putExtra(EntrySaveResult.class.getName(), Parcels.wrap(result));
                returnIntent.putExtra(ChartEntry.class.getName(), getChartEntry());
                setResult(OK_RESPONSE, returnIntent);
                dialog.dismiss();
                finish();
              }, throwable -> {
                // TODO: make this better
                Timber.e(throwable);
                dialog.dismiss();
                finish();
              }));
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
        mEntryContext.chartEntry.entryDate,
        (ObservationEntry) mEntries.get(ObservationEntry.class),
        (WellnessEntry) mEntries.get(WellnessEntry.class),
        (SymptomEntry) mEntries.get(SymptomEntry.class));
  }

  private Single<EntrySaveResult> doSave(final Consumer<String> updateConsumer) {
    if (DEBUG) Log.v(TAG, "Checking for updates to entry on cycleToShow: " + mEntryContext.currentCycle.id);
    final ChartEntry entry = updateEntryMapFromUIs();

    return mEntryRepo.insert(entry)
        .andThen(Single.just(EntrySaveResult.forCycle(mEntryContext.currentCycle)));

    /*if (observationEntryFragment.shouldSplitCycle()) {
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
    }*/
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final EntryContext mEntryContext;

    public SectionsPagerAdapter(FragmentManager fm, EntryContext entryContext) {
      super(fm);
      mEntryContext = entryContext;
    }

    public EntryFragment getCachedItem(ViewGroup container, int position) {
      return (EntryFragment) instantiateItem(container, position);
    }

    @Override
    public EntryFragment getItem(int position) {
      Bundle args = new Bundle();
      args.putParcelable(EntryContext.class.getCanonicalName(), Parcels.wrap(mEntryContext));

      EntryFragment fragment;
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      switch (position) {
        case 0:
          fragment = new ObservationEntryFragment();
          fragment.setArguments(args);
          if (DEBUG) Log.v(TAG, "Creating new instance of ObservationEntryFragment");
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
          return "Observation";
        case 1:
          return "Wellness";
        case 2:
          return "Symptoms";
      }
      return null;
    }
  }

  private String getSaveMessage(ChartEntry container) {
    List<String> lines = new ArrayList<>();

    int dayNum = 1 + Days.daysBetween(mEntryContext.currentCycle.startDate, container.entryDate).getDays();
    lines.add("Day #" + dayNum + " Summary\n");
    lines.addAll(container.getSummaryLines());
    return ON_NEW_LINE.join(lines);
  }
}
