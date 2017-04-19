package com.roamingroths.cmcc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

public class ObservationDetailActivity extends AppCompatActivity {

  private CollapsingToolbarLayout mToolbarLayout;
  private int mIndex;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_observation_detail);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    mToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);

    Intent intentThatStartedThisActivity = getIntent();
    if (intentThatStartedThisActivity != null
        && intentThatStartedThisActivity.hasExtra(Intent.EXTRA_INDEX)) {
      mIndex = intentThatStartedThisActivity.getIntExtra(Intent.EXTRA_INDEX, -1);
      mToolbarLayout.setTitle("Item #" + mIndex);
    }

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Context context = ObservationDetailActivity.this;
        Intent startModifyActivity = new Intent(context, ObservationModifyActivity.class);
        startModifyActivity.putExtra(Intent.EXTRA_INDEX, mIndex);
        startActivity(startModifyActivity);
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_observation_detail, menu);
    return true;
  }
}
