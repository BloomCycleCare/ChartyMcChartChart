package com.roamingroths.cmcc;

import android.content.Context;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.utils.Callbacks;

/**
 * Created by parkeroth on 5/27/17.
 */

public class ChartEntryListener implements ChildEventListener {

  private final ChartEntryAdapter mAdapter;
  private final Context mContext;

  public ChartEntryListener(Context context, ChartEntryAdapter adapter) {
    mContext = context;
    mAdapter = adapter;
  }

  @Override
  public void onChildAdded(DataSnapshot dataSnapshot, String s) {
    ChartEntry.fromSnapshot(dataSnapshot, mContext, new Callbacks.HaltingCallback<ChartEntry>() {
      @Override
      public void acceptData(ChartEntry entry) {
        mAdapter.addEntry(entry);
      }
    });
  }

  @Override
  public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    mAdapter.changeEntry(dataSnapshot.getKey(), dataSnapshot.getValue(String.class));
    mAdapter.notifyDataSetChanged();
  }

  @Override
  public void onChildRemoved(DataSnapshot dataSnapshot) {
    mAdapter.removeEntry(dataSnapshot.getKey());
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
