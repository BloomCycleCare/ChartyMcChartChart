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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.Sticker;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import io.reactivex.disposables.CompositeDisposable;

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

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_select_sticker, container);

    TextView stickerTextView = view.findViewById(R.id.sticker);

    Spinner stickerSpinner = view.findViewById(R.id.sticker_selector_spinner);
    ArrayAdapter<CharSequence> stickerAdapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.sticker, android.R.layout.simple_spinner_item);
    stickerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    stickerSpinner.setAdapter(stickerAdapter);
    mDisposables.add(RxAdapterView.itemSelections(stickerSpinner)
        .map(index -> Sticker.values()[index])
        .subscribe(sticker -> {
          int resourceId;
          switch (sticker) {
            case GREY:
              resourceId = R.drawable.sticker_grey;
              break;
            case RED:
              resourceId = R.drawable.sticker_red;
              break;
            case WHITE:
              resourceId = R.drawable.sticker_white;
              break;
            case WHITE_BABY:
              resourceId = R.drawable.sticker_white_baby;
              break;
            case YELLOW:
              resourceId = R.drawable.sticker_yellow;
              break;
            case YELLOW_BABY:
              resourceId = R.drawable.sticker_yellow_baby;
              break;
            case GREEN:
              resourceId = R.drawable.sticker_green;
              break;
            case GREEN_BABY:
              resourceId = R.drawable.sticker_green_baby;
              break;
            default:
              throw new IllegalStateException();
          }
          stickerTextView.setBackground(getContext().getDrawable(resourceId));
          selection.sticker = sticker;
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
        .subscribe(text -> {
          selection.text = text;
          stickerTextView.setText(text);
        }));

    Button mButtonCancel = view.findViewById(R.id.button_cancel);
    mButtonCancel.setOnClickListener(v -> {
      selectionConsumer.accept(selection);
      dismiss();
    });
    Button mButtonConfirm = view.findViewById(R.id.button_confirm);
    mButtonConfirm.setOnClickListener(v -> {
      dismiss();
    });

    return view;
  }
}
