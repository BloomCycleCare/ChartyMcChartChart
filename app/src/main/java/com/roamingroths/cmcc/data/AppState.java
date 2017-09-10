package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.firebase.auth.FirebaseAuth;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;

import org.joda.time.LocalDate;

import java.io.File;
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

  public static void fromFile(File file) throws Exception {
    List<String> lines = Files.readLines(file, Charsets.UTF_8);

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
