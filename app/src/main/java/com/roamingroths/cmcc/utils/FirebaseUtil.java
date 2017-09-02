package com.roamingroths.cmcc.utils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by parkeroth on 9/2/17.
 */

public class FirebaseUtil {

  /**
   * Utility method for doing what amounts to a critical read of a DB reference in Firebase.
   *
   * @param reference from which to read
   * @param callback  which will be triggered for failures
   * @param listener  which will be given the data
   */
  public static void criticalRead(
      final DatabaseReference reference, final Callbacks.Callback callback, final ValueEventListener listener) {
    reference.keepSynced(true);
    reference.child("pile-of-poo").setValue(true, new DatabaseReference.CompletionListener() {
      @Override
      public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        reference.addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            reference.child("pile-of-poo").removeValue();
            listener.onDataChange(dataSnapshot);
          }
        });
      }
    });
  }
}
