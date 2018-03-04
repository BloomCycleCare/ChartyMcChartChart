package com.roamingroths.cmcc.mvi;

import io.reactivex.Observable;

/**
 * Object that will subscribes to a {@link MviView}'s {@link MviIntent}s,
 * process it and emit a {@link MviViewState} back.
 *
 * Credit: https://github.com/oldergod/android-architecture
 *
 * @param <I> Top class of the {@link MviIntent} that the {@link MviViewModel} will be subscribing
 *            to.
 * @param <S> Top class of the {@link MviViewState} the {@link MviViewModel} will be emitting.
 */
public interface MviViewModel<I extends MviIntent, S extends MviViewState> {

  void processIntents(Observable<I> intents);

  Observable<S> states();
}
