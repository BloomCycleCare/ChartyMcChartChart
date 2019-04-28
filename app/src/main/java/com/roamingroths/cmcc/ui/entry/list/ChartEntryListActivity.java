package com.roamingroths.cmcc.ui.entry.list;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.logic.profile.Profile;
import com.roamingroths.cmcc.providers.CycleProvider;
import com.roamingroths.cmcc.providers.ProfileProvider;
import com.roamingroths.cmcc.ui.appointments.AppointmentListActivity;
import com.roamingroths.cmcc.ui.entry.EntrySaveResult;
import com.roamingroths.cmcc.ui.init.UserInitActivity;
import com.roamingroths.cmcc.ui.print.PrintChartActivity;
import com.roamingroths.cmcc.ui.profile.ProfileActivity;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.UpdateHandle;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.io.File;
import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;

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

  private Single<CycleProvider> mCycleProvider;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private final Subject<String> mLayerSubject = BehaviorSubject.create();
  private SingleSubject<EntryListPageAdapter> mPageAdapter = SingleSubject.create();

  public Observable<String> layerStream() {
    return mLayerSubject;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_list);

    mCycleProvider = MyApplication.cycleProvider().cache();

    mNavView = findViewById(R.id.nav_view);
    // Set the "My Chart" item as selected
    mNavView.setNavigationItemSelectedListener(this);

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    View navHeaderView = mNavView.getHeaderView(0);
    final TextView drawerTitleView = navHeaderView.findViewById(R.id.drawer_title);
    final TextView drawerSubtitleView = navHeaderView.findViewById(R.id.drawer_subtitle);
    drawerSubtitleView.setText(Objects.requireNonNull(user).getEmail());

    mToolbar = findViewById(R.id.app_bar);
    setTitle("Current Cycle");
    setSupportActionBar(mToolbar);

    mDrawerLayout = findViewById(R.id.drawer_layout);
    mDrawerToggle = new ActionBarDrawerToggle(
        this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    mDrawerLayout.addDrawerListener(mDrawerToggle);
    mDrawerToggle.syncState();

    mErrorView = findViewById(R.id.refresh_error);
    mProgressBar = findViewById(R.id.progress_bar);
    mViewPager = findViewById(R.id.view_pager);

    mDisposables.add(MyApplication.profileProvider()
        .flatMap(ProfileProvider.getProfileFn(ChartEntryListActivity.this))
        .subscribe(new Consumer<Profile>() {
          @Override
          public void accept(Profile profile) throws Exception {
            drawerTitleView.setText(profile.mPreferredName);
          }
        }));

    mDisposables.add(MyApplication.chartEntryProvider()
        .map(EntryListPageAdapter.create(getSupportFragmentManager()))
        .flatMap(EntryListPageAdapter.initializeFn(MyApplication.getCurrentUser().toSingle(), mCycleProvider))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(entryListPageAdapter -> {
          mPageAdapter.onSuccess(entryListPageAdapter);
          mViewPager.setAdapter(entryListPageAdapter);
          mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageScrollStateChanged(int state) {}

            @Override
            public void onPageSelected(int position) {
              boolean viewingFirstCycle = position == entryListPageAdapter.getCount() - 1;
              if (viewingFirstCycle) {
                showFab();
              } else {
                hideFab();
              }

              String title;
              if (position == 0) {
                title = "Current Cycle";
              } else {
                title = position + " Cycles Ago";
              }
              setTitle(title);

              entryListPageAdapter.onPageActive(position);
            }
          });
          showList();
        }));

    mNewCycleFab = findViewById(R.id.fab_new_cycle);
    mNewCycleFab.setOnClickListener(__ -> {
      EntryListPageAdapter adapter = (EntryListPageAdapter) mViewPager.getAdapter();
      Cycle cycle = adapter.getCycle(mViewPager.getCurrentItem());
      final LocalDate endDate = cycle.startDate.minusDays(1);
      DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
          new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(
                DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
              LocalDate startDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
              AlertDialog.Builder builder = new AlertDialog.Builder(ChartEntryListActivity.this);
              builder.setTitle("Create Cycle");
              builder.setIcon(R.drawable.ic_save_black_24dp);
              builder.setMessage(
                  String.format("Create cycle starting %s (%d days)?",
                  DateUtil.toPrintUiStr(startDate),
                  Days.daysBetween(startDate, endDate).getDays()));
              builder.setPositiveButton("Create", (dialog, which) -> {
                mCycleProvider.flatMap(provider -> provider.createCycle(user, startDate, endDate))
                    .flatMapCompletable(result -> mPageAdapter.flatMapCompletable(adapter -> {
                      mViewPager.setCurrentItem(adapter.onResult(result));
                      return Completable.complete();
                    }))
                    .doOnSubscribe(__ -> dialog.dismiss())
                    .subscribe();
              });
              builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
              builder.show();
            }
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
      final EntrySaveResult result = data.getParcelableExtra(EntrySaveResult.class.getName());
      if (DEBUG) Log.v(TAG, "Received cycleToShow:" + result.cycleToShow + " in result");
      mDisposables.add(mPageAdapter
          .subscribe(adapter -> mViewPager.setCurrentItem(adapter.onResult(result))));
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
      new AlertDialog.Builder(this)
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
      return true;
    }
    if (id == R.id.action_export) {
      Log.v("PrintChartActivity", "Begin export");
      final ChartEntryListActivity activity = this;
      mDisposables.add(MyApplication.appStateProvider()
          .flatMap(provider -> provider.fetchAsJson(ChartEntryListActivity.this))
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
  public void clearOverlay() {
    mLayerSubject.onNext("");
    Log.i(TAG, "Overlay: clear");
  }

  @Override
  public void showProgress() {
    mProgressBar.setVisibility(View.VISIBLE);
    mViewPager.setVisibility(View.INVISIBLE);
    mErrorView.setVisibility(View.INVISIBLE);
  }

  @Override
  public void showError(String message) {
    mProgressBar.setVisibility(View.INVISIBLE);
    mViewPager.setVisibility(View.INVISIBLE);
    mErrorView.setVisibility(View.VISIBLE);
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
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    switch (item.getItemId()) {
      case R.id.nav_appointments:
        startActivity(new Intent(this, AppointmentListActivity.class));
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
