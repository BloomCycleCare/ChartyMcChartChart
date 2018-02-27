package com.roamingroths.cmcc.ui.profile;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.goals.GoalTemplate;

public class GoalDetailActivity extends AppCompatActivity implements GoalTemplateAdapter.OnClickHandler {

  private EditText mEditText;
  private RecyclerView mGoalOptions;
  private GoalTemplateAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_goal_detail);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle("");
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    mAdapter = new GoalTemplateAdapter(this, this);
    mGoalOptions = findViewById(R.id.goal_types);
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    mGoalOptions.setLayoutManager(layoutManager);
    mGoalOptions.setHasFixedSize(false);
    mGoalOptions.setAdapter(mAdapter);

    mEditText = findViewById(R.id.goal_edit_text);
    mEditText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        mAdapter.updateItems(s.toString());
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_goal_detail, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == android.R.id.home) {
      onBackPressed();
      return true;
    }

    return true;
  }

  @Override
  public void onClick(int index) {
    GoalTemplate template = mAdapter.getTemplate(index);
    String newText = template.toString().replaceAll("\\.\\.\\.", "");
    if (!newText.equals(mEditText.getText())) {
      mEditText.setText(newText);
      mEditText.setSelection(newText.length());
    }
  }
}
