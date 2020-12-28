package com.bloomcyclecare.cmcc.ui.pregnancy.detail;

import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;

import org.joda.time.LocalDate;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class PregnancyDetailViewModel extends AndroidViewModel {

  private final RWPregnancyRepo mPregnancyRepo;
  private final SingleSubject<Pregnancy> mPregnancy = SingleSubject.create();
  private final Subject<Optional<LocalDate>> mDueDateUpdates = BehaviorSubject.create();
  private final Subject<Optional<LocalDate>> mDeliveryDateUpdates = BehaviorSubject.create();
  private final Subject<Optional<LocalDate>> mBreastfeedingStartDateUpdates = BehaviorSubject.create();
  private final Subject<Optional<LocalDate>> mBreastfeedingEndDateUpdates = BehaviorSubject.create();
  private final Subject<ViewState> mState = BehaviorSubject.create();
  private final Subject<Boolean> mBreastfeedingUpdates = BehaviorSubject.create();

  public PregnancyDetailViewModel(@NonNull Application application, Pregnancy pregnancy) {
    super(application);
    mPregnancyRepo = ChartingApp.cast(application).pregnancyRepo();

    Timber.v("Initializing with %s", pregnancy);
    mDueDateUpdates.onNext(Optional.ofNullable(pregnancy.dueDate));
    mDeliveryDateUpdates.onNext(Optional.ofNullable(pregnancy.deliveryDate));
    onBreastfeedingToggle(pregnancy.breastfeedingStartDate != null);
    mBreastfeedingStartDateUpdates.onNext(Optional.ofNullable(pregnancy.breastfeedingStartDate));
    mBreastfeedingEndDateUpdates.onNext(Optional.ofNullable(pregnancy.breastfeedingEndDate));
    mPregnancy.onSuccess(pregnancy);

    stateStream().subscribe(mState);
  }

  void onBreastfeedingToggle(boolean value) {
    mBreastfeedingUpdates.onNext(value);
  }

  void onNewDueDate(LocalDate date) {
    mDueDateUpdates.onNext(Optional.of(date));
  }

  void onNewDeliveryDate(LocalDate date) {
    mDeliveryDateUpdates.onNext(Optional.of(date));
  }

  void onNewBreastfeedingStartDate(LocalDate date) {
    mBreastfeedingStartDateUpdates.onNext(Optional.of(date));
  }

  void onNewBreastfeedingEndDate(LocalDate date) {
    mBreastfeedingEndDateUpdates.onNext(Optional.of(date));
  }

  Completable onSave() {
    if (!initialized()) {
      return Completable.error(new IllegalStateException("Not initialized"));
    }
    return mState.firstElement()
        .toSingle()
        .map(state -> state.pregnancy)
        .flatMapCompletable(mPregnancyRepo::update);
  }

  Maybe<ViewState> currentState() {
    if (!initialized()) {
      return Maybe.error(new IllegalStateException("Not initialized"));
    }
    return mState.firstElement();
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mState.toFlowable(BackpressureStrategy.BUFFER));
  }

  private boolean initialized() {
    return mPregnancy.hasValue();
  }

  private Observable<ViewState> stateStream() {
    return Flowable.combineLatest(
        mPregnancy.toFlowable().distinctUntilChanged(),
        mDueDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mDeliveryDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mBreastfeedingUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mBreastfeedingStartDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mBreastfeedingEndDateUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        (pregnancy, dueDate, deliveryDate, breastfeedingSwitchValue, breastfeedingStart, breastfeedingEnd) -> {
          pregnancy.dueDate = dueDate.orElse(null);
          pregnancy.deliveryDate = deliveryDate.orElse(null);
          pregnancy.breastfeedingStartDate = breastfeedingStart.orElse(null);
          pregnancy.breastfeedingEndDate = breastfeedingEnd.orElse(null);
          return new ViewState(pregnancy, breastfeedingSwitchValue);
        })
        .toObservable();
  }

  public static class ViewState {

    public final Pregnancy pregnancy;
    public final boolean showBreastfeedingSection;
    public final boolean showBreastfeedingStartDate;
    public final boolean showBreastfeedingEndDate;

    private ViewState(Pregnancy pregnancy, boolean breastfeedingToggleValue) {
      this.pregnancy = pregnancy;
      this.showBreastfeedingSection = pregnancy.deliveryDate != null;
      this.showBreastfeedingStartDate = showBreastfeedingSection && breastfeedingToggleValue;
      this.showBreastfeedingEndDate = showBreastfeedingSection && pregnancy.breastfeedingStartDate != null;
    }
  }

  static class Factory implements ViewModelProvider.Factory {

    private final @NonNull Application application;
    private final @NonNull Pregnancy pregnancy;

    public Factory(@NonNull Application application, @NonNull Pregnancy pregnancy) {
      this.application = application;
      this.pregnancy = pregnancy;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new PregnancyDetailViewModel(application, pregnancy);
    }
  }
}
