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
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.DateUtil;

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

    mFab = (FloatingActionButton) findViewById(R.id.fab);
    mFab.setOnClickListener(new View.OnClickListener() {
      Toast futureEntryToast;
      @Override
      public void onClick(View view) {
        LocalDate now = DateUtil.now();
        LocalDate nextEntryDate = mChartEntryAdapter.getNextEntryDate();
        if (nextEntryDate.isAfter(now)) {
          if (futureEntryToast != null) {
            futureEntryToast.cancel();
          }
          futureEntryToast = Toast.makeText(
              ChartEntryListActivity.this, "Cannot add future entries!", Toast.LENGTH_SHORT);
          futureEntryToast.show();
        } else {
          Intent createChartEntry =
              new Intent(ChartEntryListActivity.this, ChartEntryModifyActivity.class);
          fillExtrasForModifyActivity(createChartEntry, mChartEntryAdapter.getNextEntryDate());
          startActivityForResult(
              createChartEntry, ChartEntryModifyActivity.CREATE_REQUEST);
        }
      }
    });

    getSupportActionBar().setTitle("Cycle #1");

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

      Intent intentThatStartedThisActivity = getIntent();
      if (intentThatStartedThisActivity != null
          && intentThatStartedThisActivity.hasExtra(Extras.CYCLE_ID)
          && intentThatStartedThisActivity.hasExtra(Extras.CYCLE_START_DATE_STR)) {
        String cycleId = intentThatStartedThisActivity.getStringExtra(Extras.CYCLE_ID);
        String startDateStr =
            intentThatStartedThisActivity.getStringExtra(Extras.CYCLE_START_DATE_STR);
        attachAdapterToCycle(cycleId, DateUtil.fromWireStr(startDateStr));
      } else {
        DataStore.getCurrentCycleId(user.getUid(), new DataStore.Callback<String>() {
          @Override
          public void acceptData(final String cycleId) {
            DataStore.getCycleStartDate(cycleId, new DataStore.Callback<LocalDate>() {
              @Override
              public void acceptData(LocalDate cycleStartDate) {
                attachAdapterToCycle(cycleId, cycleStartDate);
              }

              @Override
              public void handleNotFound() {
                throw new IllegalStateException("Could not find start date for cycle: " + cycleId);
              }

              @Override
              public void handleError(DatabaseError error) {
                error.toException().printStackTrace();
              }
            });
          }

          @Override
          public void handleNotFound() {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                ChartEntryListActivity.this, new DatePickerDialog.OnDateSetListener() {
              @Override
              public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                LocalDate cycleStartDate = new LocalDate(year, month + 1, dayOfMonth);
                String cycleId = DataStore.createEmptyCycle(user.getUid(), cycleStartDate, true);
                attachAdapterToCycle(cycleId, cycleStartDate);
              }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
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
    intent.putExtra(Extras.CYCLE_ID, mChartEntryAdapter.getCycleId());
    intent.putExtra(Extras.ENTRY_DATE_STR, entryDateStr);

    String cycleStartDateStr =
        DateUtil.toWireStr(mChartEntryAdapter.getCycleStartDate());
    intent.putExtra(Extras.CYCLE_START_DATE_STR, cycleStartDateStr);

    return intent;
  }

  private void attachAdapterToCycle(String cycleId, LocalDate cycleStartDate) {
    mChartEntryAdapter.attachToCycle(cycleId, cycleStartDate);
    mProgressBar.setVisibility(View.INVISIBLE);
  }

  private void detachAdapterFromCycle() {
    mChartEntryAdapter.detachFromCycle();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mRecyclerView.scrollToPosition(0);
  }

  @Override
  protected void onPause() {
    super.onPause();
    //detachAdapterFromCycle();
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
}
