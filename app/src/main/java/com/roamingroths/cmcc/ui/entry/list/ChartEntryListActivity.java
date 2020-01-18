package com.roamingroths.cmcc.ui.entry.list;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.backup.AppStateExporter;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.ui.entry.EntrySaveResult;
import com.roamingroths.cmcc.ui.instructions.InstructionsListActivity;
import com.roamingroths.cmcc.ui.print.PrintChartActivity;
import com.roamingroths.cmcc.ui.profile.ProfileActivity;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.GsonUtil;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.io.File;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class ChartEntryListActivity extends AppCompatActivity
    implements EntryListView, NavigationView.OnNavigationItemSelectedListener {

  private static final boolean DEBUG = true;
  private static final String TAG = ChartEntryListActivity.class.getSimpleName();

  public static final int RC_SIGN_IN = 1;

  private NavigationView mNavView;
  private Toolbar mToolbar;
  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;
  private TextView mErrorView;
  private ProgressBar mProgressBar;
  private ViewPager mViewPager;
  private FloatingActionButton mNewCycleFab;

  private CycleRepo mCycleRepo;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private EntryListViewModel mViewModel;

  private final Subject<String> mLayerSubject = BehaviorSubject.create();
  private EntryListPageAdapter mPageAdapter;

  public Observable<String> layerStream() {
    return mLayerSubject;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_list);

    mCycleRepo = new CycleRepo(MyApplication.cast(getApplication()).db());

    mNavView = findViewById(R.id.nav_view);
    // Set the "My Chart" item as selected
    mNavView.setNavigationItemSelectedListener(this);

    View navHeaderView = mNavView.getHeaderView(0);
    final TextView drawerTitleView = navHeaderView.findViewById(R.id.drawer_title);
    final TextView drawerSubtitleView = navHeaderView.findViewById(R.id.drawer_subtitle);
    //drawerSubtitleView.setText(Objects.requireNonNull(user).getEmail());

    mToolbar = findViewById(R.id.app_bar);
    setTitle("Current Cycle");
    setSupportActionBar(mToolbar);
    mToolbar.setSubtitle("Stats TBD");

    mToolbar.setOnClickListener(view -> {
      new AlertDialog.Builder(this)
          .setTitle("Cycle Stats")
          .setMessage("Stats can go here.")
          .setPositiveButton("Close", (dialogInterface, i) -> dialogInterface.dismiss())
          .show();
    });

    mDrawerLayout = findViewById(R.id.drawer_layout);
    mDrawerToggle = new ActionBarDrawerToggle(
        this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    mDrawerLayout.addDrawerListener(mDrawerToggle);
    mDrawerToggle.syncState();

    mErrorView = findViewById(R.id.refresh_error);
    mProgressBar = findViewById(R.id.progress_bar);
    mViewPager = findViewById(R.id.view_pager);

    drawerTitleView.setText("TODO: name");

    mPageAdapter = new EntryListPageAdapter(getSupportFragmentManager());

    mViewModel = ViewModelProviders.of(this).get(EntryListViewModel.class);
    mViewModel.viewStates().observe(this, viewState -> {
      setTitle(viewState.title);
      setSubtitle(viewState.subtitle);
      mPageAdapter.update(viewState.cycles);
      mPageAdapter.onPageActive(viewState.currentCycleIndex);

      if (viewState.showFab) {
        showFab();
      } else {
        hideFab();
      }
    });

    mViewPager.setAdapter(mPageAdapter);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

      @Override
      public void onPageSelected(int position) {
        mViewModel.currentPageUpdates.onNext(position);
        mPageAdapter.onPageActive(position);
      }

      @Override
      public void onPageScrollStateChanged(int state) {}
    });
    mViewPager.setOffscreenPageLimit(4);
    showList();

    mNewCycleFab = findViewById(R.id.fab_new_cycle);
    mNewCycleFab.setOnClickListener(__ -> {
      EntryListPageAdapter adapter = (EntryListPageAdapter) mViewPager.getAdapter();
      Cycle cycle = adapter.getCycle(mViewPager.getCurrentItem());
      final LocalDate endDate = cycle.startDate.minusDays(1);
      DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
          (view, year, monthOfYear, dayOfMonth) -> {
            LocalDate startDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
            AlertDialog.Builder builder = new AlertDialog.Builder(ChartEntryListActivity.this);
            builder.setTitle("Create Cycle");
            builder.setIcon(R.drawable.ic_save_black_24dp);
            builder.setMessage(
                String.format("Create cycle starting %s (%d days)?",
                DateUtil.toPrintUiStr(startDate),
                Days.daysBetween(startDate, endDate).getDays()));
            builder.setPositiveButton("Create", (dialog, which) -> {
              Cycle newCycle = new Cycle("baz", startDate, endDate);
              mDisposables.add(mCycleRepo
                  .insertOrUpdate(newCycle)
                  .doOnSubscribe(s -> dialog.dismiss())
                  .doOnComplete(() -> mViewPager.setCurrentItem(mPageAdapter.getItemPosition(newCycle)))
                  .subscribe(() -> Timber.i("New cycle created"), Timber::w));
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
          });
      datePickerDialog.setTitle("First day of previous cycle");
      datePickerDialog.setMaxDate(endDate.toDateTimeAtCurrentTime().toGregorianCalendar());
      datePickerDialog.show(getFragmentManager(), "datepickerdialog");
    });
    hideFab();
  }

  @Override
  protected void onResume() {
    setNavItem();
    super.onResume();
  }

  private void setNavItem() {
    mNavView.setCheckedItem(R.id.nav_my_chart);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (data != null) {
      final EntrySaveResult result = Parcels.unwrap(data.getParcelableExtra(EntrySaveResult.class.getName()));
      //if (DEBUG) Log.v(TAG, "Received cycleToShow:" + result.cycleToShow + " in result");
      //mViewPager.setCurrentItem(mPageAdapter.onResult(result));
    }
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_entry_list, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_layer) {
      FragmentManager fm = getSupportFragmentManager();
      LayerDialogFragment fragment = new LayerDialogFragment();
      fragment.show(fm, "tag");
      return true;
    }

    if (id == R.id.action_print) {
      startActivity(new Intent(this, PrintChartActivity.class));
      return true;
    }

    if (id == R.id.action_drop_cycles) {
      /*new AlertDialog.Builder(this)
          //set message, title, and icon
          .setTitle("Delete All Cycles?")
          .setMessage("This is permanent and cannot be undone!")
          .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {
              showProgress();
              mDisposables.add(mPageAdapter
                  .subscribe(adapter -> adapter.shutdown(mViewPager)));
              mDisposables.add(Single
                  .merge(Single.zip(
                      mCycleProvider, MyApplication.getCurrentUser().toSingle(), CycleProvider::dropCycles))
                  .flatMapCompletable(UpdateHandle.run())
                  .subscribe(() -> {
                    Intent intent = new Intent(ChartEntryListActivity.this, UserInitActivity.class);
                    startActivity(intent);
                    dialog.dismiss();
                  }));
            }
          })
          .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
          })
          .create().show();
      return true;*/
    }
    if (id == R.id.action_export) {
      Log.v("PrintChartActivity", "Begin export");
      final ChartEntryListActivity activity = this;
      AppStateExporter exporter = new AppStateExporter(MyApplication.cast(getApplication()));
      mDisposables.add(exporter.export()
          .map(appState -> GsonUtil.getGsonInstance().toJson(appState))
          .subscribe(json -> {
            File path = new File(activity.getFilesDir(), "tmp/");
            if (!path.exists()) {
              path.mkdir();
            }
            File file = new File(path, "cmcc_export.chart");

            Files.write(json, file, Charsets.UTF_8);

            Uri uri = FileProvider.getUriForFile(activity, "com.roamingroths.cmcc.fileprovider", file);

            Intent shareIntent = ShareCompat.IntentBuilder.from(activity)
                .setSubject("CMCC Export")
                .setEmailTo(null)
                .setType("application/json")
                .setStream(uri)
                .getIntent();
            shareIntent.setData(uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(shareIntent);
          }));
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void showList() {
    mViewPager.setVisibility(View.VISIBLE);
    mErrorView.setVisibility(View.INVISIBLE);
    mProgressBar.setVisibility(View.INVISIBLE);
  }

  @Override
  public void setOverlay(String key) {
    mLayerSubject.onNext(key);
    Log.i(TAG, "Overlay: " + key);
  }

  @Override
  protected void onDestroy() {
    mDisposables.dispose();

    super.onDestroy();
  }

  @Override
  public void setTitle(String title) {
    mToolbar.setTitle(title);
  }

  @Override
  public void setSubtitle(String subtitle) {
    mToolbar.setSubtitle(subtitle);
  }

  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    switch (item.getItemId()) {
      case R.id.nav_instructions:
        startActivity(new Intent(this, InstructionsListActivity.class));
        break;
      case R.id.nav_profile:
        // TODO: get updated profile
        startActivity(new Intent(this, ProfileActivity.class));
        break;
      case R.id.nav_settings:
        startActivity(new Intent(this, SettingsActivity.class));
        break;
      case R.id.nav_share:
      case R.id.nav_reference:
      case R.id.nav_help_and_feedback:
        Toast.makeText(this, "Work in progress.", Toast.LENGTH_SHORT).show();
        break;
    }
    // TODO: check items

    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    setNavItem();
    return true;
  }

  private void showFab() {
    mNewCycleFab.setVisibility(View.VISIBLE);
  }

  private void hideFab() {
    mNewCycleFab.setVisibility(View.INVISIBLE);
  }
}
