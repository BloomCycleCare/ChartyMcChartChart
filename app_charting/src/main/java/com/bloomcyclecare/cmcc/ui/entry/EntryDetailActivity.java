package com.bloomcyclecare.cmcc.ui.entry;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.observation.ClarifyingQuestion;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Joiner;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

import static com.bloomcyclecare.cmcc.ui.entry.ObservationEntryFragment.OK_RESPONSE;

public class EntryDetailActivity extends AppCompatActivity {

  private static final boolean DEBUG = true;
  private static final String TAG = EntryDetailActivity.class.getSimpleName();

  private static final Joiner ON_NEW_LINE = Joiner.on('\n');

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private EntryDetailViewModel mViewModel;

  public static Intent createIntent(Context context, CycleRenderer.EntryModificationContext entryModificationContext) {
    Intent intent = new Intent(context, EntryDetailActivity.class);
    intent.putExtra(CycleRenderer.EntryModificationContext.class.getCanonicalName(), Parcels.wrap(entryModificationContext));
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_detail);

    if (DEBUG) Log.v(TAG, "onCreate: Start");

    Intent intent = getIntent();
    CycleRenderer.EntryModificationContext entryModifyContext = Parcels.unwrap(
        intent.getParcelableExtra(CycleRenderer.EntryModificationContext.class.getCanonicalName()));

    mViewModel = ViewModelProviders.of(this).get(EntryDetailViewModel.class);
    mViewModel.initialize(entryModifyContext);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(getTitle(entryModifyContext.cycle, entryModifyContext.entry.entryDate));
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
    SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), entryModifyContext);

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

  private Menu menu;

  public Menu getMenu() {
    return menu;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_entry_detail, menu);
    this.menu = menu;
    return true;
  }

  private Single<Boolean> addressIssue(EntryDetailViewModel.ValidationIssue issue) {
    return Single.create(emitter -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(EntryDetailActivity.this);
      builder.setTitle(issue.summary);
      builder.setMessage(issue.details);
      switch (issue.action) {
        case CONFIRM:
          builder.setPositiveButton("Yes", (dialog, which) -> {
            emitter.onSuccess(true);
            dialog.dismiss();
          });
          builder.setNegativeButton("No", (dialog, which) -> {
            emitter.onSuccess(false);
            dialog.dismiss();
          });
          break;
        case BLOCK:
          builder.setPositiveButton("Ok", (dialog, which) -> {
            emitter.onSuccess(false);
            dialog.dismiss();
          });
          break;
      }
      builder.create().show();
    });
  }

  private Single<Boolean> resolveQuestion(ClarifyingQuestion question) {
    return Single.create(emitter -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(EntryDetailActivity.this);
      builder.setTitle(question.title);
      builder.setMessage(question.message);
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
    switch (item.getItemId()) {
      case R.id.action_pregnancy_test:
        mViewModel.positivePregnancyTestUpdates.onNext(true);
        return true;

      case R.id.action_save:
        mDisposables.add(mViewModel.getSaveSummary(this::resolveQuestion, this::addressIssue).subscribe(summaryLines -> {
          LayoutInflater inflater = getLayoutInflater();
          View dialogView = inflater.inflate(R.layout.dialog_save_entry, null);

          final TextView summary = dialogView.findViewById(R.id.save_summary);
          List<String> lines = new ArrayList<>();
          lines.add("Entry Summary\n");
          lines.addAll(summaryLines);
          summary.setText(ON_NEW_LINE.join(lines));

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

      case android.R.id.home:
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
    return DateUtil.toUiStr(entryDate);
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    private final CycleRenderer.EntryModificationContext mEntryModificationContext;

    public SectionsPagerAdapter(FragmentManager fm, CycleRenderer.EntryModificationContext entryModificationContext) {
      super(fm);
      mEntryModificationContext = entryModificationContext;
    }

    @Override
    public Fragment getItem(int position) {
      Bundle args = new Bundle();
      args.putParcelable(CycleRenderer.EntryModificationContext.class.getCanonicalName(), Parcels.wrap(mEntryModificationContext));

      Fragment fragment = null;
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      switch (position) {
        case 0:
          fragment = new ObservationEntryFragment();
          break;
        case 1:
          fragment = new MarquetteEntryFragment();
          break;
        case 2:
          fragment = new WellnessEntryFragment();
          break;
        case 3:
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
      // TODO: change to 4 when we're ready to show wellness and symptom stuff
      // TODO: conditionally activate mm fragment
      return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0:
          return "Observation";
        case 1:
          return "Marquette";
        case 2:
          return "Wellness";
        case 3:
          return "Symptoms";
      }
      return null;
    }
  }
}
