package com.roamingroths.cmcc.mvi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;

/**
 * Created by parkeroth on 3/4/18.
 */

public abstract class MviProcessors<A extends MviAction, R extends MviResult> {

  private final Map<Class<? extends A>, ObservableTransformer<? extends A, ? extends R>> mMapping;

  protected MviProcessors() {
    this.mMapping = new HashMap<>();
  }

  /**
   * Splits the {@link Observable<MviAction>} to match each type of {@link MviAction} to its
   * corresponding business logic processor. Each processor takes a defined {@link MviAction},
   * returns a defined {@link MviResult}.
   */
  @SuppressWarnings("unchecked")
  public Observable<R> compose(Observable<A> actions) {
    List<Observable<R>> processors = new ArrayList<>();

    for (Map.Entry<Class<? extends A>, ObservableTransformer<? extends A, ? extends R>> entry
        : mMapping.entrySet()) {
      Class<? extends A> actionClazz = entry.getKey();
      ObservableTransformer<A, R> actionToResult = (ObservableTransformer<A, R>) entry.getValue();
      processors.add(actions.ofType(actionClazz).compose(actionToResult));
    }

    return actions.publish(shared -> Observable.merge(processors).mergeWith(shared
        .filter(this::unknownActionFilter)
        .flatMap(w -> Observable.error(new IllegalArgumentException("Unknown Action: " + w)))));
  }

  protected final void registerTransformer(Class<? extends A> actionClazz, ObservableTransformer<? extends A, ? extends R> transformer) {
    mMapping.put(actionClazz, transformer);
  }

  private boolean unknownActionFilter(A action) {
    for (Class<? extends A> actionClazz : mMapping.keySet()) {
      if (actionClazz.isInstance(action)) {
        return false;
      }
    }
    return true;
  }
}
