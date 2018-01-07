package com.roamingroths.cmcc.ui.entry.list;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.AppState;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.ui.entry.detail.EntrySaveResult;
import com.roamingroths.cmcc.ui.init.UserInitActivity;
import com.roamingroths.cmcc.ui.print.PrintChartActivity;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;
import com.roamingroths.cmcc.utils.FileUtil;
import com.roamingroths.cmcc.utils.GsonUtil;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
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

  private final Subject<String> mLayerSubject;
  private EntryListPageAdapter mPageAdapter;
  private CycleProvider mCycleProvider;

  public ChartEntryListActivity() {
    mLayerSubject = BehaviorSubject.create();
  }

  public Observable<String> layerStream() {
    return mLayerSubject;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_entry_list);

    mNavView = (NavigationView) findViewById(R.id.nav_view);
    // Set the "My Chart" item as selected
    mNavView.setNavigationItemSelectedListener(this);

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    View navHeaderView = mNavView.getHeaderView(0);
    TextView drawerTitleView = navHeaderView.findViewById(R.id.drawer_title);
    drawerTitleView.setText(user.getDisplayName());
    TextView drawerSubtitleView = navHeaderView.findViewById(R.id.drawer_subtitle);
    drawerSubtitleView.setText(user.getEmail());

    mToolbar = (Toolbar) findViewById(R.id.app_bar);
    setTitle("Current Cycle");
    setSupportActionBar(mToolbar);

    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mDrawerToggle = new ActionBarDrawerToggle(
        this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    mDrawerLayout.addDrawerListener(mDrawerToggle);
    mDrawerToggle.syncState();

    mErrorView = (TextView) findViewById(R.id.refresh_error);
    mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
    mViewPager = (ViewPager) findViewById(R.id.view_pager);

    mCycleProvider = MyApplication.getProviders().forCycle();
    mPageAdapter = new EntryListPageAdapter(getSupportFragmentManager(), MyApplication.getProviders().forChartEntry());
    mPageAdapter.initialize(FirebaseAuth.getInstance().getCurrentUser(), mCycleProvider);
    mViewPager.setAdapter(mPageAdapter);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageScrollStateChanged(int state) {
      }

      @Override
      public void onPageSelected(int position) {
        setTitle(position == 0 ? "Current Cycle" : position + " Cycles Ago");
      }
    });

    showList();
  }

  @Override
  protected void onResume() {
    mNavView.setCheckedItem(R.id.nav_my_chart);
    super.onResume();
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
      EntrySaveResult result = data.getParcelableExtra(EntrySaveResult.class.getName());
      if (DEBUG) Log.v(TAG, "Received cycleToShow:" + result.cycleToShow + " in result");
      mViewPager.setCurrentItem(mPageAdapter.onResult(result));
    }
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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

  private void updateOverlay(String key, String val) {
    Log.i(TAG, "Overlay key:" + key + " val:" + val);
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
      /*EntryListFragment fragment = (EntryListFragment) mPageAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
      CycleEntryProvider provider =
          new CycleEntryProvider(mCycleProvider, new ChartEntryProvider(FirebaseDatabase.getInstance(), MyApplication.getCryptoUtil()));

      Observable<ChartEntryList> lists = provider.getChartEntryLists(FirebaseAuth.getInstance().getCurrentUser(), Preferences.fromShared(this));
      ChartPrinter.create(this, lists).print().subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<PrintJob>() {
        @Override
        public void accept(PrintJob printJob) throws Exception {
          if (DEBUG) Log.v(TAG, "Printing done");
        }
      });*/
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
              mPageAdapter.shutdown(mViewPager);
              MyApplication.runUpdate(mCycleProvider.dropCycles(FirebaseAuth.getInstance().getCurrentUser())).subscribe(new Action() {
                @Override
                public void run() throws Exception {
                  Intent intent = new Intent(ChartEntryListActivity.this, UserInitActivity.class);
                  startActivity(intent);
                  dialog.dismiss();
                }
              });
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
      AppState.create(mCycleProvider)
          .subscribe(new Consumer<AppState>() {
            @Override
            public void accept(AppState appState) throws Exception {
              String json = GsonUtil.getGsonInstance().toJson(appState);

              FileUtil.shareAppState(appState, activity);
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
            }
          }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
              Log.w("PrintChartActivity", "Could not create AppState", throwable);
            }
          });
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
    super.onDestroy();
    log("onDestroy");
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
      case R.id.nav_settings:
        Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
        startActivity(startSettingsActivity);
        return true;
    }
    // TODO: check items

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  private void log(String message) {
    Log.v(ChartEntryListActivity.class.getName(), message);
  }
}
