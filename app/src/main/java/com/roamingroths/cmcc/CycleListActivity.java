package com.roamingroths.cmcc;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.DatePicker;

import com.google.firebase.auth.FirebaseAuth;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;

import java.util.Calendar;
import java.util.Date;

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
}
