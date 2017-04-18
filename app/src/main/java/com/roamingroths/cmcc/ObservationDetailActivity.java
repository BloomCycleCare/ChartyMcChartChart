package com.roamingroths.cmcc;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.roamingroths.cmcc.R;

public class ObservationDetailActivity extends AppCompatActivity {

  private CollapsingToolbarLayout mToolbarLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_observation_detail);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
    mToolbarLayout.setTitle("Foo");

    Intent intentThatStartedThisActivity = getIntent();
    if (intentThatStartedThisActivity != null) {
      if (intentThatStartedThisActivity.hasExtra(Intent.EXTRA_INDEX)) {
        int index = intentThatStartedThisActivity.getIntExtra(Intent.EXTRA_INDEX, -1);
        mToolbarLayout.setTitle("Item #" + index);
      }
    }

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });
  }
}
