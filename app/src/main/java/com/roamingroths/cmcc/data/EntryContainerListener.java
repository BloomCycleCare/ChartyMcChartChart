package com.roamingroths.cmcc.data;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.EntryContainer;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 5/27/17.
 */

public class EntryContainerListener implements ChildEventListener {

  private final Context mContext;
  private final EntryContainerList mList;
  private final EntryProvider<ChartEntry> mProvider;

  public EntryContainerListener(Context context, EntryContainerList list, EntryProvider<ChartEntry> provider) {
    mContext = context;
    mList = list;
    mProvider = provider;
  }

  @Override
  public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
    final LocalDate entryDate = DateUtil.fromWireStr(dataSnapshot.getKey());
    Single.just(dataSnapshot)
        .observeOn(Schedulers.computation())
        .flatMap(new Function<DataSnapshot, SingleSource<ChartEntry>>() {
          @Override
          public SingleSource<ChartEntry> apply(DataSnapshot snapshot) throws Exception {
            return mProvider.fromSnapshot(dataSnapshot, mList.mCycle.keys.chartKey);
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<ChartEntry>() {
          @Override
          public void accept(ChartEntry chartEntry) throws Exception {
            mList.addEntry(new EntryContainer(entryDate, chartEntry, null, null));
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            Log.e(EntryContainerList.class.getSimpleName(), "Error decoding ChartEntry from DataSnapshot", throwable);
          }
        });
  }

  @Override
  public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    final LocalDate entryDate = DateUtil.fromWireStr(dataSnapshot.getKey());
    Single.just(dataSnapshot)
        .subscribeOn(Schedulers.computation())
        .flatMap(new Function<DataSnapshot, SingleSource<ChartEntry>>() {
          @Override
          public SingleSource<ChartEntry> apply(DataSnapshot snapshot) throws Exception {
            return mProvider.fromSnapshot(snapshot, mList.mCycle.keys.chartKey);
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<ChartEntry>() {
          @Override
          public void accept(ChartEntry chartEntry) throws Exception {
            mList.changeEntry(new EntryContainer(entryDate, chartEntry, null, null));
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            Log.e(EntryContainerList.class.getSimpleName(), "Error decoding ChartEntry from DataSnapshot", throwable);
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
