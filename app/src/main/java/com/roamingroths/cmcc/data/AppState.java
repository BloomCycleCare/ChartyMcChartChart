package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.collect.ImmutableSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.logic.ObservationEntry;
import com.roamingroths.cmcc.logic.SymptomEntry;
import com.roamingroths.cmcc.logic.WellnessEntry;
import com.roamingroths.cmcc.utils.GsonUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.functions.Predicate;

/**
 * Class holding application state for writing to JSON in plain text.
 */

public class AppState {
  public final List<CycleData> cycles;

  public static Single<AppState> create(final CycleProvider cycleProvider) {
    return fetchCycleDatas(cycleProvider)
        .toList()
        .flatMap(new Function<List<CycleData>, SingleSource<? extends AppState>>() {
          @Override
          public SingleSource<? extends AppState> apply(List<CycleData> cycleData) throws Exception {
            return Single.just(new AppState(cycleData));
          }
        });
  }

  private static Callable<AppState> parseFromFile(final InputStream in) {
    return new Callable<AppState>() {
      @Override
      public AppState call() throws Exception {
        Log.v("AppState", "Reading file");
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
          total.append(line);
        }
        return GsonUtil.getGsonInstance().fromJson(total.toString(), AppState.class);
      }
    };
  }

  public static Single<Cycle> parseAndPushToDB(InputStream in, final FirebaseUser user, final CycleProvider cycleProvider) {
    return Single.fromCallable(parseFromFile(in))
        .flatMapObservable(new Function<AppState, ObservableSource<? extends CycleData>>() {
          @Override
          public ObservableSource<? extends CycleData> apply(AppState appState) throws Exception {
            return Observable.fromIterable(appState.cycles);
          }
        })
        .flatMap(new Function<CycleData, ObservableSource<Cycle>>() {
          @Override
          public ObservableSource<Cycle> apply(CycleData cycleData) throws Exception {
            Cycle cycle = cycleData.cycle;
            Log.v("AppState", "Creating new keys for cycle starting " + cycle.startDateStr);
            cycle.setKeys(new Cycle.Keys(AesCryptoUtil.createKey(), AesCryptoUtil.createKey(), AesCryptoUtil.createKey()));
            Set<Completable> putResults = new HashSet<>();
            putResults.add(cycleProvider.putCycleRx(user.getUid(), cycle));
            for (EntrySet entrySet : cycleData.entrySets) {
              putResults.add(putEntry(cycleProvider, cycle, entrySet.mObservationEntry));
              putResults.add(putEntry(cycleProvider, cycle, entrySet.mWellnessEntry));
              putResults.add(putEntry(cycleProvider, cycle, entrySet.mSymptomEntry));
            }
            return Completable.merge(putResults).andThen(Observable.just(cycle));
          }
        })
        .filter(new Predicate<Cycle>() {
          @Override
          public boolean test(Cycle cycle) throws Exception {
            return cycle.endDate == null;
          }
        })
        .firstOrError();
  }

  private static Completable putEntry(CycleProvider cycleProvider, Cycle cycle, Entry entry) {
    EntryProvider provider = cycleProvider.getProviderForEntry(entry);
    entry.swapKey(provider.getKey(cycle));
    return provider.putEntry(cycle.id, entry);
  }

  private static Observable<CycleData> fetchCycleDatas(final CycleProvider cycleProvider) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    Log.v("AppState", "Fetching cycle datas for user " + user.getUid());
    return cycleProvider.getAllCycles(user.getUid())
        .flatMap(new Function<Cycle, ObservableSource<CycleData>>() {
          @Override
          public ObservableSource<CycleData> apply(final Cycle cycle) throws Exception {
            Observable<ObservationEntry> chartEntries =
                cycleProvider.getProviderForClazz(ObservationEntry.class).getDecryptedEntries(cycle);
            Observable<WellnessEntry> wellnessEntries =
                cycleProvider.getProviderForClazz(WellnessEntry.class).getDecryptedEntries(cycle);
            Observable<SymptomEntry> symptomEntries =
                cycleProvider.getProviderForClazz(SymptomEntry.class).getDecryptedEntries(cycle);
            Observable<EntrySet> entrySets = Observable.zip(chartEntries, wellnessEntries, symptomEntries, new Function3<ObservationEntry, WellnessEntry, SymptomEntry, EntrySet>() {
              @Override
              public EntrySet apply(ObservationEntry chartEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry) throws Exception {
                return new EntrySet(chartEntry, wellnessEntry, symptomEntry);
              }
            });
            return entrySets.toList().flatMapObservable(new Function<List<EntrySet>, ObservableSource<? extends CycleData>>() {
              @Override
              public ObservableSource<? extends CycleData> apply(List<EntrySet> entrySets) throws Exception {
                return Observable.just(new CycleData(cycle, entrySets));
              }
            });
          }
        });
  }

  public AppState(List<CycleData> cycles) {
    this.cycles = cycles;
  }

  public static class CycleData {
    public final Cycle cycle;
    public final Set<EntrySet> entrySets;

    public CycleData(Cycle cycle, Collection<EntrySet> entrySets) {
      this.cycle = cycle;
      this.entrySets = ImmutableSet.copyOf(entrySets);
    }
  }

  public static class EntrySet {
    public final ObservationEntry mObservationEntry;
    public final WellnessEntry mWellnessEntry;
    public final SymptomEntry mSymptomEntry;

    public EntrySet(ObservationEntry observationEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry) {
      mObservationEntry = observationEntry;
      mWellnessEntry = wellnessEntry;
      mSymptomEntry = symptomEntry;
    }
  }
}
