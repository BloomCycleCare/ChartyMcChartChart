package com.roamingroths.cmcc.ui.entry.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.ChartEntryAdapter;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.EntryContainer;
import com.roamingroths.cmcc.ui.CycleListActivity;
import com.roamingroths.cmcc.ui.entry.detail.EntryDetailActivity;
import com.roamingroths.cmcc.ui.settings.SettingsActivity;
import com.roamingroths.cmcc.utils.Callbacks;

public class ChartEntryListActivity extends AppCompatActivity implements
    ChartEntryAdapter.OnClickHandler {

  public static final int RC_SIGN_IN = 1;

  private TextView mErrorView;
  private ProgressBar mProgressBar;
  private FloatingActionButton mFab;

  private FirebaseDatabase mDb;
  private RecyclerView mRecyclerView;
  private ChartEntryAdapter mChartEntryAdapter;
  private CycleProvider mCycleProvider;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mErrorView = (TextView) findViewById(R.id.refresh_error);
    mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

    mDb = FirebaseDatabase.getInstance();
    mCycleProvider = CycleProvider.forDb(mDb);

    Intent intentThatStartedThisActivity = Preconditions.checkNotNull(getIntent());
    Preconditions.checkState(intentThatStartedThisActivity.hasExtra(Cycle.class.getName()));
    Cycle cycle = intentThatStartedThisActivity.getParcelableExtra(Cycle.class.getName());

    getSupportActionBar().setTitle("Cycle starting " + cycle.startDateStr);

    Callbacks.Callback<Void> adapterInitialzationCallback = new Callbacks.HaltingCallback<Void>() {
      @Override
      public void acceptData(Void data) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            showList();
          }
        });
      }
    };

    mChartEntryAdapter = new ChartEntryAdapter(
        getApplicationContext(), cycle, this, mDb, mCycleProvider, adapterInitialzationCallback);

    mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_entry);
    boolean shouldReverseLayout = false;
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, shouldReverseLayout);
    mRecyclerView.setLayoutManager(layoutManager);
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mChartEntryAdapter);


    showProgress();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (data != null && data.hasExtra(Cycle.class.getName())) {
      Cycle cycleFromResponse = data.getParcelableExtra(Cycle.class.getName());
      if (!mChartEntryAdapter.getCycle().equals(cycleFromResponse)) {
        swapCycles(cycleFromResponse);
      }
    }
    switch (requestCode) {
      default:
        Log.w(ChartEntryListActivity.class.getName(), "Unknown request code: " + requestCode);
        break;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    log("onResume");

    mChartEntryAdapter.attachListener();
    mRecyclerView.scrollToPosition(0);
  }

  @Override
  protected void onPause() {
    super.onPause();
    log("onPause");

    mChartEntryAdapter.detachListener();
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

  @Override
  public void onClick(EntryContainer container, int index) {
    startActivityForResult(
        mChartEntryAdapter.getIntentForModification(container, index),
        EntryDetailActivity.MODIFY_REQUEST);
  }

  private void swapCycles(Cycle newCycle) {
    log("Switching to cycle: " + newCycle.id);
    showProgress();
    Callbacks.Callback<Void> adapterInitialzationCallback = new Callbacks.HaltingCallback<Void>() {
      @Override
      public void acceptData(Void data) {
        log("Adapter initialized");
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            showList();
          }
        });
      }
    };
    mChartEntryAdapter = new ChartEntryAdapter(
        getApplicationContext(), newCycle, this, mDb, mCycleProvider, adapterInitialzationCallback);
    mRecyclerView.setAdapter(mChartEntryAdapter);
  }

  private void showList() {
    mRecyclerView.setVisibility(View.VISIBLE);
    mErrorView.setVisibility(View.INVISIBLE);
    mProgressBar.setVisibility(View.INVISIBLE);
  }

  private void showProgress() {
    mProgressBar.setVisibility(View.VISIBLE);
    mRecyclerView.setVisibility(View.INVISIBLE);
    mErrorView.setVisibility(View.INVISIBLE);
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
