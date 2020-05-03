package com.bloomcyclecare.cmcc.ui.entry.list;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.backup.AppStateExporter;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.bloomcyclecare.cmcc.ui.drive.DriveActivity;
import com.bloomcyclecare.cmcc.ui.entry.list.grid.EntryGridPageFragment;
import com.bloomcyclecare.cmcc.ui.entry.list.vertical.EntryListPageFragment;
import com.bloomcyclecare.cmcc.ui.instructions.InstructionsListActivity;
import com.bloomcyclecare.cmcc.ui.pregnancy.list.PregnancyListActivity;
import com.bloomcyclecare.cmcc.ui.print.PrintChartActivity;
import com.bloomcyclecare.cmcc.ui.profile.ProfileActivity;
import com.bloomcyclecare.cmcc.ui.settings.SettingsActivity;
import com.bloomcyclecare.cmcc.ui.training.TrainingActivity;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class ChartEntryListActivity extends AppCompatActivity
    implements EntryListView, NavigationView.OnNavigationItemSelectedListener {

  public enum Extras {
    CYCLE_DESC_INDEX,
    VIEW_MODE
  }

  private enum RequestCode {
    PREGNANCY_LIST
  }

  private NavigationView mNavView;
  private Toolbar mToolbar;
  private ActionBarDrawerToggle mDrawerToggle;
  private FloatingActionButton mNewCycleFab;

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final Subject<String> mLayerSubject = BehaviorSubject.create();

  private EntryListViewModel mViewModel;

  public Observable<String> layerStream() {
    return mLayerSubject;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_list);

    mNavView = findViewById(R.id.nav_view);
    // Set the "My Chart" item as selected
    mNavView.setNavigationItemSelectedListener(this);

    View navHeaderView = mNavView.getHeaderView(0);
    final TextView drawerTitleView = navHeaderView.findViewById(R.id.drawer_title);
    drawerTitleView.setText("TODO: name");

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

    DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
    mDrawerToggle = new ActionBarDrawerToggle(
        this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    mDrawerLayout.addDrawerListener(mDrawerToggle);
    mDrawerToggle.syncState();

    if (!getIntent().hasExtra(Extras.VIEW_MODE.name())) {
      Timber.w("View mode not found, assuming CHARTING");
    }
    PreferenceRepo.PreferenceSummary preferenceSummary =
        MyApplication.cast(getApplication()).preferenceRepo().currentSummary();

    ViewMode defaultViewMode = preferenceSummary.defaultToDemoMode() ? ViewMode.DEMO : ViewMode.CHARTING;
    ViewMode viewMode = ViewMode.values()[getIntent().getIntExtra(Extras.VIEW_MODE.name(), defaultViewMode.ordinal())];
    EntryListViewModel.Factory factory = new EntryListViewModel.Factory(getApplication(), viewMode);
    mViewModel = ViewModelProviders.of(this, factory).get(EntryListViewModel.class);
    mViewModel.viewStates().observe(this, viewState -> {
      switch (viewState.viewMode) {
        case TRAINING:
          setTitle("Training Cycle");
          break;
        case DEMO:
          setTitle("Demo Cycle");
          break;
        default:
          setTitle(viewState.title);
          setSubtitle(viewState.subtitle);
          break;
      }
      if (viewState.viewMode == ViewMode.CHARTING && viewState.showFab) {
        showFab();
      } else {
        hideFab();
      }
      if (viewState.targetLayoutMode.isPresent()) {
        Fragment fragment = viewState.targetLayoutMode.get() == EntryListViewModel.LayoutMode.GRID
            ? new EntryGridPageFragment() : new EntryListPageFragment();
        Bundle args = new Bundle();
        args.putInt(ViewMode.class.getCanonicalName(), viewState.viewMode.ordinal());
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        getSupportFragmentManager().executePendingTransactions();
        mViewModel.completeLayoutTransition(viewState.targetLayoutMode.get());
      }
    });

    mNewCycleFab = findViewById(R.id.fab_new_cycle);
    /*mNewCycleFab.setOnClickListener(__ -> {
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
              Cycle newCycle = new Cycle("baz", startDate, endDate, null);
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
    });*/
    hideFab();
    maybeUpdateCurrentPage(getIntent());

    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        if (mViewModel.currentViewMode() == ViewMode.CHARTING) {
          finish();
        } else {
          mNavView.getMenu().findItem(R.id.nav_my_chart).setChecked(true);
          ViewMode targetViewMode = preferenceSummary.defaultToDemoMode()
              ? ViewMode.DEMO : ViewMode.CHARTING;
          mViewModel.setViewMode(targetViewMode);
        }
      }
    });
  }

  @Override
  protected void onNewIntent(Intent intent) {
    maybeUpdateCurrentPage(intent);
    super.onNewIntent(intent);
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
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mDrawerToggle.onConfigurationChanged(newConfig);
    switch (newConfig.orientation) {
      case ORIENTATION_LANDSCAPE:
        mViewModel.setLandscapeMode();
        break;
      case ORIENTATION_PORTRAIT:
        mViewModel.setPortraitMode();
        break;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode < 0 || requestCode > RequestCode.values().length) {
      Timber.e("Invalid request code: %d", requestCode);
      return;
    }
    switch (RequestCode.values()[requestCode]) {
      case PREGNANCY_LIST:
        maybeUpdateCurrentPage(data);
        break;
      default:
        Timber.w("Fall through for code: %d", requestCode);
    }
  }

  private void maybeUpdateCurrentPage(Intent intent) {
    if (intent != null && intent.hasExtra(Extras.CYCLE_DESC_INDEX.name())) {
      int cycleIndex = intent.getIntExtra(Extras.CYCLE_DESC_INDEX.name(), -1);
      if (cycleIndex >= 0) {
        Timber.d("Updating current cycle based on pregnancy click");
        mViewModel.currentPageUpdates.onNext(cycleIndex);
      }
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

  @SuppressLint("SourceLockedOrientationActivity")
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

    if (id == R.id.action_export) {
      Log.v("PrintChartActivity", "Begin export");
      final ChartEntryListActivity activity = this;
      AppStateExporter exporter = new AppStateExporter(MyApplication.cast(getApplication()));
      mDisposables.add(exporter.export()
          .map(appState -> GsonUtil.getGsonInstance().toJson(appState))
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeOn(Schedulers.computation())
          .subscribe(json -> {
            File path = new File(activity.getFilesDir(), "tmp/");
            if (!path.exists()) {
              path.mkdir();
            }
            File file = new File(path, "cmcc_export.chart");

            Files.write(json, file, Charsets.UTF_8);

            Uri uri = FileProvider.getUriForFile(
                activity, String.format("%s.fileprovider", getApplicationContext().getPackageName()), file);

            Intent shareIntent = ShareCompat.IntentBuilder.from(activity)
                .setSubject("CMCC Export")
                .setEmailTo(null)
                .setType("application/json")
                .setStream(uri)
                .getIntent();
            shareIntent.setData(uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(shareIntent);
          }, t -> {
            Timber.e(t, "Error exporting data");
            new AlertDialog.Builder(this)
                .setTitle("Error exporting data!")
                .setMessage(t.getMessage())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
          }));
    }
    if (id == R.id.action_trigger_sync) {
      MyApplication.cast(getApplication()).triggerSync();
      return true;
    }

    if (id == R.id.action_toggle_layout) {
      switch (getResources().getConfiguration().orientation) {
        case ORIENTATION_PORTRAIT:
          setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
          break;
        case ORIENTATION_LANDSCAPE:
          setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
          break;
      }
      mViewModel.toggleLayoutMode().subscribe();
    }

    if (id == R.id.action_toggle_demo) {
      if (mViewModel.currentViewMode() == ViewMode.CHARTING) {
        mViewModel.setViewMode(ViewMode.DEMO);
      } else {
        mViewModel.setViewMode(ViewMode.CHARTING);
      }
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void setOverlay(String key) {
    mLayerSubject.onNext(key);
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
        startActivity(new Intent(this, DriveActivity.class));
        break;
      case R.id.nav_pregnancies:
        startActivityForResult(new Intent(this, PregnancyListActivity.class), RequestCode.PREGNANCY_LIST.ordinal());
        break;
      case R.id.nav_training:
        startActivity(new Intent(this, TrainingActivity.class));
        break;
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
