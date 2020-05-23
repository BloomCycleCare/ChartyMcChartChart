package com.bloomcyclecare.cmcc.ui.cycle;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.logic.chart.SelectionChecker;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import org.parceler.Parcels;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

import static android.view.View.GONE;

public class StickerDialogFragment extends DialogFragment {

  public enum Args {
    EXPECTED_SELECTION,
    PREVIOUS_SELECTION
  }

  private final Subject<StickerSelection> selectionSubject = BehaviorSubject.create();
  private final Consumer<SelectionChecker.Result> resultConsumer;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  public StickerDialogFragment(Consumer<SelectionChecker.Result> resultConsumer) {
    super();
    this.resultConsumer = resultConsumer;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return super.onCreateDialog(savedInstanceState);
  }

  private TextView mHintTextView;
  private TextView mInfoTextView;
  private Button mShowHintButton;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_select_sticker, container);

    mInfoTextView = view.findViewById(R.id.tv_info);
    mHintTextView = view.findViewById(R.id.tv_hint);
    mShowHintButton = view.findViewById(R.id.button_show_hint);
    mShowHintButton.setOnClickListener(v -> {
      showHint();
    });
    clearResultView();

    Optional<StickerSelection> previousSelection = Optional.ofNullable(
        Parcels.unwrap(requireArguments().getParcelable(Args.PREVIOUS_SELECTION.name())));
    Timber.d("Previous selection: %s", previousSelection);

    TextView stickerTextView = view.findViewById(R.id.sticker);

    Spinner stickerSpinner = view.findViewById(R.id.sticker_selector_spinner);
    ArrayAdapter<CharSequence> stickerAdapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.sticker, android.R.layout.simple_spinner_item);
    stickerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    stickerSpinner.setAdapter(stickerAdapter);
    if (previousSelection.isPresent()) {
      stickerSpinner.setSelection(previousSelection.get().sticker.ordinal());
    }

    Spinner textSpinner = view.findViewById(R.id.text_selector_spinner);
    ArrayAdapter<CharSequence> textAdapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.sticker_text, android.R.layout.simple_spinner_item);
    textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    textSpinner.setAdapter(textAdapter);
    if (previousSelection.isPresent()) {
      textSpinner.setSelection(
          previousSelection.get().text != null ? previousSelection.get().text.ordinal() : 0);
    }

    Observable.combineLatest(
        RxAdapterView.itemSelections(stickerSpinner)
            .map(index -> StickerSelection.Sticker.values()[index]),
        RxAdapterView.itemSelections(textSpinner)
            .map(index -> {
              if (index == 0) {
                return Optional.<StickerSelection.Text>empty();
              }
              String spinnerText = requireContext().getResources().getStringArray(R.array.sticker_text)[index];
              StickerSelection.Text t = StickerSelection.Text.fromString(spinnerText);
              if (t == null) {
                Timber.w("Unexpected null value for sticker text: %s", spinnerText);
              }
              return Optional.ofNullable(t);
            }),
        (s, t) -> StickerSelection.create(s, t.orElse(null)))
        .subscribe(selectionSubject);

    Button mButtonCancel = view.findViewById(R.id.button_cancel);
    mButtonCancel.setOnClickListener(v -> dismiss());

    Optional<StickerSelection> expectedSelection = Optional.ofNullable(
        Parcels.unwrap(requireArguments().getParcelable(Args.EXPECTED_SELECTION.name())));
    Timber.d("Expected selection: %s", expectedSelection);

    mDisposables.add(selectionSubject.distinctUntilChanged().subscribe(selection -> {
      stickerTextView.setBackground(requireContext().getDrawable(selection.sticker.resourceId));
      stickerTextView.setText(selection.text != null ? String.valueOf(selection.text.value) : "");
      if (previousSelection.isPresent() && expectedSelection.isPresent()) {
        SelectionChecker.Result result = SelectionChecker.check(previousSelection.get(), expectedSelection.get());
        if (!result.ok() && selection.equals(previousSelection.get())) {
          renderIncorrectResult(result);
          return;
        }
      }
      clearResultView();
    }));


    Button mButtonConfirm = view.findViewById(R.id.button_confirm);
    mButtonConfirm.setOnClickListener(v -> {
      StickerSelection selection = selectionSubject.blockingFirst();
      if (selection.isEmpty()) {
        renderEmptySelect();
        return;
      }
      resultConsumer.accept(SelectionChecker.check(selection, expectedSelection.get()));
      dismiss();
    });

    return view;
  }

  private void showHint() {
    mHintTextView.setVisibility(View.VISIBLE);
    mShowHintButton.setVisibility(GONE);
  }

  private void renderEmptySelect() {
    mInfoTextView.setVisibility(View.VISIBLE);
    mInfoTextView.setText("Please make a selection");
  }

  private void clearResultView() {
    mInfoTextView.setVisibility(GONE);
    mHintTextView.setVisibility(GONE);
    mShowHintButton.setVisibility(GONE);
  }

  private void renderIncorrectResult(SelectionChecker.Result result) {
    mInfoTextView.setVisibility(View.VISIBLE);
    mInfoTextView.setText(String.format("Incorrect selection\n\nReason: %s",
        result.reason.map(Enum::name).orElse("")));
    mHintTextView.setText(String.format("Hint: %s",
        result.hint.map(Enum::name).orElse("")));

    mShowHintButton.setVisibility(View.VISIBLE);
  }
}
