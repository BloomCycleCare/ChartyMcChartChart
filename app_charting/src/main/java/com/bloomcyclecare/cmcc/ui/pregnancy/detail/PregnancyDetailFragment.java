package com.bloomcyclecare.cmcc.ui.pregnancy.detail;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxTextView;

import org.joda.time.LocalDate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class PregnancyDetailFragment extends Fragment {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private TextView mTestDateValueView;
  private TextView mDueDateValueView;
  private TextView mDeliveryDateValueView;

  private View mBreastfeedingGroup;
  private SwitchCompat mBreastfeedingValue;
  private View mBreastfeedingStartGroup;
  private TextView mBreastfeedingStartValue;
  private View mBreastfeedingEndGroup;
  private TextView mBreastfeedingEndValue;
  private EditText mBabyNameValue;
  private TextView mBreastfeedingStats;

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

    mBreastfeedingGroup = view.findViewById(R.id.breastfeeding_group);
    mBreastfeedingValue = view.findViewById(R.id.breastfeeding_value);
    mBreastfeedingStartGroup = view.findViewById(R.id.breastfeeding_start_group);
    mBreastfeedingStartValue = view.findViewById(R.id.tv_breastfeeding_start_value);
    mBreastfeedingEndGroup = view.findViewById(R.id.breastfeeding_end_group);
    mBreastfeedingEndValue = view.findViewById(R.id.tv_breastfeeding_end_value);
    mBabyNameValue = view.findViewById(R.id.baby_name_value);
    mBreastfeedingStats = view.findViewById(R.id.breastfeeding_stats);

    // Connect views to handlers
    mDisposables.add(RxView.clicks(mDueDateValueView).subscribe(o -> onDueDateClick()));
    mDisposables.add(RxView.clicks(mDeliveryDateValueView).subscribe(o -> onDeliveryDateClick()));
    mDisposables.add(RxView.clicks(mBreastfeedingStartValue).subscribe(o -> onBreastfeedingStartDateClick()));
    mDisposables.add(RxView.clicks(mBreastfeedingEndValue).subscribe(o -> onBreastfeedingEndDateClick()));

    mViewModel = new ViewModelProvider(this, new PregnancyDetailViewModel.Factory(
        requireActivity().getApplication(),
        PregnancyDetailFragmentArgs.fromBundle(requireArguments()).getPregnancy().pregnancy))
        .get(PregnancyDetailViewModel.class);

    AtomicBoolean rendered = new AtomicBoolean();
    mViewModel.viewState().observe(getViewLifecycleOwner(), vs -> render(vs, rendered));

    // Connect view events to view model
    mDisposables.add(RxCompoundButton.checkedChanges(mBreastfeedingValue)
        .filter(v -> {
          if (!rendered.get()) {
            Timber.v("Dropping switch toggle, ViewState not yet rendered");
            return false;
          }
          return true;
        })
        .subscribe(mViewModel::onBreastfeedingToggle));
    mDisposables.add(RxTextView.textChanges(mBabyNameValue)
        .filter(v -> {
          if (!rendered.get()) {
            Timber.v("Dropping name update, ViewState not yet rendered");
            return false;
          }
          return true;
        })
        .map(CharSequence::toString)
        .subscribe(mViewModel::onBabyNameUpdate));

    return view;
  }

  private void render(PregnancyDetailViewModel.ViewState viewState, AtomicBoolean rendered) {
    Timber.d("Rendering new ViewState");
    Pregnancy pregnancy = viewState.pregnancy;
    mTestDateValueView.setText(DateUtil.toNewUiStr(pregnancy.positiveTestDate));
    if (pregnancy.dueDate == null) {
      mDueDateValueView.setText("TBD");
    } else {
      String dueDateStr = DateUtil.toNewUiStr(pregnancy.dueDate);
      if (!mDueDateValueView.getText().toString().equals(dueDateStr)) {
        Timber.d("Updating due date UI");
        mDueDateValueView.setText(dueDateStr);
      }
    }
    if (pregnancy.deliveryDate == null) {
      mDeliveryDateValueView.setText("TBD");
    } else {
      String deliveryDateStr = DateUtil.toNewUiStr(pregnancy.deliveryDate);
      if (!mDeliveryDateValueView.getText().toString().equals(deliveryDateStr)) {
        Timber.d("Updating delivery date UI");
        mDeliveryDateValueView.setText(deliveryDateStr);
      }
    }
    mBreastfeedingGroup.setVisibility(
        viewState.showBreastfeedingSection ? View.VISIBLE : View.GONE);
    mBreastfeedingStartGroup.setVisibility(
        viewState.showBreastfeedingStartDate ? View.VISIBLE : View.GONE);
    mBreastfeedingEndGroup.setVisibility(
        viewState.showBreastfeedingEndDate ? View.VISIBLE : View.GONE);

    if (pregnancy.breastfeedingStartDate == null) {
      mBreastfeedingStartValue.setText("TBD");
    } else {
      String dateStr = DateUtil.toNewUiStr(pregnancy.breastfeedingStartDate);
      if (!mBreastfeedingStartValue.getText().toString().equals(dateStr)) {
        Timber.d("Updating breastfeeding start date UI");
        mBreastfeedingStartValue.setText(dateStr);
      }
    }

    if (pregnancy.breastfeedingEndDate == null) {
      mBreastfeedingEndValue.setText("TBD");
    } else {
      String dateStr = DateUtil.toNewUiStr(pregnancy.breastfeedingEndDate);
      if (!mBreastfeedingEndValue.getText().toString().equals(dateStr)) {
        Timber.d("Updating breastfeeding end date UI");
        mBreastfeedingEndValue.setText(dateStr);
      }
    }

    if (!mBabyNameValue.getText().toString().equals(pregnancy.babyDaybookName)) {
      Timber.d("Updating baby name UI");
      mBabyNameValue.setText(pregnancy.babyDaybookName);
    }

    if (mBreastfeedingValue.isChecked() != viewState.showBreastfeedingStartDate) {
      mBreastfeedingValue.setChecked(viewState.showBreastfeedingStartDate);
    }

    mBreastfeedingStats.setText(viewState.stats);

    if (rendered.compareAndSet(false, true)) {
      Timber.d("Rendered first view state");
    }
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
        mDisposables.add(mViewModel.onSave().observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {
          NavHostFragment.findNavController(this).popBackStack();
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
    showDatePicker(
        "Start Date",
        vs -> Optional.ofNullable(vs.pregnancy.dueDate)
            .orElse(vs.pregnancy.positiveTestDate.plusMonths(9)),
        vs -> vs.pregnancy.positiveTestDate.toDate().getTime(),
        d -> {
          Timber.d("Registering due date update");
          mViewModel.onNewDueDate(d.orElse(null));
        });
  }

  private void onDeliveryDateClick() {
    Timber.d("Handling delivery date click");
    showDatePicker(
        "Due Date",
        vs -> Optional.ofNullable(vs.pregnancy.deliveryDate)
            .orElse(vs.pregnancy.positiveTestDate.plusMonths(9)),
        vs -> vs.pregnancy.positiveTestDate.toDate().getTime(),
        d -> {
          Timber.d("Registering delivery date update");
          mViewModel.onNewDeliveryDate(d.orElse(null));
        });
  }

  private void onBreastfeedingStartDateClick() {
    Timber.d("Handling breastfeeding start date click");
    showDatePicker(
        "Start Date",
        vs -> {
          if (vs.pregnancy.deliveryDate == null) {
            throw new IllegalStateException("Cannot start breastfeeding until after delivery date!");
          }
          return Optional.ofNullable(vs.pregnancy.breastfeedingStartDate).orElse(vs.pregnancy.deliveryDate);
        },
        vs -> {
          if (vs.pregnancy.deliveryDate == null) {
            throw new IllegalStateException("Cannot start breastfeeding until after delivery date!");
          }
          return vs.pregnancy.deliveryDate.toDate().getTime();
        },
        d -> {
          Timber.d("Registering breastfeeding start date update");
          mViewModel.onNewBreastfeedingStartDate(d.orElse(null));
        });
  }

  private void onBreastfeedingEndDateClick() {
    Timber.d("Handling breastfeeding end date click");
    showDatePicker(
        "End Date",
        vs -> {
          if (vs.pregnancy.breastfeedingStartDate == null) {
            throw new IllegalStateException("Cannot end breastfeeding before you've started!");
          }
          return Optional.ofNullable(vs.pregnancy.breastfeedingEndDate).orElse(LocalDate.now());
        },
        vs -> {
          if (vs.pregnancy.breastfeedingStartDate == null) {
            throw new IllegalStateException("Cannot end breastfeeding before you've started!");
          }
          return vs.pregnancy.breastfeedingStartDate.toDate().getTime();
        },
        d -> {
          Timber.d("Registering breastfeeding end date update");
          mViewModel.onNewBreastfeedingEndDate(d.orElse(null));
        });
  }

  private void showDatePicker(
      String title,
      Function<PregnancyDetailViewModel.ViewState, LocalDate> initialValueFn,
      Function<PregnancyDetailViewModel.ViewState, Long> minDateFn,
      Consumer<Optional<LocalDate>> dateConsumer) {
    mDisposables.add(mViewModel.currentState().toSingle().subscribe(currentState -> {
      LocalDate date = initialValueFn.apply(currentState);
      DatePickerDialog dialog = new DatePickerDialog(requireContext(), (d, year, month, day) -> {
        dateConsumer.accept(Optional.of(new LocalDate(year, month + 1, day)));
      }, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
      dialog.setTitle(title);
      dialog.getDatePicker().setMinDate(minDateFn.apply(currentState));
      dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Clear", (dialog1, which) -> {
        dateConsumer.accept(Optional.empty());
      });
      dialog.show();
    }, Timber::e));
  }

}
