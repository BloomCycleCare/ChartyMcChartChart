package com.bloomcyclecare.cmcc.ui.cycle.stickers;

import android.app.Activity;
import android.app.Application;
import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.SelectionChecker;

import org.joda.time.LocalDate;

import java.util.Optional;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class StickerDialogViewModel extends AndroidViewModel {

  private final RWStickerSelectionRepo mRepo;

  private final Subject<Optional<Sticker>> mStickerClicks = BehaviorSubject.createDefault(Optional.empty());
  private final Subject<Optional<StickerText>> mStickerTextClicks = BehaviorSubject.createDefault(Optional.empty());
  private final Subject<ViewState> mViewState = BehaviorSubject.create();
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  public StickerDialogViewModel(
      @NonNull Application application,
      LocalDate entryDate,
      ViewMode viewMode,
      @Nullable Exercise exercise,
      CycleRenderer.StickerSelectionContext selectionContext,
      @Nullable StickerSelection previousSelection) {
    super(application);

    if (exercise != null) {
      mRepo = ChartingApp.cast(application).stickerSelectionRepo(exercise);
    } else {
      mRepo = ChartingApp.cast(application).stickerSelectionRepo(viewMode);
    }

    StickerSelection expectedSelection = selectionContext.expectedSelection();
    if (viewMode == ViewMode.TRAINING) {
      expectedSelection = StickerSelection.create(expectedSelection.sticker, null);
    }

    Observable<Optional<Sticker>> activeSticker = mStickerClicks
        .scan(Optional.empty(), StickerDialogViewModel::toggleSelection);
    Observable<Optional<StickerText>> activeText = mStickerTextClicks
        .scan(Optional.empty(), StickerDialogViewModel::toggleSelection);

    Observable.combineLatest(
        activeSticker.distinctUntilChanged(),
        activeText.distinctUntilChanged(),
        (sticker, stickerText) -> {
          StickerSelection selection = StickerSelection.create(
              sticker.orElse(null), stickerText.orElse(null));
          SelectionChecker.Result checkerResult = null;
          if (previousSelection != null && previousSelection.equals(selection)) {
            checkerResult = SelectionChecker.create(selectionContext).check(selection);
          }
          return new ViewState(selection, checkerResult);
        })
        .subscribe(mViewState);

    mDisposables.add(mRepo
        .getSelectionStream(entryDate)
        .subscribeOn(Schedulers.computation())
        .firstOrError().subscribe(stickerSelection -> stickerSelection.ifPresent(selection -> {
          onStickerClick(selection.sticker);
          onStickerTextClick(selection.text);
        })));
  }

  private static <T> Optional<T> toggleSelection(Optional<T> previous, Optional<T> value) {
    if (!value.isPresent()) {
      return Optional.empty();
    }
    if (previous.isPresent() && previous.get() == value.get()) {
      return Optional.empty();
    }
    return Optional.of(value.get());
  }

  public void onStickerClick(Sticker sticker) {
    mStickerClicks.onNext(Optional.ofNullable(sticker));
  }

  public void onStickerTextClick(StickerText stickerText) {
    mStickerTextClicks.onNext(Optional.ofNullable(stickerText));
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewState.toFlowable(BackpressureStrategy.BUFFER));
  }

  static class ViewState {

    private final StickerSelection stickerSelection;
    @Nullable private final SelectionChecker.Result checkerResult;

    public ViewState(StickerSelection stickerSelection,
                     @Nullable SelectionChecker.Result checkerResult) {
      this.stickerSelection = stickerSelection;
      this.checkerResult = checkerResult;
    }

    public Optional<Sticker> stickerSelection() {
      return Optional.ofNullable(stickerSelection.sticker);
    }

    public Optional<StickerText> stickerTextSelection() {
      return Optional.ofNullable(stickerSelection.text);
    }

    public Optional<SelectionChecker.Result> checkerResult() {
      return Optional.ofNullable(checkerResult);
    }
  }

  static class Factory implements ViewModelProvider.Factory {

    private final Activity activity;
    private final ViewMode initialViewMode;
    private final @Nullable Exercise exercise;
    private final LocalDate entryDate;
    private final CycleRenderer.StickerSelectionContext selectionContext;
    private final @Nullable StickerSelection previousSelection;

      Factory(
        Activity activity,
        ViewMode initialViewMode,
        @Nullable Exercise exercise,
        LocalDate entryDate,
        CycleRenderer.StickerSelectionContext selectionContext,
        @Nullable StickerSelection previousSelection) {
      this.activity = activity;
      this.initialViewMode = initialViewMode;
      this.exercise = exercise;
      this.entryDate = entryDate;
      this.selectionContext = selectionContext;
      this.previousSelection = previousSelection;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new StickerDialogViewModel(activity.getApplication(), entryDate, initialViewMode, exercise, selectionContext, previousSelection);
    }
  }
}
