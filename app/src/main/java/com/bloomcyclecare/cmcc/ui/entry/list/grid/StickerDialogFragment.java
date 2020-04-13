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
import com.bloomcyclecare.cmcc.data.models.Sticker;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import io.reactivex.disposables.CompositeDisposable;

public class StickerDialogFragment extends DialogFragment {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

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
        .map(sticker -> {
          switch (sticker) {
            case GREY:
              return R.drawable.sticker_grey;
            case RED:
              return R.drawable.sticker_red;
            case WHITE:
              return R.drawable.sticker_white;
            case WHITE_BABY:
              return R.drawable.sticker_white_baby;
            case YELLOW:
              return R.drawable.sticker_yellow;
            case YELLOW_BABY:
              return R.drawable.sticker_yellow_baby;
            case GREEN:
              return R.drawable.sticker_green;
            case GREEN_BABY:
              return R.drawable.sticker_green_baby;
            default:
              throw new IllegalStateException();
          }
        })
        .subscribe(resourceId -> {
          stickerTextView.setBackground(getContext().getDrawable(resourceId));
        }));

    Spinner textSpinner = view.findViewById(R.id.text_selector_spinner);
    ArrayAdapter<CharSequence> textAdapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.sticker_text, android.R.layout.simple_spinner_item);
    textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    textSpinner.setAdapter(textAdapter);
    mDisposables.add(RxAdapterView.itemSelections(textSpinner)
        .map(index -> {
          if (index == 0) {
            return Optional.<String>empty();
          }
          return Optional.of(getContext().getResources().getStringArray(R.array.sticker_text)[index]);
        })
        .subscribe(text -> stickerTextView.setText(text.orElse(""))));

    Button mButtonCancel = view.findViewById(R.id.button_cancel);
    mButtonCancel.setOnClickListener(v -> dismiss());
    Button mButtonConfirm = view.findViewById(R.id.button_confirm);
    mButtonConfirm.setOnClickListener(v -> dismiss());

    return view;
  }
}
