package com.roamingroths.cmcc.mvi;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public abstract class BaseMviViewModel<I extends MviIntent, A extends MviAction, R extends MviResult, S extends MviViewState> extends ViewModel implements MviViewModel<I, S> {

  @NonNull
  private PublishSubject<I> mIntentsSubject;
  @NonNull
  private Observable<S> mStatesObservable;

  private MviProcessors<A, R> mProcessors;

  protected BaseMviViewModel(
      Class<? extends I> initialIntentClazz,
      S idleViewState,
      MviProcessors<A, R> processors) {
    mProcessors = processors;
    mIntentsSubject = PublishSubject.create();
    mStatesObservable = mIntentsSubject
        // Filter out all but the first initial intent
        .compose(upstream -> upstream.publish(shared -> Observable.merge(
            shared.ofType(initialIntentClazz).take(1),
            shared.filter(intent -> !initialIntentClazz.isInstance(intent)))
        ))
        .map(this::actionFromIntent)
        .compose(mProcessors::compose)
        .scan(idleViewState, this::reducer)
        .distinctUntilChanged()
        .replay(1)
        .autoConnect(0);
  }

  /**
   * Translate an {@link MviIntent} to an {@link MviAction}.
   */
  protected abstract A actionFromIntent(MviIntent intent);

  /**
   * The Reducer is where {@link MviViewState}, that the {@link MviView} will use to render itself
   * are created.
   *
   * It takes the last cached {@link MviViewState}, the latest {@link MviResult} and creates a new
   * {@link MviViewState} by only updating the related fields.
   */
  protected abstract S reducer(S state, R result);

  @Override
  public final void processIntents(Observable<I> intents) {
    intents.subscribe(mIntentsSubject);
  }

  @Override
  public final Observable<S> states() {
    return mStatesObservable;
  }
}
