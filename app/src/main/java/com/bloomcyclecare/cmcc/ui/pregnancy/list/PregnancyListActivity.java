package com.bloomcyclecare.cmcc.ui.pregnancy.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.ui.entry.list.ChartEntryListActivity;
import com.bloomcyclecare.cmcc.ui.pregnancy.detail.PregnancyDetailActivity;
import com.bloomcyclecare.cmcc.utils.RxPrompt;
import com.bloomcyclecare.cmcc.utils.SimpleArrayAdapter;

import org.parceler.Parcels;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProviders;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class PregnancyListActivity extends AppCompatActivity {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private SimpleArrayAdapter<PregnancyListViewModel.PregnancyViewModel, ViewHolder> mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pregnancy_list);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Pregnancies");

    ListView pregnanciesView = findViewById(R.id.lv_pregnancies);
    mAdapter = new SimpleArrayAdapter<>(this, R.layout.list_item_pregnancy, v -> new ViewHolder(v, this::viewDetails), c -> {});
    pregnanciesView.setAdapter(mAdapter);

    ViewModelProviders.of(this)
        .get(PregnancyListViewModel.class)
        .viewState()
        .observe(this, this::render);
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

  private void render(PregnancyListViewModel.ViewState viewState) {
    for (RxPrompt prompt : viewState.prompts) {
      mDisposables.add(prompt.doPrompt(this));
    }
    mAdapter.updateData(viewState.viewModels);
  }

  private void viewDetails(Pregnancy pregnancy) {
    Intent intent = new Intent(this, PregnancyDetailActivity.class);
    intent.putExtra(Pregnancy.class.getCanonicalName(), Parcels.wrap(pregnancy));
    startActivity(intent);
  }

  private void navigateToPregnancy(int cycleIndex) {
    if (cycleIndex < 0) {
      Timber.w("Invalid cycle index");
      return;
    }
    Intent data = new Intent();
    data.putExtra(ChartEntryListActivity.Extras.CYCLE_DESC_INDEX.name(), cycleIndex);
    setResult(RESULT_OK, data);
    finish();
  }

  private static class ViewHolder extends SimpleArrayAdapter.SimpleViewHolder<PregnancyListViewModel.PregnancyViewModel> {

    final TextView infoTextView;

    ViewHolder(View view, Consumer<Pregnancy> onClick) {
      super(view);
      infoTextView = view.findViewById(R.id.tv_pregnancy_info);
      infoTextView.setOnClickListener(v -> {
        onClick.accept(getCurrent().pregnancy);
      });
    }

    @Override
    protected void updateUI(PregnancyListViewModel.PregnancyViewModel data) {
      infoTextView.setText(data.getInfo());
    }
  }
}
