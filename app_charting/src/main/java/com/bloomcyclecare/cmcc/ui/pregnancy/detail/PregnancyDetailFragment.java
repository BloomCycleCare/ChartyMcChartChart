package com.bloomcyclecare.cmcc.ui.pregnancy.detail;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.jakewharton.rxbinding2.view.RxView;

import org.joda.time.LocalDate;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class PregnancyDetailFragment extends Fragment {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private TextView mTestDateValueView;
  private TextView mDueDateValueView;
  private TextView mDeliveryDateValueView;

  private PregnancyDetailViewModel mViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_pregnancy_detail, container, false);

    mTestDateValueView = view.findViewById(R.id.tv_test_value);
    mDueDateValueView = view.findViewById(R.id.tv_due_date_value);
    mDeliveryDateValueView = view.findViewById(R.id.tv_delivery_date_value);

    mDisposables.add(RxView.clicks(mDueDateValueView).subscribe(o -> onDueDateClick()));
    mDisposables.add(RxView.clicks(mDeliveryDateValueView).subscribe(o -> onDeliveryDateClick()));

    mViewModel = new ViewModelProvider(this).get(PregnancyDetailViewModel.class);
    // TODO: replace init with factory
    mViewModel.init(PregnancyDetailFragmentArgs.fromBundle(requireArguments()).getPregnancy().pregnancy);
    mViewModel.viewState().observe(getViewLifecycleOwner(), this::render);

    return view;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_pregnancy_detail, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_save:
        mDisposables.add(mViewModel.onSave().subscribe(() -> {
          NavHostFragment.findNavController(this).navigate(
              PregnancyDetailFragmentDirections.actionSavePregnancyUpdates());
        }, t -> Timber.e(t, "Error saving updates")));
        return true;

      case R.id.action_view_cycle:
        LocalDate dateToFocus = mViewModel.currentState().blockingGet().pregnancy.positiveTestDate.plusDays(1);
        NavHostFragment.findNavController(this).navigate(
            PregnancyDetailFragmentDirections.actionViewCycle()
                .setDateToFocus(DateUtil.toWireStr(dateToFocus))
                .setViewMode(ViewMode.CHARTING));
        return true;

      default:
        return NavigationUI.onNavDestinationSelected(
            item, NavHostFragment.findNavController(this));
    }
  }

  private void onDueDateClick() {
    Timber.d("Handling due date click");
    mDisposables.add(mViewModel.currentState().toSingle().subscribe(currentState -> {
      LocalDate date = Optional.ofNullable(currentState.pregnancy.dueDate)
          .orElse(currentState.pregnancy.positiveTestDate.plusMonths(9));
      DatePickerDialog dialog = new DatePickerDialog(requireContext(), (d, year, month, day) -> {
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
      DatePickerDialog dialog = new DatePickerDialog(requireContext(), (d, year, month, day) -> {
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
}
