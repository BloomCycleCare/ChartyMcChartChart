package com.roamingroths.cmcc.utils;

import com.google.common.base.Preconditions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by parkeroth on 5/21/17.
 */

public class EventListeners {

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
}
