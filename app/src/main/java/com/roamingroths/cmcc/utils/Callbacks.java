package com.roamingroths.cmcc.utils;

import com.google.common.base.Function;
import com.google.firebase.database.DatabaseError;

/**
 * Created by parkeroth on 5/21/17.
 */

public class Callbacks {

  public interface Callback<T> {
    void acceptData(T data);

    void handleNotFound();

    void handleError(DatabaseError error);
  }

  public static class TransformingCallback<I, O> implements Callback<I> {
    private final Function<I, O> mDataTransformer;
    private final Callback<O> mDelegate;

    public TransformingCallback(Callback<O> delegate, Function<I, O> dataTransformer) {
      mDelegate = delegate;
      mDataTransformer = dataTransformer;
    }

    @Override
    public void acceptData(I data) {
      mDelegate.acceptData(mDataTransformer.apply(data));
    }

    @Override
    public void handleNotFound() {
      mDelegate.handleNotFound();
    }

    @Override
    public void handleError(DatabaseError error) {
      mDelegate.handleError(error);
    }
  }
}
