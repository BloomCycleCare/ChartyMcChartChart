package com.roamingroths.cmcc;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.Callbacks;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class CycleListActivity extends AppCompatActivity
    implements CycleAdapter.OnClickHandler {

  private RecyclerView mRecyclerView;
  private CycleAdapter mCycleAdapter;
  private String mUserId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cycle_list);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    //setSupportActionBar(toolbar);

    setTitle("Your Cycles");

    mCycleAdapter = new CycleAdapter(this, this);

    mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_cycle_entry);
    boolean shouldReverseLayout = false;
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, shouldReverseLayout);
    mRecyclerView.setLayoutManager(layoutManager);
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mCycleAdapter);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            CycleListActivity.this, new DatePickerDialog.OnDateSetListener() {
          @Override
          public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            Date date = new Date(year, month, dayOfMonth);
            //mCycleAdapter.addCycle(new Cycle(date, ImmutableList.<ChartEntry>of()));
          }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
      }
    });

    mUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
  }

  @Override
  protected void onResume() {
    super.onResume();
    DataStore.attachCycleListener(mCycleAdapter, mUserId);
  }

  @Override
  protected void onPause() {
    super.onPause();
    DataStore.detachCycleListener(mCycleAdapter, mUserId);
  }

  @Override
  public void onClick(Cycle cycle, int itemNum) {
    Context context = this;
    Intent intent = new Intent(context, ChartEntryListActivity.class);
    intent.putExtra(Cycle.class.getName(), cycle);
    startActivity(intent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_cycle_list, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_drop_cycles) {
      new AlertDialog.Builder(this)
          //set message, title, and icon
          .setTitle("Delete All Cycles?")
          .setMessage("This is permanent and cannot be undone!")
          .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {
              DataStore.dropCycles(new Callbacks.HaltingCallback<Void>() {
                @Override
                public void acceptData(Void data) {
                  Intent intent = new Intent(CycleListActivity.this, SplashActivity.class);
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
    return super.onOptionsItemSelected(item);
  }
}
