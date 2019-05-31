package com.roamingroths.cmcc.ui.entry.detail;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Joiner;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

import static com.roamingroths.cmcc.ui.entry.detail.ObservationEntryFragment.OK_RESPONSE;

public class EntryDetailActivity extends AppCompatActivity {

  private static final boolean DEBUG = true;
  private static final String TAG = EntryDetailActivity.class.getSimpleName();

  private static final Joiner ON_NEW_LINE = Joiner.on('\n');

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private EntryDetailViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_detail);

    if (DEBUG) Log.v(TAG, "onCreate: Start");

    Intent intent = getIntent();
    EntryContext entryContext = Parcels.unwrap(intent.getParcelableExtra(EntryContext.class.getCanonicalName()));

    mViewModel = ViewModelProviders.of(this).get(EntryDetailViewModel.class);
    mViewModel.initialize(entryContext);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(getTitle(entryContext.currentCycle, entryContext.chartEntry.entryDate));
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), entryContext);

    // Set up the ViewPager with the sections adapter.
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager = findViewById(R.id.container);
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

  private Single<Boolean> addressIssue(EntryDetailViewModel.ValidationIssue issue) {
    return Single.create(emitter -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(EntryDetailActivity.this);
      builder.setTitle(issue.summary);
      builder.setMessage(issue.details);
      builder.setPositiveButton("Yes", (dialog, which) -> {
        emitter.onSuccess(true);
        dialog.dismiss();
      });
      builder.setNegativeButton("No", (dialog, which) -> {
        emitter.onSuccess(false);
        dialog.dismiss();
      });
      builder.create().show();
    });
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
      mDisposables.add(mViewModel.getSaveSummary().subscribe(summaryLines -> {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_save_entry, null);

        final TextView summary = dialogView.findViewById(R.id.save_summary);
        List<String> lines = new ArrayList<>();
        lines.add("Entry Summary\n");
        lines.addAll(summaryLines);
        summary.setText(ON_NEW_LINE.join(lines));

        final ProgressBar progressBar = dialogView.findViewById(R.id.save_progress_bar);
        progressBar.setVisibility(View.GONE);
        final TextView progressSummary = dialogView.findViewById(R.id.save_progress_summary);
        progressSummary.setVisibility(View.GONE);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
            //set message, title, and icon
            .setTitle("Save Entry?")
            .setIcon(R.drawable.ic_assignment_black_24dp)
            .setPositiveButton("Save", (dialogInterface, i) -> {
              mDisposables.add(mViewModel
                  .save(this::addressIssue)
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(() -> {
                Intent returnIntent = new Intent();
                //returnIntent.putExtra(EntrySaveResult.class.getName(), Parcels.wrap(result));
                //returnIntent.putExtra(ChartEntry.class.getName(), getChartEntry());
                setResult(OK_RESPONSE, returnIntent);
                dialogInterface.dismiss();
                finish();
              }, (throwable) -> {
                Timber.e(throwable);
                dialogInterface.dismiss();
              }));
               // TODO:save
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        dialogBuilder.setView(dialogView);
        final AlertDialog dialog = dialogBuilder.create();
        dialog.show();
      }));
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
      mDisposables.add(mViewModel.isDirty().subscribe(isDirty -> {
        if (isDirty) {
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
      }));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private static String getTitle(Cycle cycle, LocalDate entryDate) {
    int daysBetween = Days.daysBetween(cycle.startDate, entryDate).getDays();
    return "Day #" + (daysBetween + 1);
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

    @Override
    public Fragment getItem(int position) {
      Bundle args = new Bundle();
      args.putParcelable(EntryContext.class.getCanonicalName(), Parcels.wrap(mEntryContext));

      Fragment fragment = null;
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      switch (position) {
        case 0:
          fragment = new ObservationEntryFragment();
          break;
        case 1:
          fragment = new WellnessEntryFragment();
          break;
        case 2:
          fragment = new SymptomEntryFragment();
          break;
      }
      if (fragment == null) {
        Timber.e("Fragment should not be null!");
        return null;
      } else {
        fragment.setArguments(args);
        return fragment;
      }
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
}
