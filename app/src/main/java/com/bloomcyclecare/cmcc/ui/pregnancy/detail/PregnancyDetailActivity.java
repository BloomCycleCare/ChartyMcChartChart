package com.bloomcyclecare.cmcc.ui.pregnancy.detail;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.ui.entry.list.ChartEntryListActivity;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.Optional;

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
  private TextView mDeliveryDateValueView;

  private PregnancyDetailViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pregnancy_detail);

    mTestDateValueView = findViewById(R.id.tv_test_value);
    mDueDateValueView = findViewById(R.id.tv_due_date_value);
    mDeliveryDateValueView = findViewById(R.id.tv_delivery_date_value);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Your Pregnancy");

    Intent intent = getIntent();
    Pregnancy pregnancy = Parcels.unwrap(intent.getParcelableExtra(Extras.PREGNANCY.name()));

    mViewModel = ViewModelProviders.of(this).get(PregnancyDetailViewModel.class);
    mDisposables.add(RxView.clicks(mDueDateValueView).subscribe(o -> onDueDateClick()));
    mDisposables.add(RxView.clicks(mDeliveryDateValueView).subscribe(o -> onDeliveryDateClick()));

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

      case R.id.action_view_cycle:
        navigateToPregnancy(getIntent().getIntExtra(Extras.CYCLE_INDEX.name(), -1));
        return true;

      default:
        Timber.w("Sipping unknown action");
    }
    return super.onOptionsItemSelected(item);
  }

  private void onDueDateClick() {
    Timber.d("Handling due date click");
    mDisposables.add(mViewModel.currentState().toSingle().subscribe(currentState -> {
      LocalDate date = Optional.ofNullable(currentState.pregnancy.dueDate)
          .orElse(currentState.pregnancy.positiveTestDate.plusMonths(9));
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

  private void onDeliveryDateClick() {
    Timber.d("Handling delivery date click");
    mDisposables.add(mViewModel.currentState().toSingle().subscribe(currentState -> {
      LocalDate date = Optional.ofNullable(currentState.pregnancy.deliveryDate)
          .orElse(currentState.pregnancy.positiveTestDate.plusMonths(9));
      DatePickerDialog dialog = new DatePickerDialog(this, (d, year, month, day) -> {
        LocalDate dueDate = new LocalDate(year, month + 1, day);
        Timber.d("Registering delivery date update");
        mViewModel.onNewDeliveryDate(dueDate);
      }, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
      dialog.setTitle("Start Date");
      dialog.getDatePicker().setMinDate(currentState.pregnancy.positiveTestDate.toDate().getTime());
      dialog.show();
    }));
  }

  private void render(PregnancyDetailViewModel.ViewState viewState) {
    Timber.d("Rendering new ViewState");
    Pregnancy pregnancy = viewState.pregnancy;
    mTestDateValueView.setText(DateUtil.toNewUiStr(pregnancy.positiveTestDate));
    if (pregnancy.dueDate != null) {
      String dueDateStr = DateUtil.toNewUiStr(pregnancy.dueDate);
      if (!mDueDateValueView.getText().toString().equals(dueDateStr)) {
        Timber.d("Updating due date UI");
        mDueDateValueView.setText(dueDateStr);
      }
    }
    if (pregnancy.deliveryDate != null) {
      String deliveryDateStr = DateUtil.toNewUiStr(pregnancy.deliveryDate);
      if (!mDeliveryDateValueView.getText().toString().equals(deliveryDateStr)) {
        Timber.d("Updating delivery date UI");
        mDeliveryDateValueView.setText(deliveryDateStr);
      }
    }
  }

  private void navigateToPregnancy(int cycleIndex) {
    if (cycleIndex < 0) {
      Timber.w("Invalid cycle index");
      return;
    }
    Intent intent = new Intent(getApplicationContext(), ChartEntryListActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.putExtra(ChartEntryListActivity.Extras.CYCLE_DESC_INDEX.name(), cycleIndex);
    startActivity(intent);
    finish();
  }
}
