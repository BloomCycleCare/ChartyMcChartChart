package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.GsonUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.LocalDate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    AppState appState = null;
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
        callback.acceptData(putCycleData(appState.cycles, userId, cycleProvider, callback));
      }
    });
  }

  private static Cycle putCycleData(
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
          for (ChartEntry entry : cycleData.entries) {
            entry.setKey(cycle.keys.chartKey);
            try {
              cycleProvider.getChartEntryProvider().putEntry(
                  cycle.id, entry, Listeners.completionListener(callback));
            } catch (CryptoUtil.CryptoException ce) {
              callback.handleError(DatabaseError.fromException(ce));
            }
          }
        }
      });
    }
    return currentCycle;
  }

  private static void fetchCycleDatas(final CycleProvider cycleProvider, final Callback<Set<CycleData>> callback) {
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    Log.v("AppState", "Fetching cycle datas for user " + userId);
    cycleProvider.getAllCycles(userId, new Callbacks.ErrorForwardingCallback<Collection<Cycle>>(callback) {
      @Override
      public void acceptData(final Collection<Cycle> cycles) {
        Log.v("AppState", "Found " + cycles.size() + " cycles");
        final Map<String, CycleData> data = Maps.newConcurrentMap();
        for (final Cycle cycle : cycles) {
          Callback<Map<LocalDate, ChartEntry>> entriesCallback = new Callbacks.ErrorForwardingCallback<Map<LocalDate, ChartEntry>>(callback) {
            @Override
            public void acceptData(Map<LocalDate, ChartEntry> entryMap) {
              List<ChartEntry> entries = Lists.newArrayList(entryMap.values());
              data.put(cycle.id, new CycleData(cycle, entries));
              if (data.size() == cycles.size()) {
                callback.acceptData(ImmutableSet.copyOf(data.values()));
              }
            }
          };
          cycleProvider.getChartEntryProvider().getDecryptedEntries(cycle, entriesCallback);
        }
      }
    });
  }

  public AppState(Set<CycleData> cycles) {
    this.cycles = cycles;
  }

  public static class CycleData {
    public final Cycle cycle;
    public final List<ChartEntry> entries;

    public CycleData(Cycle cycle, List<ChartEntry> entries) {
      this.cycle = cycle;
      this.entries = entries;
    }
  }
}
