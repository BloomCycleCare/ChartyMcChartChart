package com.bloomcyclecare.cmcc.ui.training;

import android.os.Bundle;
import android.view.MenuItem;

import com.bloomcyclecare.cmcc.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

public class TrainingActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_training);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    TrainingViewModel viewModel = ViewModelProviders.of(this).get(TrainingViewModel.class);
    viewModel.viewState().observe(this, this::render);

    viewModel.transitionToFragment(new ExerciseListFragment());
  }

  private void render(TrainingViewModel.ViewState viewState) {
    getSupportActionBar().setTitle(viewState.title());
    getSupportActionBar().setSubtitle(viewState.subtitle());

    if (viewState.fragmentTransition().isPresent()) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.fragment_container, viewState.fragmentTransition().get())
          .addToBackStack(null)
          .commit();
      getSupportFragmentManager().executePendingTransactions();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
