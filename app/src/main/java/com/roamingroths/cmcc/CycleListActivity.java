package com.roamingroths.cmcc;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.Callbacks;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.Calendar;

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

    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    mCycleAdapter = new CycleAdapter(this, this, userId);

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
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {
          @Override
          public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {

          }
        });
        datePickerDialog.setTitle("Select cycle start");
        datePickerDialog.setMaxDate(cal);
        datePickerDialog.show(getFragmentManager(), "datepickerdialog");
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
