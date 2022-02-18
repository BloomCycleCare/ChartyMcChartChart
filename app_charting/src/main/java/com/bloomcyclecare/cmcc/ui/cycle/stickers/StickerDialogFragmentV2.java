package com.bloomcyclecare.cmcc.ui.cycle.stickers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.SelectionChecker;
import com.bloomcyclecare.cmcc.utils.StickerUtil;
import com.google.common.collect.ImmutableMap;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class StickerDialogFragmentV2 extends DialogFragment {

  enum Args {
    SELECTION_CONTEXT,
    PREVIOUS_SELECTION,
    VIEW_MODE,
    CAN_SELECT_YELLOW_STAMPS
  }

  private final Subject<StickerSelection> selectionSubject = BehaviorSubject.create();
  private final Consumer<SelectionChecker.Result> resultConsumer;
  private final LocalDate entryDate;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private StickerDialogViewModel mViewModel;

  public StickerDialogFragmentV2(Consumer<SelectionChecker.Result> resultConsumer, LocalDate entryDate) {
    super();
    this.resultConsumer = resultConsumer;
    this.entryDate = entryDate;
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

  private static class StickerBackground {
    private final int background;
    private final int selectedBackground;

    StickerBackground(int background, int selectedBackground) {
      this.background = background;
      this.selectedBackground = selectedBackground;
    }

    public int getBackground(boolean selected) {
      return selected ? selectedBackground : background;
    }
  }

  private static final ImmutableMap<Sticker, StickerBackground> STICKER_BACKGROUNDS =
      ImmutableMap.<Sticker, StickerBackground>builder()
          .put(Sticker.RED, new StickerBackground(R.drawable.sticker_red, R.drawable.sticker_red_selected))
          .put(Sticker.WHITE_BABY, new StickerBackground(R.drawable.sticker_white_baby, R.drawable.sticker_white_baby_selected))
          .put(Sticker.GREEN, new StickerBackground(R.drawable.sticker_green, R.drawable.sticker_green_selected))
          .put(Sticker.GREEN_BABY, new StickerBackground(R.drawable.sticker_green_baby, R.drawable.sticker_green_baby_selected))
          .put(Sticker.YELLOW, new StickerBackground(R.drawable.sticker_yellow, R.drawable.sticker_yellow_selected))
          .put(Sticker.YELLOW_BABY, new StickerBackground(R.drawable.sticker_yellow_baby, R.drawable.sticker_yellow_baby_selected))
          .build();

  private static class StickerView {
    public final View view;
    private final StickerBackground background;

    private StickerView(View view, StickerBackground background) {
      this.view = view;
      this.background = background;
    }

    public void setBackground(boolean selected) {
      view.setBackground(
          AppCompatResources.getDrawable(view.getContext(), background.getBackground(selected)));
    }
  }


  private TextView createSticker() {
    Context context = requireContext();
    TextView view = new TextView(context);
    view.setGravity(Gravity.CENTER);
    view.setTextAppearance(requireContext(), R.style.Base_TextAppearance_AppCompat_Large);
    view.setTypeface(view.getTypeface(), Typeface.BOLD);

    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    layoutParams.setMargins(20, 20, 20, 20);
    layoutParams.height = (int) context.getResources().getDimension(R.dimen.sticker_height);
    layoutParams.width = (int) context.getResources().getDimension(R.dimen.sticker_width);
    view.setLayoutParams(layoutParams);

    return view;
  }

  @NonNull
  private StickerView createColorSticker(Sticker sticker) {
    TextView view = createSticker();
    view.setOnClickListener(v -> mViewModel.onStickerClick(sticker));
    return new StickerView(view, STICKER_BACKGROUNDS.get(sticker));
  }

  private StickerView createTextSticker(StickerText text) {
    TextView view = createSticker();
    view.setOnClickListener(v -> mViewModel.onStickerTextClick(text));

    view.setId(Character.valueOf(text.value).hashCode());
    view.setText(String.valueOf(text));

    return new StickerView(view, new StickerBackground(R.drawable.sticker_white, R.drawable.sticker_white_selected));
  }

  private ImmutableMap<StickerText, StickerView> createStickerTextViews(View view) {
    ImmutableMap.Builder<StickerText, StickerView> viewMap = ImmutableMap.builder();
    viewMap.put(StickerText.ONE, createTextSticker(StickerText.ONE));
    viewMap.put(StickerText.TWO, createTextSticker(StickerText.TWO));
    viewMap.put(StickerText.THREE, createTextSticker(StickerText.THREE));
    viewMap.put(StickerText.P, createTextSticker(StickerText.P));
    return viewMap.build();
  }

  private ImmutableMap<Sticker, StickerView> createStickerViews() {
    ImmutableMap.Builder<Sticker, StickerView> viewMap = ImmutableMap.builder();
    viewMap.put(Sticker.RED, createColorSticker(Sticker.RED));
    viewMap.put(Sticker.WHITE_BABY, createColorSticker(Sticker.WHITE_BABY));
    viewMap.put(Sticker.GREEN, createColorSticker(Sticker.GREEN));
    viewMap.put(Sticker.GREEN_BABY, createColorSticker(Sticker.GREEN_BABY));
    viewMap.put(Sticker.YELLOW, createColorSticker(Sticker.YELLOW));
    viewMap.put(Sticker.YELLOW_BABY, createColorSticker(Sticker.YELLOW_BABY));
    return viewMap.build();
  }

  private void populateStickers(
      View view,
      boolean yellowStickersEnabled,
      boolean showStickerTextInitially,
      ImmutableMap<Sticker, StickerView> stickerViews,
      ImmutableMap<StickerText, StickerView> stickerTextViews) {
    LinearLayout lastRow = view.findViewById(R.id.sticker_row_3);


    lastRow.addView(stickerTextViews.get(StickerText.ONE).view);
    lastRow.addView(stickerTextViews.get(StickerText.TWO).view);
    lastRow.addView(stickerTextViews.get(StickerText.THREE).view);
    lastRow.addView(stickerTextViews.get(StickerText.P).view);

    View showStickersButton = view.findViewById(R.id.show_text_stickers_button);
    if (showStickerTextInitially) {
      showStickersButton.setVisibility(GONE);
      lastRow.setVisibility(VISIBLE);
    } else {
      showStickersButton.setOnClickListener(v -> {
        showStickersButton.setVisibility(GONE);
        lastRow.setVisibility(VISIBLE);
      });
      lastRow.setVisibility(GONE);
    }

    if (yellowStickersEnabled) {
      view.findViewById(R.id.sticker_row_2).setVisibility(VISIBLE);

      LinearLayout firstRow = view.findViewById(R.id.sticker_row_1);
      firstRow.addView(stickerViews.get(Sticker.RED).view);
      firstRow.addView(stickerViews.get(Sticker.GREEN).view);
      firstRow.addView(stickerViews.get(Sticker.YELLOW).view);

      LinearLayout secondRow = view.findViewById(R.id.sticker_row_2);
      secondRow.addView(stickerViews.get(Sticker.WHITE_BABY).view);
      secondRow.addView(stickerViews.get(Sticker.GREEN_BABY).view);
      secondRow.addView(stickerViews.get(Sticker.YELLOW_BABY).view);
    } else {
      view.findViewById(R.id.sticker_row_2).setVisibility(GONE);

      LinearLayout firstRow = view.findViewById(R.id.sticker_row_1);
      firstRow.addView(stickerViews.get(Sticker.RED).view);
      firstRow.addView(stickerViews.get(Sticker.GREEN).view);
      firstRow.addView(stickerViews.get(Sticker.GREEN_BABY).view);
      firstRow.addView(stickerViews.get(Sticker.WHITE_BABY).view);
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_select_sticker_v2, container);

    mInfoTextView = view.findViewById(R.id.tv_info);
    mHintTextView = view.findViewById(R.id.tv_hint);
    mShowHintButton = view.findViewById(R.id.button_show_hint);
    mShowHintButton.setOnClickListener(v -> {
      showHint();
    });
    clearResultView();

    ViewMode viewMode = ViewMode.values()[requireArguments().getInt(Args.VIEW_MODE.name())];

    Button mButtonCancel = view.findViewById(R.id.button_cancel);
    mButtonCancel.setOnClickListener(v -> dismiss());

    CycleRenderer.StickerSelectionContext selectionContext = Parcels.unwrap(
        requireArguments().getParcelable(Args.SELECTION_CONTEXT.name()));

    Optional<StickerSelection> previousSelection = Optional.ofNullable(
        Parcels.unwrap(requireArguments().getParcelable(Args.PREVIOUS_SELECTION.name())));
    Timber.d("Previous selection: %s", previousSelection);

    StickerDialogViewModel.Factory factory = new StickerDialogViewModel.Factory(
        requireActivity(), viewMode, null, entryDate, selectionContext,
        previousSelection.orElse(null));
    mViewModel = new ViewModelProvider(this, factory).get(StickerDialogViewModel.class);

    boolean yellowStickersEnabled = requireArguments().getBoolean(Args.CAN_SELECT_YELLOW_STAMPS.name());
    Timber.d("Yellow stickers enabled: %b", yellowStickersEnabled);
    List<String> adapterValues = new ArrayList<>();
    for (String option : getResources().getStringArray(R.array.sticker)) {
      if (!option.startsWith("Yellow") || yellowStickersEnabled) {
        adapterValues.add(option);
      } else {
        Timber.v("Suppressing option %s", option);
      }
    }

    ImmutableMap<Sticker, StickerView> stickerViews = createStickerViews();
    ImmutableMap<StickerText, StickerView> stickerTextViews = createStickerTextViews(view);
    boolean showStickerTextInitially = previousSelection.map(s -> s.text != null).orElse(false);
    populateStickers(view, yellowStickersEnabled, showStickerTextInitially, stickerViews, stickerTextViews);

    mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      stickerViews.forEach((sticker, stickerView) -> {
        boolean enable = viewState.stickerSelection().map(s -> s == sticker).orElse(false);
        Timber.v("%s %b", sticker, enable);
        stickerView.setBackground(enable);
      });
      stickerTextViews.forEach((text, stickerView) -> {
        boolean enable = viewState.stickerTextSelection().map(t -> t == text).orElse(false);
        Timber.v("%s %b", text, enable);
        stickerView.setBackground(enable);
      });

      if (viewState.checkerResult().isPresent() && !viewState.checkerResult().get().ok()) {
        renderIncorrectResult(viewState.checkerResult().get());
      } else {
        clearResultView();
      }
    });

    Button mButtonConfirm = view.findViewById(R.id.button_confirm);
    mButtonConfirm.setOnClickListener(v -> {
      mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
        StickerSelection selection = StickerSelection.create(
            viewState.stickerSelection().orElse(null),
            viewState.stickerTextSelection().orElse(null));
        if (selection.isEmpty()) {
          renderEmptySelect();
          return;
        }
        resultConsumer.accept(SelectionChecker.create(selectionContext).check(selection));
        dismiss();
      });
    });

    return view;
  }

  private void showHint() {
    mHintTextView.setVisibility(VISIBLE);
    mShowHintButton.setVisibility(GONE);
  }

  private void renderEmptySelect() {
    mInfoTextView.setVisibility(VISIBLE);
    mInfoTextView.setText("Please make a selection");
  }

  private void clearResultView() {
    mInfoTextView.setVisibility(GONE);
    mHintTextView.setVisibility(GONE);
    mShowHintButton.setVisibility(GONE);
  }

  private void renderIncorrectResult(SelectionChecker.Result result) {
    mInfoTextView.setVisibility(VISIBLE);
    mInfoTextView.setText(String.format("Incorrect selection\n\nReason: %s\n\n%s",
        Optional.ofNullable(result.reason).orElse(""),
        Optional.ofNullable(result.explanation).orElse("")));
    mHintTextView.setText(String.format("Hint: %s",
        Optional.ofNullable(result.hint).orElse("")));

    mShowHintButton.setVisibility(VISIBLE);
  }
}
