package com.roamingroths.cmcc.providers;

import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.models.ChartEntryList;
import com.roamingroths.cmcc.data.entities.Cycle;

import org.joda.time.LocalDate;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 12/4/17.
 */

public class CycleEntryProvider {

  private static boolean DEBUG = true;
  private static String TAG = CycleEntryProvider.class.getSimpleName();

  private final ChartEntryProvider mChartEntryProvider;

  public CycleEntryProvider(ChartEntryProvider mChartEntryProvider) {
    this.mChartEntryProvider = mChartEntryProvider;
  }

  public Observable<ChartEntryList> getChartEntryLists(Set<Cycle> cycles, final Preferences prefs) {
    return Observable.fromIterable(cycles)
        .flatMap(new Function<Cycle, ObservableSource<ChartEntryList>>() {
          @Override
          public ObservableSource<ChartEntryList> apply(Cycle cycle) throws Exception {
            ChartEntryList list = ChartEntryList.builder(cycle, prefs).build();
            return mChartEntryProvider
                .getEntries(cycle)
                .toList()
                .map(fillList(list))
                .toObservable();
          }
        })
        .sorted(new Comparator<ChartEntryList>() {
          @Override
          public int compare(ChartEntryList l, ChartEntryList r) {
            LocalDate lDate = l.mCurrentCycle.startDate;
            LocalDate rDate = r.mCurrentCycle.startDate;
            return lDate.compareTo(rDate);
          }
        });
  }

  private Function<List<ChartEntry>, ChartEntryList> fillList(final ChartEntryList list) {
    return new Function<List<ChartEntry>, ChartEntryList>() {
      @Override
      public ChartEntryList apply(List<ChartEntry> chartEntries) throws Exception {
        for (ChartEntry entry : chartEntries) {
          list.addEntry(entry);
        }
        return list;
      }
    };
  }
}
