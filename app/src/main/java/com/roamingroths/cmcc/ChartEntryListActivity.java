package com.roamingroths.cmcc;

import android.content.Context;
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

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.util.Calendar;

public class ChartEntryListActivity extends AppCompatActivity
    implements ChartEntryAdapter.OnClickHandler, ChartEntryAdapter.OnItemAddedHandler {

  public static final int RC_SIGN_IN = 1;

  private TextView mErrorView;
  private ProgressBar mProgressBar;
  private FloatingActionButton mFab;

  private RecyclerView mRecyclerView;
  private ChartEntryAdapter mChartEntryAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Log.v("ChartEntryListActivity", "onCreate: start");

    mErrorView = (TextView) findViewById(R.id.refresh_error);
    mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

    mChartEntryAdapter = new ChartEntryAdapter(this, this, this);

    mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_entry);
    boolean shouldReverseLayout = false;
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, shouldReverseLayout);
    mRecyclerView.setLayoutManager(layoutManager);
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mChartEntryAdapter);

    // Init Firebase stuff
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      Log.d(ChartEntryListActivity.class.getName(), "No existing user");
      startActivityForResult(
          AuthUI.getInstance().createSignInIntentBuilder()
              .setProviders(AuthUI.GOOGLE_PROVIDER)
              .setIsSmartLockEnabled(false)
              .build(),
          RC_SIGN_IN);
    } else {
      // Find the current cycle
      mProgressBar.setVisibility(View.VISIBLE);

      Intent intentThatStartedThisActivity = getIntent();
      if (intentThatStartedThisActivity != null
          && intentThatStartedThisActivity.hasExtra(Cycle.class.getName())) {
        Log.v("ChartEntryListActivity", "Using Cycle from Intent");
        Cycle cycle = intentThatStartedThisActivity.getParcelableExtra(Cycle.class.getName());
        attachAdapterToCycle(cycle);
      } else {
        Log.v("ChartEntryListActivity", "Looking up current cycle from DB.");
        DataStore.getCurrentCycle(user.getUid(), new Callbacks.Callback<Cycle>() {
          @Override
          public void acceptData(Cycle cycle) {
            Log.v("ChartEntryListActivity", "Received current cycle from DB.");
            attachAdapterToCycle(cycle);
          }

          @Override
          public void handleNotFound() {
            Log.v("ChartEntryListActivity", "Prompting for start of first cycle.");
            Calendar cal = Calendar.getInstance();
            DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                  @Override
                  public void onDateSet(
                      DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                    LocalDate cycleStartDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    Log.v("ChartEntryListActivity",
                        "Starting new cycle on " + cycleStartDate.toString());
                    Cycle cycle = DataStore.createCycle(
                        ChartEntryListActivity.this, user.getUid(), cycleStartDate, null);
                    attachAdapterToCycle(cycle);
                  }
                });
            datePickerDialog.setTitle("First day of current cycle");
            datePickerDialog.setMaxDate(Calendar.getInstance());
            datePickerDialog.show(getFragmentManager(), "datepickerdialog");
          }

          @Override
          public void handleError(DatabaseError error) {
            // TODO: Improve
            showError();
            error.toException().printStackTrace();
          }
        });
      }
    }
    Log.v("ChartEntryListActivity", "onCreate: start");
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case ChartEntryModifyActivity.CREATE_REQUEST:
        switch (resultCode) {
          case ChartEntryModifyActivity.OK_RESPONSE:
            mRecyclerView.scrollToPosition(0);
            break;
        }
        break;
      case ChartEntryModifyActivity.MODIFY_REQUEST:
        switch (resultCode) {
          case ChartEntryModifyActivity.OK_RESPONSE:
            break;
        }
        break;
    }
  }

  private Intent fillExtrasForModifyActivity(Intent intent, LocalDate entryDate) {
    String entryDateStr = DateUtil.toWireStr(entryDate);
    return fillExtrasForModifyActivity(intent, entryDateStr);
  }

  private Intent fillExtrasForModifyActivity(Intent intent, String entryDateStr) {
    intent.putExtra(Extras.ENTRY_DATE_STR, entryDateStr);
    intent.putExtra(Cycle.class.getName(), mChartEntryAdapter.getCycle());
    return intent;
  }

  private void attachAdapterToCycle(Cycle cycle) {
    getSupportActionBar().setTitle("Cycle starting " + cycle.startDateStr);
    Log.v("ChartEntryListActivity", "Attaching to cycle starting " + cycle.startDateStr);
    mChartEntryAdapter.attachToCycle(cycle);
    mProgressBar.setVisibility(View.INVISIBLE);
    Log.v("ChartEntryListActivity", "Attached to cycle starting " + cycle.startDateStr);
  }

  private void detachAdapterFromCycle() {
    mChartEntryAdapter.detachFromCycle();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.v("ChartEntryListActivity", "onResume");
    mRecyclerView.scrollToPosition(0);
  }

  @Override
  protected void onPause() {
    super.onPause();
    //detachAdapterFromCycle();
    Log.v("ChartEntryListActivity", "onPause");
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
    Intent intent = new Intent(context, destinationClass);
    fillExtrasForModifyActivity(intent, entry.getDateStr());
    startActivityForResult(intent, ChartEntryModifyActivity.MODIFY_REQUEST);
  }

  private void showError() {
    mErrorView.setVisibility(View.VISIBLE);
    mRecyclerView.setVisibility(View.INVISIBLE);
  }

  private void showList() {
    mErrorView.setVisibility(View.INVISIBLE);
    mRecyclerView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onItemAdded(ChartEntry entry, int index) {
    mRecyclerView.scrollToPosition(index);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.v("ChartEntryListActivity", "onDestroy");
  }
}
