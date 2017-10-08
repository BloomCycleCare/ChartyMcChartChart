package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.crypto.CyrptoExceptions;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.logic.SymptomEntry;
import com.roamingroths.cmcc.logic.WellnessEntry;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.GsonUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.LocalDate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class holding application state for writing to JSON in plain text.
 */

public class AppState {
  public final Set<CycleData> cycles;

  public static void create(final CycleProvider cycleProvider, final Callback<AppState> callback) {
    Log.v("AppState", "Create");
    fetchCycleDatas(cycleProvider, new Callbacks.ErrorForwardingCallback<Set<CycleData>>(callback) {
      @Override
      public void acceptData(Set<CycleData> cycles) {
        AppState appState = new AppState(cycles);
        callback.acceptData(appState);
      }
    });
  }

  private static AppState parseFromFile(InputStream in, Callback<?> callback) {
    try {
      Log.v("AppState", "Reading file");
      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      StringBuilder total = new StringBuilder();
      String line;
      while ((line = r.readLine()) != null) {
        total.append(line);
      }
      return GsonUtil.getGsonInstance().fromJson(total.toString(), AppState.class);
    } catch (Exception e) {
      callback.handleError(DatabaseError.fromException(e));
    }
    return null;
  }

  public static void parseAndPushToDB(InputStream in, final String userId, final CycleProvider cycleProvider, final Callback<Cycle> callback) {
    final AppState appState = parseFromFile(in, callback);
    Log.v("AppState", "Found " + appState.cycles.size() + " cycles in file");
    Log.v("AppState", "Dropping existing cycles");
    cycleProvider.dropCycles(new Callbacks.ErrorForwardingCallback<Void>(callback) {
      @Override
      public void acceptData(Void data) {
        Log.v("AppState", "Existing cycles dropped");
        callback.acceptData(putCycleDatas(appState.cycles, userId, cycleProvider, callback));
      }
    });
  }

  private static Cycle putCycleDatas(
      Collection<CycleData> cycleDatas, String userId, final CycleProvider cycleProvider, final Callback<?> callback) {
    Cycle currentCycle = null;
    for (final CycleData cycleData : cycleDatas) {
      final Cycle cycle = cycleData.cycle;
      if (cycle.endDate == null) {
        currentCycle = cycle;
      }
      Log.v("AppState", "Creating new keys for cycle starting " + cycle.startDateStr);
      cycle.setKeys(new Cycle.Keys(CryptoUtil.createSecretKey(), CryptoUtil.createSecretKey(), CryptoUtil.createSecretKey()));
      Log.v("AppState", "Storing cycle starting " + cycle.startDateStr);
      cycleProvider.putCycle(userId, cycle, new Callbacks.ErrorForwardingCallback<Cycle>(callback) {
        @Override
        public void acceptData(Cycle data) {
          for (EntrySet entrySet : cycleData.entrySets) {
            putEntry(cycleProvider, cycle, entrySet.mChartEntry, callback);
            putEntry(cycleProvider, cycle, entrySet.mWellnessEntry, callback);
            putEntry(cycleProvider, cycle, entrySet.mSymptomEntry, callback);
          }
        }
      });
    }
    return currentCycle;
  }

  public static void putEntry(CycleProvider cycleProvider, Cycle cycle, Entry entry, Callback<?> callback) {
    EntryProvider provider = cycleProvider.getProviderForEntry(entry);
    entry.swapKey(provider.getKey(cycle));
    try {
      provider.putEntry(cycle.id, entry, Listeners.completionListener(callback));
    } catch (CyrptoExceptions.CryptoException ce) {
      callback.handleError(DatabaseError.fromException(ce));
    }
  }

  private static void fetchCycleData(final Cycle cycle, CycleProvider cycleProvider, final Callback<CycleData> callback) {
    final Map<Class<? extends Entry>, Map<LocalDate, Entry>> entryIndex = Maps.newConcurrentMap();
    for (EntryProvider entryProvider : cycleProvider.getEntryProviders()) {
      entryIndex.put(entryProvider.getEntryClazz(), Maps.<LocalDate, Entry>newConcurrentMap());
    }
    final AtomicInteger providersToProcess =
        new AtomicInteger(cycleProvider.getEntryProviders().size());
    for (EntryProvider entryProvider : cycleProvider.getEntryProviders()) {
      final Class<? extends Entry> clazz = entryProvider.getEntryClazz();
      entryProvider.getDecryptedEntries(cycle, new Callbacks.ErrorForwardingCallback<Map<LocalDate, Entry>>(callback) {
        @Override
        public void acceptData(Map<LocalDate, Entry> entries) {
          entryIndex.put(clazz, entries);
          if (providersToProcess.decrementAndGet() == 0) {
            Set<EntrySet> entrySets = new HashSet<EntrySet>();
            for (LocalDate entryDate : entries.keySet()) {
              entrySets.add(new EntrySet(
                  (ChartEntry) entryIndex.get(ChartEntry.class).get(entryDate),
                  (WellnessEntry) entryIndex.get(WellnessEntry.class).get(entryDate),
                  (SymptomEntry) entryIndex.get(SymptomEntry.class).get(entryDate)));
            }
            callback.acceptData(new CycleData(cycle, entrySets));
          }
        }
      });
    }
  }

  private static void fetchCycleDatas(final CycleProvider cycleProvider, final Callback<Set<CycleData>> callback) {
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    Log.v("AppState", "Fetching cycle datas for user " + userId);
    cycleProvider.getAllCycles(userId, new Callbacks.ErrorForwardingCallback<Collection<Cycle>>(callback) {
      @Override
      public void acceptData(final Collection<Cycle> cycles) {
        Log.v("AppState", "Found " + cycles.size() + " cycles");
        final AtomicInteger cyclesProcessed = new AtomicInteger(0);
        final Map<String, CycleData> cycleDatas = Maps.newConcurrentMap();
        for (final Cycle cycle : cycles) {
          fetchCycleData(cycle, cycleProvider, new Callbacks.ErrorForwardingCallback<CycleData>(callback) {
            @Override
            public void acceptData(CycleData cycleData) {
              cycleDatas.put(cycle.id, cycleData);
              if (cyclesProcessed.incrementAndGet() == cycles.size()) {
                callback.acceptData(ImmutableSet.copyOf(cycleDatas.values()));
              }
            }
          });
        }
      }
    });
  }

  public AppState(Set<CycleData> cycles) {
    this.cycles = cycles;
  }

  public static class CycleData {
    public final Cycle cycle;
    public final Set<EntrySet> entrySets;

    public CycleData(Cycle cycle, Set<EntrySet> entrySets) {
      this.cycle = cycle;
      this.entrySets = entrySets;
    }
  }

  public static class EntrySet {
    public final ChartEntry mChartEntry;
    public final WellnessEntry mWellnessEntry;
    public final SymptomEntry mSymptomEntry;

    public EntrySet(ChartEntry chartEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry) {
      mChartEntry = chartEntry;
      mWellnessEntry = wellnessEntry;
      mSymptomEntry = symptomEntry;
    }
  }
}
