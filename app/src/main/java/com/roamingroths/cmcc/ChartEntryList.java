package com.roamingroths.cmcc;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.common.base.Preconditions;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static android.R.interpolator.cycle;

/**
 * Created by parkeroth on 6/24/17.
 */

public class ChartEntryList {

  private final AtomicBoolean mInitialized = new AtomicBoolean(false);

  // Chart state members
  public final Cycle mCycle;
  private final SortedList<ChartEntry> mEntries;
  private final Map<LocalDate, ChartEntry> mEntryIndex = new HashMap<>();
  private final SortedSet<LocalDate> mPeakDays = new TreeSet<>();

  public static Builder builder(Cycle cycle) {
    return new Builder(cycle);
  }

  private ChartEntryList(Cycle cycle, SortedList<ChartEntry> entries) {
    mEntries = entries;
    mCycle = cycle;
  }

  public void initialize(final Context context, final Callbacks.Callback<Void> doneCallback) {
    if (mInitialized.compareAndSet(false, true)) {
      fillFromDb(context, new Callbacks.ErrorForwardingCallback<LocalDate>(doneCallback) {
        @Override
        public void acceptData(LocalDate lastEntryDate) {
          if (mCycle.endDate == null && lastEntryDate.isBefore(DateUtil.now())) {
            LocalDate endDate = null;
            createEmptyEntries(context, lastEntryDate, endDate, this);
          } else {
            doneCallback.acceptData(null);
          }
        }
      });
    }
  }

  public synchronized void addEntry(ChartEntry entry) {
    if (mEntryIndex.containsKey(entry.date)) {
      changeEntry(entry);
      return;
    }
    // Maybe add peak day to set
    if (entry.peakDay) {
      mPeakDays.add(entry.date);
    }
    // Add entry to list
    mEntries.add(entry);
    mEntryIndex.put(entry.date, entry);
    return;
  }

  public synchronized void changeEntry(ChartEntry entry) {
    // Maybe add or remove from peak day set
    if (mEntryIndex.containsKey(entry.date) && mEntryIndex.get(entry.date).equals(entry)) {
      return;
    }
    if (entry.peakDay) {
      mPeakDays.add(entry.date);
    } else {
      mPeakDays.remove(entry.date);
    }
    int entryIndex = getEntryIndex(entry.date);
    if (entryIndex < 0) {
      Log.w("ChartEntryList", "No entry to update for: " + entry.date + ", adding instead.");
    } else {
      mEntries.updateItemAt(entryIndex, entry);
    }
  }

  public void removeEntry(String entryDateStr) {
    removeEntry(findEntry(entryDateStr));
  }

  public synchronized void removeEntry(ChartEntry entry) {
    // Maybe remove peak day from set
    mEntryIndex.remove(entry.date);
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
      if (entry.date.equals(entryDate)) {
        return i;
      }
    }
    return -1;
  }

  public static class Builder {

    private final Cycle cycle;
    private ChartEntryAdapter adapter;

    private Builder(Cycle cycle) {
      this.cycle = Preconditions.checkNotNull(cycle);
    }

    public Builder withAdapter(ChartEntryAdapter adapter) {
      this.adapter = adapter;
      return this;
    }

    public ChartEntryList build() {
      SortedList<ChartEntry> entries = new SortedList<ChartEntry>(ChartEntry.class, new SortedList.Callback<ChartEntry>() {
        @Override
        public void onInserted(int position, int count) {
          if (adapter != null) {
            adapter.notifyItemRangeInserted(position, count);
          }
        }

        @Override
        public void onRemoved(int position, int count) {
          if (adapter != null) {
            adapter.notifyItemRangeRemoved(position, count);
          }
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
          if (adapter != null) {
            adapter.notifyItemMoved(fromPosition, toPosition);
          }
        }

        @Override
        public void onChanged(int position, int count) {
          if (adapter != null) {
            adapter.notifyItemRangeChanged(position, count);
          }
        }

        @Override
        public int compare(ChartEntry e1, ChartEntry e2) {
          return e2.date.compareTo(e1.date);
        }

        @Override
        public boolean areContentsTheSame(ChartEntry oldItem, ChartEntry newItem) {
          return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(ChartEntry item1, ChartEntry item2) {
          return item1 == item2;
        }
      });
      return new ChartEntryList(cycle, entries);
    }
  }

  private void fillFromDb(
      final Context context,
      final Callbacks.Callback<LocalDate> lastDateAddedCallback) {
    Log.v("ChartEntryList", "Begin filling from DB");
    DatabaseReference dbRef =
        FirebaseDatabase.getInstance().getReference("entries").child(mCycle.id);
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
                  addEntry(entry);
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

  private void createEmptyEntries(
      Context context,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      final Callbacks.Callback<?> callback) {
    String cycleId = mCycle.id;
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
              addEntry(entry);
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
