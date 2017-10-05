package com.roamingroths.cmcc.utils;

import android.os.AsyncTask;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Runnables;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by parkeroth on 5/21/17.
 */

public class Listeners {

  public static abstract class SimpleValueEventListener implements ValueEventListener {

    private final Callbacks.Callback<?> mCallback;

    public SimpleValueEventListener(Callbacks.Callback<?> callback) {
      mCallback = Preconditions.checkNotNull(callback);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
      mCallback.handleError(databaseError);
    }
  }

  public static DatabaseReference.CompletionListener completionListener(
      Callbacks.Callback<?> callback) {
    return completionListener(callback, Runnables.doNothing());
  }

  public static DatabaseReference.CompletionListener doneOnCompletion(
      final Callbacks.Callback<Void> onDone) {
    return completionListener(onDone, new Runnable() {
      @Override
      public void run() {
        onDone.acceptData(null);
      }
    });
  }

  public static DatabaseReference.CompletionListener completionListener(
      Callbacks.Callback<?> callback, Runnable onSuccess) {
    return new SimpleCompletionListener(callback, onSuccess);
  }

  private static class SimpleCompletionListener implements DatabaseReference.CompletionListener {

    private final Callbacks.Callback<?> mCallback;
    private final Runnable mOnSuccess;

    private SimpleCompletionListener(Callbacks.Callback<?> callback, Runnable onSuccess) {
      mCallback = callback;
      mOnSuccess = onSuccess;
    }

    @Override
    public void onComplete(final DatabaseError databaseError, DatabaseReference databaseReference) {
      new AsyncTask<Void, Integer, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          if (databaseError == null) {
            mOnSuccess.run();
          } else {
            mCallback.handleError(databaseError);
          }
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
