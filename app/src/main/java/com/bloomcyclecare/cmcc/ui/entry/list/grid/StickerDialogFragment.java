package com.bloomcyclecare.cmcc.ui.entry.list.grid;

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
import com.bloomcyclecare.cmcc.logic.training.SelectionChecker;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import io.reactivex.disposables.CompositeDisposable;

import static android.view.View.GONE;

public class StickerDialogFragment extends DialogFragment {

  private final Consumer<StickerSelection> selectionConsumer;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private final StickerSelection selection = new StickerSelection();

  StickerDialogFragment(Consumer<StickerSelection> selectionConsumer) {
    super();
    this.selectionConsumer = selectionConsumer;
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

    TextView stickerTextView = view.findViewById(R.id.sticker);

    Spinner stickerSpinner = view.findViewById(R.id.sticker_selector_spinner);
    ArrayAdapter<CharSequence> stickerAdapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.sticker, android.R.layout.simple_spinner_item);
    stickerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    stickerSpinner.setAdapter(stickerAdapter);
    mDisposables.add(RxAdapterView.itemSelections(stickerSpinner)
        .map(index -> StickerSelection.Sticker.values()[index])
        .subscribe(sticker -> {
          stickerTextView.setBackground(getContext().getDrawable(sticker.resourceId));
          selection.sticker = sticker;
          clearResultView();
        }));

    Spinner textSpinner = view.findViewById(R.id.text_selector_spinner);
    ArrayAdapter<CharSequence> textAdapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.sticker_text, android.R.layout.simple_spinner_item);
    textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    textSpinner.setAdapter(textAdapter);
    mDisposables.add(RxAdapterView.itemSelections(textSpinner)
        .map(index -> {
          if (index == 0) {
            return "";
          }
          return getContext().getResources().getStringArray(R.array.sticker_text)[index];
        })
        .subscribe(str -> {
          selection.text = StickerSelection.Text.fromString(str);
          stickerTextView.setText(str);
          clearResultView();
        }));

    Button mButtonCancel = view.findViewById(R.id.button_cancel);
    mButtonCancel.setOnClickListener(v -> {
      dismiss();
    });

    StickerSelection expected = Parcels.unwrap(getArguments().getParcelable(
        StickerSelection.class.getCanonicalName()));

    Button mButtonConfirm = view.findViewById(R.id.button_confirm);
    mButtonConfirm.setOnClickListener(v -> {
      if (selection.isEmpty()) {
        renderEmptySelect();
        return;
      }
      SelectionChecker.Result result = SelectionChecker.check(selection, expected);
      if (result.ok()) {
        selectionConsumer.accept(selection);
        dismiss();
        return;
      }
      renderIncorrectResult(result);
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
    mInfoTextView.setText(String.format("Incorrect...\nreason: %s",
        result.reason.map(Enum::name).orElse("")));
    mHintTextView.setText(String.format("hint: %s",
        result.hint.map(Enum::name).orElse("")));

    mShowHintButton.setVisibility(View.VISIBLE);
  }
}
