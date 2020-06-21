package com.bloomcyclecare.cmcc.ui.pregnancy.detail;

import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.google.common.base.Optional;

import org.joda.time.LocalDate;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
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
  private final Subject<ViewState> mState = BehaviorSubject.create();

  public PregnancyDetailViewModel(@NonNull Application application) {
    super(application);
    mPregnancyRepo = ChartingApp.cast(application).pregnancyRepo();
    stateStream().subscribe(mState);
  }

  public void init(Pregnancy pregnancy) {
    Timber.v("Initializing with %s", pregnancy);
    mDueDateUpdates.onNext(Optional.fromNullable(pregnancy.dueDate));
    mDeliveryDateUpdates.onNext(Optional.fromNullable(pregnancy.deliveryDate));
    mPregnancy.onSuccess(pregnancy);
  }

  void onNewDueDate(LocalDate date) {
    mDueDateUpdates.onNext(Optional.of(date));
  }

  void onNewDeliveryDate(LocalDate date) {
    mDeliveryDateUpdates.onNext(Optional.of(date));
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
        (pregnancy, dueDate, deliveryDate) -> {
          pregnancy.dueDate = dueDate.orNull();
          pregnancy.deliveryDate = deliveryDate.orNull();
          return new ViewState(pregnancy);
        })
        .toObservable();
  }

  public static class ViewState {

    public final Pregnancy pregnancy;

    private ViewState(Pregnancy pregnancy) {
      this.pregnancy = pregnancy;
    }
  }
}
