package com.roamingroths.cmcc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;

import java.util.Date;

public class ChartEntryListActivity extends AppCompatActivity implements
        ChartEntryAdapter.ChartEntryAdapterOnClickHandler {

  public static final int RC_SIGN_IN = 1;

  private static final String ANONYMOUS = "anonymous";

  private RecyclerView mRecyclerView;
  private ChartEntryAdapter mChartEntryAdapter;
  private String mUsername = ANONYMOUS;
  private int mIndex;
  private Date mStartDate;

  // Firebase stuff
  private FirebaseAuth mFirebaseAuth;
  private FirebaseAuth.AuthStateListener mAuthStateListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

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

    mIndex = 1;
    Intent intentThatStartedThisActivity = getIntent();
    if (intentThatStartedThisActivity != null) {
      if (intentThatStartedThisActivity.hasExtra(Intent.EXTRA_INDEX)) {
        mIndex = intentThatStartedThisActivity.getIntExtra(Intent.EXTRA_INDEX, -1);
      }
      if (intentThatStartedThisActivity.hasExtra(Cycle.class.getName())) {
        Cycle cycle =
            intentThatStartedThisActivity.getParcelableExtra(Cycle.class.getName());
        mStartDate = cycle.firstDay;
        mChartEntryAdapter.installCycle(cycle);
      }
    }

    // Init Firebase stuff
    mFirebaseAuth = FirebaseAuth.getInstance();

    getSupportActionBar().setTitle("Cycle #" + mIndex);

    mAuthStateListener = new FirebaseAuth.AuthStateListener() {
      @Override
      public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
          // user signed in
          onSignedInInit(user);
        } else {
          // user signed out
          onSignedOutCleanup();
          startActivityForResult(
              AuthUI.getInstance().createSignInIntentBuilder()
                  .setProviders(AuthUI.GOOGLE_PROVIDER)
                  .setIsSmartLockEnabled(false)
                  .build(),
              RC_SIGN_IN);
        }
      }
    };
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case ChartEntryModifyActivity.CREATE_REQUEST:
        switch (resultCode) {
          case ChartEntryModifyActivity.OK_RESPONSE:
            ChartEntry newEntry = data.getParcelableExtra(ChartEntry.class.getName());
            mChartEntryAdapter.addEntry(newEntry);
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

  private void onSignedInInit(FirebaseUser user) {
    mUsername = user.getDisplayName();
    Toast.makeText(this, "Username: " + mUsername, Toast.LENGTH_LONG).show();
  }

  private void onSignedOutCleanup() {

  }

  @Override
  protected void onResume() {
    super.onResume();
    mFirebaseAuth.addAuthStateListener(mAuthStateListener);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
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
      startCycleList.putExtra(Intent.EXTRA_INDEX, mIndex);
      if (mStartDate != null) {
        Cycle cycle = new Cycle(mStartDate, mChartEntryAdapter.getCurrentEntries());
        startCycleList.putExtra(Cycle.class.getName(), cycle);
      }
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
}
