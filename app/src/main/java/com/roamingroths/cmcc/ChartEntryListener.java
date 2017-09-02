package com.roamingroths.cmcc;

import android.content.Context;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.utils.Callbacks;

/**
 * Created by parkeroth on 5/27/17.
 */

public class ChartEntryListener implements ChildEventListener {

  private final Context mContext;
  private final ChartEntryList mList;

  public ChartEntryListener(Context context, ChartEntryList list) {
    mContext = context;
    mList = list;
  }

  @Override
  public void onChildAdded(DataSnapshot dataSnapshot, String s) {
    ChartEntry.fromSnapshot(dataSnapshot, mList.mCycle.key, new Callbacks.HaltingCallback<ChartEntry>() {
      @Override
      public void acceptData(ChartEntry entry) {
        mList.addEntry(entry);
      }
    });
  }

  @Override
  public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    String dateStr = dataSnapshot.getKey();
    String encryptedPayload = dataSnapshot.getValue(String.class);
    ChartEntry.fromEncryptedString(
        encryptedPayload, mList.mCycle.key, new Callbacks.HaltingCallback<ChartEntry>() {
          @Override
          public void acceptData(ChartEntry entry) {
            mList.changeEntry(entry);
          }
        });
  }

  @Override
  public void onChildRemoved(DataSnapshot dataSnapshot) {
    mList.removeEntry(dataSnapshot.getKey());
  }

  @Override
  public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    throw new IllegalStateException("NOT IMPLEMENTED");
  }

  @Override
  public void onCancelled(DatabaseError databaseError) {
    databaseError.toException().printStackTrace();
  }
}
