package com.bloomcyclecare.cmcc.ui.entry.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class ChartEntryListActivity extends AppCompatActivity
    implements EntryListView, NavigationView.OnNavigationItemSelectedListener {

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    return false;
  }

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
  }

  @Override
  protected void onNewIntent(Intent intent) {
    maybeUpdateCurrentPage(intent);
    super.onNewIntent(intent);
  }

  @Override
  protected void onResume() {
    mNavView.setCheckedItem(R.id.chart_pager);
    super.onResume();
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
}
