package com.bloomcyclecare.cmcc.ui.pregnancy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.entry.list.ChartEntryListActivity;
import com.bloomcyclecare.cmcc.utils.SimpleArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProviders;
import timber.log.Timber;

public class PregnancyListActivity extends AppCompatActivity {

  private SimpleArrayAdapter<PregnancyListViewModel.PregnancyViewModel, ViewHolder> mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pregnancy_list);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Pregnancies");

    ListView pregnanciesView = findViewById(R.id.lv_pregnancies);
    mAdapter = new SimpleArrayAdapter<>(this, R.layout.list_item_pregnancy, v -> new ViewHolder(v, this::navigateToPregnancy), c -> {});
    pregnanciesView.setAdapter(mAdapter);

    ViewModelProviders.of(this)
        .get(PregnancyListViewModel.class)
        .viewState()
        .observe(this, this::render);
  }

  private void render(PregnancyListViewModel.ViewState viewState) {
    mAdapter.updateData(viewState.viewModels);
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

    ViewHolder(View view, Consumer<Integer> onClick) {
      super(view);
      infoTextView = view.findViewById(R.id.tv_pregnancy_info);
      infoTextView.setOnClickListener(v -> {
        onClick.accept(getCurrent().cycleAscIndex);
      });
    }

    @Override
    protected void updateUI(PregnancyListViewModel.PregnancyViewModel data) {
      infoTextView.setText(data.getInfo());
    }
  }
}
