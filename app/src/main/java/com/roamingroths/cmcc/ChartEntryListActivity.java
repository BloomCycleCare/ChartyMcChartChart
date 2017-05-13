package com.roamingroths.cmcc;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.common.collect.ImmutableList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;

import java.util.Calendar;
import java.util.UUID;

public class ChartEntryListActivity extends AppCompatActivity
    implements ChartEntryAdapter.OnClickHandler {

  public static final int RC_SIGN_IN = 1;

  private TextView mErrorView;
  private ProgressBar mProgressBar;

  private RecyclerView mRecyclerView;
  private ChartEntryAdapter mChartEntryAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mErrorView = (TextView) findViewById(R.id.refresh_error);
    mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

    mChartEntryAdapter = new ChartEntryAdapter(this, this);

    mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_entry);
    boolean shouldReverseLayout = false;
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, shouldReverseLayout);
    mRecyclerView.setLayoutManager(layoutManager);
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mChartEntryAdapter);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent createChartEntry =
            new Intent(ChartEntryListActivity.this, ChartEntryModifyActivity.class);
        startActivityForResult(
            createChartEntry, ChartEntryModifyActivity.CREATE_REQUEST);
      }
    });

    Intent intentThatStartedThisActivity = getIntent();
    if (intentThatStartedThisActivity != null) {
      if (intentThatStartedThisActivity.hasExtra(Cycle.class.getName())) {
        Cycle cycle =
            intentThatStartedThisActivity.getParcelableExtra(Cycle.class.getName());
        mChartEntryAdapter.installCycle(cycle);
      }
    }

    getSupportActionBar().setTitle("Cycle #1");
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case ChartEntryModifyActivity.CREATE_REQUEST:
        switch (resultCode) {
          case ChartEntryModifyActivity.OK_RESPONSE:
            ChartEntry newEntry = data.getParcelableExtra(ChartEntry.class.getName());
            try {
              DataStore.putChartEntry(this, mChartEntryAdapter.getCycleId(), newEntry);
              mChartEntryAdapter.addEntry(newEntry);
            } catch (Exception e) {
              e.printStackTrace();
            }
            break;
        }
        break;
      case ChartEntryModifyActivity.MODIFY_REQUEST:
        switch (resultCode) {
          case ChartEntryModifyActivity.OK_RESPONSE:
            ChartEntry newEntry = data.getParcelableExtra(ChartEntry.class.getName());
            if (!data.hasExtra(Intent.EXTRA_INDEX)) {
              throw new IllegalStateException("ChartEntry index missing from Intent");
            }
            int index = data.getIntExtra(Intent.EXTRA_INDEX, -1);
            mChartEntryAdapter.updateEntry(index, newEntry);
            break;
        }
        break;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Init Firebase stuff
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      startActivityForResult(
          AuthUI.getInstance().createSignInIntentBuilder()
              .setProviders(AuthUI.GOOGLE_PROVIDER)
              .setIsSmartLockEnabled(false)
              .build(),
          RC_SIGN_IN);
    } else {
      // Find the current cycle
      mProgressBar.setVisibility(View.VISIBLE);
      DataStore.getCurrentCycle(this, user.getUid(), new DataStore.Callback<Cycle>() {
        @Override
        public void acceptData(Cycle data) {
          mProgressBar.setVisibility(View.INVISIBLE);
          mChartEntryAdapter.installCycle(data);
        }

        @Override
        public void handleNotFound() {
          Calendar cal = Calendar.getInstance();
          DatePickerDialog datePickerDialog = new DatePickerDialog(
              ChartEntryListActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
              Calendar cal = Calendar.getInstance();
              cal.set(year, month, dayOfMonth);
              mProgressBar.setVisibility(View.INVISIBLE);
              mChartEntryAdapter.installCycle(DataStore.createCycle(
                  user.getUid(), new Cycle(
                      UUID.randomUUID().toString(), cal.getTime(), ImmutableList.<ChartEntry>of())));
            }
          }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
          datePickerDialog.show();
        }

        @Override
        public void handleError(DatabaseError error) {
          error.toException().printStackTrace();
        }
      });
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
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
      startActivity(startCycleList);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(ChartEntry entry, int index) {
    Context context = this;
    Class destinationClass = ChartEntryModifyActivity.class;
    Intent intentToStartDetailActivity = new Intent(context, destinationClass);
    intentToStartDetailActivity.putExtra(ChartEntry.class.getName(), entry);
    intentToStartDetailActivity.putExtra(Intent.EXTRA_INDEX, index);
    startActivityForResult(intentToStartDetailActivity, ChartEntryModifyActivity.MODIFY_REQUEST);
  }

  private void showError() {
    mErrorView.setVisibility(View.VISIBLE);
    mRecyclerView.setVisibility(View.INVISIBLE);
  }

  private void showList() {
    mErrorView.setVisibility(View.INVISIBLE);
    mRecyclerView.setVisibility(View.VISIBLE);
  }
}
