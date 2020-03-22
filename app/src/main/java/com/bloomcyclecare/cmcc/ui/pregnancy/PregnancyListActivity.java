package com.bloomcyclecare.cmcc.ui.pregnancy;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.utils.SimpleArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

public class PregnancyListActivity extends AppCompatActivity {

  private SimpleArrayAdapter<PregnancyListViewModel.PregnancyViewModel, ViewHolder> mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pregnancy_list);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Pregnancies");

    ListView pregnanciesView = findViewById(R.id.lv_pregnancies);
    mAdapter = new SimpleArrayAdapter<>(this, R.layout.list_item_pregnancy, ViewHolder::new, c -> {});
    pregnanciesView.setAdapter(mAdapter);

    ViewModelProviders.of(this)
        .get(PregnancyListViewModel.class)
        .viewState()
        .observe(this, this::render);
  }

  private void render(PregnancyListViewModel.ViewState viewState) {
    mAdapter.updateData(viewState.viewModels);
  }

  private static class ViewHolder extends SimpleArrayAdapter.SimpleViewHolder<PregnancyListViewModel.PregnancyViewModel> {

    final TextView infoTextView;

    ViewHolder(View view) {
      super(view);
      infoTextView = view.findViewById(R.id.tv_pregnancy_info);
    }

    @Override
    protected void updateUI(PregnancyListViewModel.PregnancyViewModel data) {
      infoTextView.setText(data.getInfo());
    }
  }
}
