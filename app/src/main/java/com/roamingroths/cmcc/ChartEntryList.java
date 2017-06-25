package com.roamingroths.cmcc;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static android.R.interpolator.cycle;

/**
 * Created by parkeroth on 6/24/17.
 */

public class ChartEntryList {

  private ChartEntryListener mListener = null;
  private boolean mAttached = false;

  // Chart state members
  public final Cycle mCycle;
  private final SortedList<ChartEntry> mEntries;
  private final SortedSet<LocalDate> mPeakDays = new TreeSet<>();

  public static ChartEntryList create(
      final Cycle cycle,
      final SortedList<ChartEntry> entries,
      final Context context,
      Callbacks.Callback<ChartEntryList> callback) {
    final ChartEntryList chartEntryList = new ChartEntryList(cycle, entries);
    fillFromDb(chartEntryList, context, new Callbacks.ErrorForwardingCallback<LocalDate>(callback) {
      @Override
      public void acceptData(LocalDate lastEntryDate) {
        if (cycle.endDate == null && lastEntryDate.isBefore(DateUtil.now())) {
          createEmptyEntries(chartEntryList, context, lastEntryDate, null, this);
        }
      }
    });
    return chartEntryList;
  }

  private ChartEntryList(Cycle cycle, SortedList<ChartEntry> entries) {
    mEntries = entries;
    mCycle = cycle;
  }

  public synchronized void attachListener(Context context) {
    if (mListener == null) {
      mListener = new ChartEntryListener(context, this);
    }
    if (mAttached) {
      throw new IllegalStateException();
    }
    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("entries").child(mCycle.id);
    dbRef.addChildEventListener(mListener);
    dbRef.keepSynced(true);
    mAttached = true;
  }

  public synchronized void detachListener() {
    if (!mAttached) {
      throw new IllegalStateException();
    }
    FirebaseDatabase.getInstance().getReference("entries").child(mCycle.id).removeEventListener(mListener);
    mAttached = false;
  }

  public int addEntry(ChartEntry entry) {
    // Maybe add peak day to set
    if (entry.peakDay) {
      mPeakDays.add(entry.date);
    }
    // Add entry to list
    int index = mEntries.add(entry);
    return index;
  }

  public void changeEntry(ChartEntry entry) {
    // Maybe add or remove from peak day set
    if (entry.peakDay) {
      mPeakDays.add(entry.date);
    } else {
      mPeakDays.remove(entry);
    }
    int entryIndex = getEntryIndex(entry.date);
    mEntries.updateItemAt(entryIndex, entry);
  }

  public void removeEntry(String entryDateStr) {
    removeEntry(findEntry(entryDateStr));
  }

  public void removeEntry(ChartEntry entry) {
    // Maybe remove peak day from set
    mPeakDays.remove(entry.date);
    mEntries.remove(entry);
  }

  @Nullable
  public ChartEntry findEntry(String dateStr) {
    int index = getEntryIndex(DateUtil.fromWireStr(dateStr));
    if (index < 0) {
      return null;
    }
    return get(index);
  }

  public int size() {
    return mEntries.size();
  }

  public ChartEntry get(int index) {
    return mEntries.get(index);
  }

  public boolean shouldShowBaby(ChartEntry entry) {
    if (entry == null) {
      return false;
    }
    return entry.observation != null && entry.observation.hasMucus();
  }

  public String getPeakDayViewText(ChartEntry entry) {
    if (entry == null) {
      return "";
    }
    LocalDate closestPeakDay = null;
    for (LocalDate peakDay : mPeakDays) {
      if (peakDay.isAfter(entry.date)) {
        continue;
      }
      if (closestPeakDay == null) {
        closestPeakDay = peakDay;
      }
      if (closestPeakDay.isBefore(peakDay)) {

        closestPeakDay = peakDay;
      }
    }
    if (closestPeakDay == null) {
      return "";
    }
    return getPeakDayViewText(entry, closestPeakDay);
  }

  private String getPeakDayViewText(ChartEntry entry, LocalDate peakDay) {
    if (entry.date.isBefore(peakDay)) {
      return "";
    }
    if (entry.date.equals(peakDay)) {
      return "P";
    }
    int daysAfterPeak = Days.daysBetween(peakDay, entry.date).getDays();
    if (daysAfterPeak < 4) {
      return String.valueOf(daysAfterPeak);
    }
    return "";
  }

  private int getEntryIndex(LocalDate entryDate) {
    for (int i = 0; i < mEntries.size(); i++) {
      ChartEntry entry = mEntries.get(i);
      if (entry.getDateStr().equals(entryDate)) {
        return i;
      }
    }
    return -1;
  }

  private static void fillFromDb(
      final ChartEntryList chartEntryList,
      final Context context,
      final Callbacks.Callback<LocalDate> lastDateAddedCallback) {
    Log.v("ChartEntryList", "Begin filling from DB");
    DatabaseReference dbRef =
        FirebaseDatabase.getInstance().getReference("entries").child(chartEntryList.mCycle.id);
    dbRef.keepSynced(true);
    dbRef.addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(lastDateAddedCallback) {
      @Override
      public void onDataChange(DataSnapshot entriesSnapshot) {
        final AtomicLong entriesToDecrypt = new AtomicLong(entriesSnapshot.getChildrenCount());
        final AtomicReference<LocalDate> lastEntryDate = new AtomicReference<>();
        for (DataSnapshot entrySnapshot : entriesSnapshot.getChildren()) {
          LocalDate entryDate = DateUtil.fromWireStr(entrySnapshot.getKey());
          if (lastEntryDate.get() == null || lastEntryDate.get().isBefore(entryDate)) {
            lastEntryDate.set(entryDate);
          }
          ChartEntry.fromEncryptedString(entrySnapshot.getValue(String.class), context,
              new Callbacks.ErrorForwardingCallback<ChartEntry>(lastDateAddedCallback) {

                @Override
                public void acceptData(ChartEntry entry) {
                  chartEntryList.addEntry(entry);
                  long numLeftToDecrypt = entriesToDecrypt.decrementAndGet();
                  if (numLeftToDecrypt < 1) {
                    Log.v("DataStore", "Done filling ChartEntryAdapter");
                    lastDateAddedCallback.acceptData(lastEntryDate.get());
                  } else {
                    Log.v("DataStore", "Still waiting for " + numLeftToDecrypt + " decryptions");
                  }
                }
              });
        }
      }
    });
  }

  private static void createEmptyEntries(
      final ChartEntryList chartEntryList,
      Context context,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      final Callbacks.Callback<?> callback) {
    String cycleId = chartEntryList.mCycle.id;
    final DatabaseReference ref =
        FirebaseDatabase.getInstance().getReference("entries").child(cycleId);
    endDate = (endDate == null) ? LocalDate.now().plusDays(1) : endDate.plusDays(1);
    Set<LocalDate> dates = new HashSet<>();
    for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
      dates.add(date);
    }
    final AtomicLong entriesRemaining = new AtomicLong(dates.size());
    for (LocalDate date : dates) {
      Log.v("ChartEntryList", "Creating empty entry for " + cycleId + " " + date);
      final ChartEntry entry = ChartEntry.emptyEntry(date);
      CryptoUtil.encrypt(entry, context, Callbacks.singleUse(new Callbacks.ErrorForwardingCallback<String>(callback) {
        @Override
        public void acceptData(String encryptedEntry) {
          Runnable runnable = new Runnable() {
            @Override
            public void run() {
              chartEntryList.addEntry(entry);
              if (entriesRemaining.decrementAndGet() == 0) {
                callback.acceptData(null);
              }
            }
          };
          ref.child(entry.getDateStr()).setValue(
              encryptedEntry, Listeners.completionListener(callback, runnable));
        }
      }));
    }
  }
}
