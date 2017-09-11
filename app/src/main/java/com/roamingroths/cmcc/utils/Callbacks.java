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

  public static <T> Callback<T> singleUse(Callback<T> delegate) {
    return new SingleUseCallback<>(delegate);
  }

  private static class SingleUseCallback<T> implements Callback<T> {

    private boolean mHasExecuted;
    private Callback<T> mDelegate;
    private Throwable mExecutionThrowable;

    public SingleUseCallback(Callback<T> delegate) {
      mDelegate = delegate;
      mHasExecuted = false;
    }

    @Override
    public final synchronized void acceptData(T data) {
      markExecuted();
      mDelegate.acceptData(data);
    }

    @Override
    public final synchronized void handleNotFound() {
      markExecuted();
      mDelegate.handleNotFound();
    }

    @Override
    public final synchronized void handleError(DatabaseError error) {
      markExecuted();
      mDelegate.handleError(error);
    }

    private void markExecuted() {
      if (mHasExecuted) {
        throw new IllegalStateException("Callback already executed!", mExecutionThrowable);
      }
      mExecutionThrowable = new Throwable();
      mHasExecuted = true;
    }
  }

  public static abstract class SwitchingCallback extends HaltingCallback<Boolean> {

    public abstract void positive();

    public abstract void negative();

    @Override
    public void acceptData(Boolean result) {
      if (result) {
        positive();
      } else {
        negative();
      }
    }
  }

  public static abstract class HaltingCallback<T> implements Callback<T> {
    @Override
    public void handleNotFound() {
      throw new IllegalStateException("Not Found");
    }

    @Override
    public void handleError(DatabaseError error) {
      throw new IllegalStateException(error.toException());
    }
  }

  public static abstract class ErrorForwardingCallback<T> implements Callback<T> {
    private final Callback<?> mDelegate;

    public ErrorForwardingCallback(Callback<?> delegate) {
      mDelegate = delegate;
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

  // TODO: remember how to make this extend ErrorForwardingCallback
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
