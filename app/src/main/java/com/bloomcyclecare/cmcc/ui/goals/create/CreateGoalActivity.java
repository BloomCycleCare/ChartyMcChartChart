package com.bloomcyclecare.cmcc.ui.goals.create;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.logic.goals.GoalModel;
import com.bloomcyclecare.cmcc.mvi.MviView;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding2.widget.RxTextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

public class CreateGoalActivity extends AppCompatActivity implements GoalModelAdapter.OnClickHandler, MviView<CreateGoalIntent, CreateGoalViewState> {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private EditText mEditText;
  private RecyclerView mGoalOptions;
  private GoalModelAdapter mAdapter;
  private CreateGoalViewModel mViewModel;
  private PublishSubject<CreateGoalIntent.SaveGoal> mSaveEvents;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_goal_detail);

    mSaveEvents = PublishSubject.create();

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle("");
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    mAdapter = new GoalModelAdapter(this, this);
    mGoalOptions = findViewById(R.id.goal_types);
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    mGoalOptions.setLayoutManager(layoutManager);
    mGoalOptions.setHasFixedSize(false);
    mGoalOptions.setAdapter(mAdapter);

    mEditText = findViewById(R.id.goal_edit_text);

    mViewModel = ViewModelProviders.of(this, MyApplication.viewModelFactory()).get(CreateGoalViewModel.class);

    mDisposables.add(mViewModel.states().subscribe(this::render));
    mViewModel.processIntents(intents());
  }

  @Override
  protected void onDestroy() {
    mDisposables.dispose();

    super.onDestroy();
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

    if (id == R.id.action_save) {
      mSaveEvents.onNext(CreateGoalIntent.SaveGoal.create(mEditText.getText().toString()));
    }

    return true;
  }

  @Override
  public void onClick(int index) {
    GoalModel template = mAdapter.getTemplate(index);
    String newText = template.toString().replaceAll("\\.\\.\\.", "");
    if (!newText.equals(mEditText.getText())) {
      mEditText.setText(newText);
      mEditText.setSelection(newText.length());
    }
  }

  @Override
  public Observable<CreateGoalIntent> intents() {
    return Observable.merge(initialIntent(), getSuggestionsIntent(), saveGoalIntent());
  }

  private Observable<CreateGoalIntent.InitialIntent> initialIntent() {
    return Observable.just(CreateGoalIntent.InitialIntent.create());
  }

  private Observable<CreateGoalIntent.GetSuggestions> getSuggestionsIntent() {
    return RxTextView.textChanges(mEditText)
        .map(input -> CreateGoalIntent.GetSuggestions.create(input.toString()));
  }

  private Observable<CreateGoalIntent.SaveGoal> saveGoalIntent() {
    return mSaveEvents;
  }

  @Override
  public void render(CreateGoalViewState state) {
    if (state.error() != null) {
      Log.w(CreateGoalActivity.class.getSimpleName(), state.error());
      showMessage("Error");
    }
    if (state.isSaved()) {
      setResult(Activity.RESULT_OK);
      finish();
    }
    if (state.goalModels() != null) {
      mAdapter.updateItems(state.goalModels());
    }
  }

  private void showMessage(String message) {
    View view = findViewById(R.id.goal_detail_layout);
    if (view == null) return;
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
  }
}
