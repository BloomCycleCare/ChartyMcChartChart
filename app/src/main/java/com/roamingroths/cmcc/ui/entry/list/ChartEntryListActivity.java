package com.roamingroths.cmcc.ui.entry.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.CycleListActivity;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;

public class ChartEntryListActivity extends AppCompatActivity implements EntryListView {

  public static final int RC_SIGN_IN = 1;

  private TextView mErrorView;
  private ProgressBar mProgressBar;
  private FrameLayout mFragmentContainer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    log("onCreate");

    mErrorView = (TextView) findViewById(R.id.refresh_error);
    mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
    mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);

    Intent intentThatStartedThisActivity = Preconditions.checkNotNull(getIntent());
    Preconditions.checkState(intentThatStartedThisActivity.hasExtra(Cycle.class.getName()));
    Cycle cycle = intentThatStartedThisActivity.getParcelableExtra(Cycle.class.getName());

    getSupportActionBar().setTitle("Cycle starting " + cycle.startDateStr);

    showList();

    // Avoid creating overlapping fragments
    if (savedInstanceState == null) {
      Fragment fragment = new EntryListFragment();
      fragment.setArguments(getIntent().getExtras());

      getSupportFragmentManager().beginTransaction()
          .add(R.id.fragment_container, fragment).commit();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    /*if (data != null) {
      if (data.hasExtra(EntryContainer.class.getName())) {
        EntryContainer container = data.getParcelableExtra(EntryContainer.class.getName());
        mChartEntryAdapter.updateContainer(container);
      }
      if (data.hasExtra(Cycle.class.getName())) {
        Cycle cycleFromResponse = data.getParcelableExtra(Cycle.class.getName());
        if (!mChartEntryAdapter.getCycle().equals(cycleFromResponse)) {
          swapCycles(cycleFromResponse);
        }
      }
    }
    switch (requestCode) {
      default:
        Log.w(ChartEntryListActivity.class.getName(), "Unknown request code: " + requestCode);
        break;
    }*/
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
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

    if (id == R.id.action_list_cycles) {
      Intent startCycleList = new Intent(this, CycleListActivity.class);
      finish();
      startActivity(startCycleList);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /*private void swapCycles(Cycle newCycle) {
    log("Switching to cycle: " + newCycle.id);
    showProgress();

    getSupportActionBar().setTitle("Cycle starting " + newCycle.startDateStr);

    mChartEntryAdapter = new ChartEntryAdapter(
        getApplicationContext(), newCycle, this, mDb, mCycleProvider);
    mRecyclerView.setAdapter(mChartEntryAdapter);

    mChartEntryAdapter.initialize(mCycleProvider)
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action() {
          @Override
          public void run() throws Exception {
            log("Adapter initialized");
            showList();
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable t) throws Exception {
            Log.e(ChartEntryListActivity.class.getSimpleName(), "Error initializing", t);
          }
        });
  }*/

  @Override
  public void showList() {
    mFragmentContainer.setVisibility(View.VISIBLE);
    mErrorView.setVisibility(View.INVISIBLE);
    mProgressBar.setVisibility(View.INVISIBLE);
  }

  @Override
  public void showProgress() {
    mProgressBar.setVisibility(View.VISIBLE);
    mFragmentContainer.setVisibility(View.INVISIBLE);
    mErrorView.setVisibility(View.INVISIBLE);
  }

  @Override
  public void showError(String message) {
    mProgressBar.setVisibility(View.INVISIBLE);
    mFragmentContainer.setVisibility(View.INVISIBLE);
    mErrorView.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    log("onDestroy");
  }

  private void log(String message) {
    Log.v(ChartEntryListActivity.class.getName(), message);
  }
}
