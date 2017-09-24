package com.roamingroths.cmcc.data;

import android.content.Context;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.EntryContainer;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

/**
 * Created by parkeroth on 5/27/17.
 */

public class EntryContainerListener implements ChildEventListener {

  private final Context mContext;
  private final EntryContainerList mList;

  public EntryContainerListener(Context context, EntryContainerList list) {
    mContext = context;
    mList = list;
  }

  @Override
  public void onChildAdded(DataSnapshot dataSnapshot, String s) {
    final LocalDate entryDate = DateUtil.fromWireStr(dataSnapshot.getKey());
    ChartEntry.fromSnapshot(dataSnapshot, mList.mCycle.keys.chartKey, new Callbacks.HaltingCallback<ChartEntry>() {
      @Override
      public void acceptData(ChartEntry entry) {
        mList.addEntry(new EntryContainer(entryDate, entry));
      }
    });
  }

  @Override
  public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    final LocalDate entryDate = DateUtil.fromWireStr(dataSnapshot.getKey());
    String encryptedPayload = dataSnapshot.getValue(String.class);
    ChartEntry.fromEncryptedString(
        encryptedPayload, mList.mCycle.keys.chartKey, new Callbacks.HaltingCallback<ChartEntry>() {
          @Override
          public void acceptData(ChartEntry entry) {
            mList.changeEntry(new EntryContainer(entryDate, entry));
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
