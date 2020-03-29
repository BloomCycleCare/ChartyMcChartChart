package com.bloomcyclecare.cmcc.ui.pregnancy.detail;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.base.Optional;
import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class PregnancyDetailActivity extends AppCompatActivity {

  public enum Extras {
    PREGNANCY, CYCLE_INDEX
  }

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private TextView mTestDateValueView;
  private TextView mDueDateValueView;

  private PregnancyDetailViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pregnancy_detail);

    mTestDateValueView = findViewById(R.id.tv_test_value);
    mDueDateValueView = findViewById(R.id.tv_due_date_value);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Pregnancy");

    Intent intent = getIntent();
    Pregnancy pregnancy = Parcels.unwrap(intent.getParcelableExtra(Pregnancy.class.getCanonicalName()));

    mViewModel = ViewModelProviders.of(this).get(PregnancyDetailViewModel.class);
    mDisposables.add(RxView.clicks(mDueDateValueView).subscribe(o -> onDueDateClick()));

    mViewModel.init(pregnancy);
    mViewModel.viewState().observe(this, this::render);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_pregnancy_detail, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;

      case R.id.action_save:
        mDisposables.add(mViewModel.onSave().subscribe(
            this::finish, t -> Timber.e(t, "Error saving updates")));
        return true;

      default:
        Timber.w("Sipping unknown action");
    }
    return super.onOptionsItemSelected(item);
  }

  private void onDueDateClick() {
    Timber.d("Handling due date click");
    mDisposables.add(mViewModel.currentState().toSingle().subscribe(currentState -> {
      LocalDate date = Optional.fromNullable(currentState.pregnancy.dueDate).or(LocalDate.now());
      DatePickerDialog dialog = new DatePickerDialog(this, (d, year, month, day) -> {
        LocalDate dueDate = new LocalDate(year, month + 1, day);
        Timber.d("Registering due date update");
        mViewModel.onNewDueDate(dueDate);
      }, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
      dialog.setTitle("Start Date");
      dialog.getDatePicker().setMinDate(currentState.pregnancy.positiveTestDate.toDate().getTime());
      dialog.show();
    }));
  }

  private void render(PregnancyDetailViewModel.ViewState viewState) {
    Timber.d("Rendering new ViewState");
    Pregnancy pregnancy = viewState.pregnancy;
    mTestDateValueView.setText(DateUtil.toUiStr(pregnancy.positiveTestDate));
    if (pregnancy.dueDate != null) {
      String dueDateStr = DateUtil.toUiStr(pregnancy.dueDate);
      if (!mDueDateValueView.getText().toString().equals(dueDateStr)) {
        Timber.d("Updating due date UI");
        mDueDateValueView.setText(dueDateStr);
      }
    }
  }
}
