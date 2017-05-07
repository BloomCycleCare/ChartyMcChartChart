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
import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.Observation;
import com.roamingroths.cmcc.utils.CryptoUtil;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ChartEntryListActivity extends AppCompatActivity implements
        ChartEntryAdapter.ChartEntryAdapterOnClickHandler {

  public static final int RC_SIGN_IN = 1;

  private RecyclerView mRecyclerView;
  private ChartEntryAdapter mChartEntryAdapter;
  private FirebaseUser mUser = null;
  private int mIndex;
  private Date mStartDate;

  // Firebase stuff
  private FirebaseAuth mFirebaseAuth;
  private FirebaseAuth.AuthStateListener mAuthStateListener;
  private DatabaseReference mDatabase;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mDatabase = FirebaseDatabase.getInstance().getReference();

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

  private static final String PERSONAL_PRIVATE_KEY_ALIAS = "PersonalPrivateKey";

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case ChartEntryModifyActivity.CREATE_REQUEST:
        switch (resultCode) {
          case ChartEntryModifyActivity.OK_RESPONSE:
            if (data != null) {
              ChartEntry newEntry = data.getParcelableExtra(ChartEntry.class.getName());
              mChartEntryAdapter.addEntry(newEntry);
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

  private void onSignedInInit(final FirebaseUser user) {
    mUser = user;
    try {
      final DatabaseReference currentCycleRef =
          mDatabase.child("users").child(user.getUid()).child("current-cycle");
      currentCycleRef.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
          String currentCycleUuid = (String) dataSnapshot.getValue();
          if (Strings.isNullOrEmpty(currentCycleUuid)) {
            currentCycleUuid = UUID.randomUUID().toString();
            DatabaseReference cycleRef = mDatabase.child("cycles").child(currentCycleUuid);
            cycleRef.child("user").setValue(user.getUid());
            currentCycleRef.setValue(currentCycleUuid);
          }
          DatabaseReference entriesRef =
              mDatabase.child("cycles").child(currentCycleUuid).child("entries");
          entriesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
              String wire = (String) dataSnapshot.getValue();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
          });
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {}
      });
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
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
