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

public class ChartEntryListActivity extends AppCompatActivity implements
    ChartEntryAdapter.ObservationAdapterOnClickHandler {

  public static final int RC_SIGN_IN = 1;

  private static final String ANONYMOUS = "anonymous";

  private RecyclerView mRecyclerView;
  private ChartEntryAdapter mChartEntryAdapter;
  private String mUsername = ANONYMOUS;

  // Firebase stuff
  private FirebaseAuth mFirebaseAuth;
  private FirebaseAuth.AuthStateListener mAuthStateListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Init Firebase stuff
    mFirebaseAuth = FirebaseAuth.getInstance();

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
        mChartEntryAdapter.notifyItemInserted(mChartEntryAdapter.addEntry());
      }
    });

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

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(int itemNum) {
    Context context = this;
    Class destinationClass = ChartEntryModifyActivity.class;
    Intent intentToStartDetailActivity = new Intent(context, destinationClass);
    intentToStartDetailActivity.putExtra(Intent.EXTRA_INDEX, itemNum);
    startActivity(intentToStartDetailActivity);
  }
}
