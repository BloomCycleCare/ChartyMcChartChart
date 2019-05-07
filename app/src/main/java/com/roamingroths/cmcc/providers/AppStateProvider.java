package com.roamingroths.cmcc.providers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.roamingroths.cmcc.logic.AppState;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.logic.profile.Profile;
import com.roamingroths.cmcc.utils.GsonUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 1/16/18.
 */

public class AppStateProvider {

  private final Gson mGson= GsonUtil.getGsonInstance();
  private final ProfileProvider mProfileProvider;
  private final CycleProvider mCycleProvider;
  private final ChartEntryProvider mChartEntryProvider;
  private final FirebaseUser mUser;

  public AppStateProvider(ProfileProvider mProfileProvider, CycleProvider mCycleProvider, ChartEntryProvider mChartEntryProvider, FirebaseUser mUser) {
    this.mProfileProvider = mProfileProvider;
    this.mCycleProvider = mCycleProvider;
    this.mChartEntryProvider = mChartEntryProvider;
    this.mUser = mUser;
  }

  public Single<AppState> fetch(Context context) {
    Single<Profile> fetchProfile = mProfileProvider.getProfile(context);
    Single<List<AppState.CycleData>> fetchCycleDatas = fetchCycleDatas().toList();
    return Single.zip(fetchCycleDatas, fetchProfile, new BiFunction<List<AppState.CycleData>, Profile, AppState>() {
      @Override
      public AppState apply(List<AppState.CycleData> cycleData, Profile profile) throws Exception {
        return new AppState(cycleData, profile);
      }
    });
  }

  public Single<String> fetchAsJson(Context context) {
    return fetch(context).map(new Function<AppState, String>() {
      @Override
      public String apply(AppState appState) throws Exception {
        return mGson.toJson(appState);
      }
    });
  }

  public Completable parseAndPushToRemote(Callable<InputStream> stream) {
    return Single.fromCallable(stream)
        .map(parseFile())
        .flatMapCompletable(new Function<AppState, CompletableSource>() {
          @Override
          public CompletableSource apply(AppState appState) throws Exception {
            for (AppState.CycleData cycleData : appState.cycles) {
              cycleData.updateKeys();
            }
            Completable putProfile = mProfileProvider.putProfile(appState.profile);
            Completable putCycleDatas = mCycleProvider.putCycleDatas(mUser, appState.cycles);
            return Completable.mergeArray(putProfile, putCycleDatas);
          }
        });
  }

  private Function<InputStream, AppState> parseFile() {
    return new Function<InputStream, AppState>() {
      @Override
      public AppState apply(InputStream stream) throws Exception {
        Log.v("AppState", "Reading file");
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
          total.append(line);
        }
        return GsonUtil.getGsonInstance().fromJson(total.toString(), AppState.class);
      }
    };
  }

  private Observable<AppState.CycleData> fetchCycleDatas() {
    return mCycleProvider.getAllCycles(mUser).flatMap(new Function<Cycle, ObservableSource<AppState.CycleData>>() {
      @Override
      public ObservableSource<AppState.CycleData> apply(final Cycle cycle) throws Exception {
        return mChartEntryProvider.getEntries(cycle).toList().map(new Function<List<ChartEntry>, AppState.CycleData>() {
          @Override
          public AppState.CycleData apply(List<ChartEntry> entries) throws Exception {
            return new AppState.CycleData(cycle, entries);
          }
        }).toObservable();
      }
    });
  }
}
