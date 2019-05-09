package com.roamingroths.cmcc.application;

import android.app.Application;
import android.content.Context;
import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.room.migration.Migration;

import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.logic.profile.Profile;
import com.roamingroths.cmcc.providers.AppStateProvider;
import com.roamingroths.cmcc.providers.ChartEntryProvider;
import com.roamingroths.cmcc.providers.CryptoProvider;
import com.roamingroths.cmcc.providers.CycleEntryProvider;
import com.roamingroths.cmcc.providers.CycleProvider;
import com.roamingroths.cmcc.providers.GoalProvider;
import com.roamingroths.cmcc.providers.KeyProvider;
import com.roamingroths.cmcc.providers.ProfileProvider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 5/15/17.
 */

public class MyApplication extends Application {

  private static final boolean DEBUG = true;
  private static final String TAG = MyApplication.class.getSimpleName();

  private static Maybe<CryptoUtil> mCryptoUtilFromKeyStore;
  private static Single<Providers> mProviders;
  private static ViewModelFactory mViewModelFactory;

  private AppDatabase mDB;

  @Override
  public void onCreate() {
    super.onCreate();
    if (DEBUG) Log.v(TAG, "onCreate()");
    FirebaseApp.initializeApp(this);
    //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    Security.addProvider(new BouncyCastleProvider());
    PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);

    Migration[] migrations = new Migration[AppDatabase.MIGRATIONS.size()];
    AppDatabase.MIGRATIONS.toArray(migrations);
    mDB = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "room-db")
        .addMigrations(migrations)
        .fallbackToDestructiveMigration()  // I'm sure this will bite me in the end...
        .build();

    mCryptoUtilFromKeyStore = CryptoProvider.forDb(FirebaseDatabase.getInstance()).tryCreateFromKeyStore().cache();
    mProviders = mCryptoUtilFromKeyStore.flatMapSingle(new Function<CryptoUtil, SingleSource<? extends Providers>>() {
      @Override
      public SingleSource<? extends Providers> apply(CryptoUtil cryptoUtil) throws Exception {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Providers providers = new Providers(FirebaseDatabase.getInstance(), mDB, cryptoUtil, user);
        return providers.initialize(user, getApplicationContext()).andThen(Single.just(providers));
      }
    }).cache();

    //FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
  }

  public AppDatabase db() {
    return mDB;
  }

  public static MyApplication cast(Application app) {
    return (MyApplication) app;
  }

  public Completable initProviders(final FirebaseUser user, Maybe<String> phoneNumber, final Context context) {
    Log.i(TAG, "Creating crypto util for user: " + user.getUid());
    Single<CryptoUtil> cryptoUtil =
        CryptoProvider.forDb(FirebaseDatabase.getInstance()).createCryptoUtil(user, phoneNumber);
    return cryptoUtil.flatMapCompletable(cryptoUtil1 -> {
      Log.i(TAG, "Crypto initialization complete");
      final Providers providers = new Providers(FirebaseDatabase.getInstance(), mDB, cryptoUtil1, user);
      return providers.initialize(user, context).doOnComplete(() -> {
        Log.i(TAG, "Provider initialization complete");
        mViewModelFactory = new ViewModelFactory(providers);
        mProviders = Single.just(providers).cache();
      });
    });
  }

  public static ViewModelFactory viewModelFactory() {
    return mViewModelFactory;
  }

  public static Maybe<FirebaseUser> getCurrentUser() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      return Maybe.empty();
    } else {
      return Maybe.just(user);
    }
  }

  public static Single<CycleProvider> cycleProvider() {
    return mProviders.map(new Function<Providers, CycleProvider>() {
      @Override
      public CycleProvider apply(Providers providers) throws Exception {
        return providers.mCycleProvider;
      }
    });
  }

  public static Single<KeyProvider> keyProvider() {
    return mProviders.map(new Function<Providers, KeyProvider>() {
      @Override
      public KeyProvider apply(Providers providers) throws Exception {
        return providers.mKeyProvider;
      }
    });
  }

  public static Single<CycleEntryProvider> cycleEntryProvider() {
    return mProviders.map(new Function<Providers, CycleEntryProvider>() {
      @Override
      public CycleEntryProvider apply(Providers providers) throws Exception {
        return providers.mCycleEntryProvider;
      }
    });
  }

  public static Single<ChartEntryProvider> chartEntryProvider() {
    return mProviders.map(new Function<Providers, ChartEntryProvider>() {
      @Override
      public ChartEntryProvider apply(Providers providers) throws Exception {
        return providers.mChartEntryProvider;
      }
    });
  }

  public static Single<ProfileProvider> profileProvider() {
    return mProviders.map(new Function<Providers, ProfileProvider>() {
      @Override
      public ProfileProvider apply(Providers providers) throws Exception {
        return providers.mProfileProvider;
      }
    });
  }

  public static Single<AppStateProvider> appStateProvider() {
    return mProviders.map(new Function<Providers, AppStateProvider>() {
      @Override
      public AppStateProvider apply(Providers providers) throws Exception {
        return providers.mAppStateProvider;
      }
    });
  }

  public static class Providers {
    final CycleProvider mCycleProvider;
    final KeyProvider mKeyProvider;
    final CycleEntryProvider mCycleEntryProvider;
    final ChartEntryProvider mChartEntryProvider;
    final ProfileProvider mProfileProvider;
    final AppStateProvider mAppStateProvider;
    final GoalProvider mGoalProvider;

    Providers(FirebaseDatabase db, AppDatabase localDB, CryptoUtil cryptoUtil, FirebaseUser currentUser) {
      mKeyProvider = new KeyProvider(cryptoUtil, db, currentUser);
      mProfileProvider = new ProfileProvider(db, currentUser, cryptoUtil, mKeyProvider);
      mChartEntryProvider = new ChartEntryProvider(db, localDB, cryptoUtil);
      mCycleProvider = new CycleProvider(db, localDB, mKeyProvider, mChartEntryProvider);
      mCycleEntryProvider = new CycleEntryProvider(mChartEntryProvider);
      mAppStateProvider = new AppStateProvider(mProfileProvider, mCycleProvider, mChartEntryProvider, currentUser);
      mGoalProvider = new GoalProvider(db, currentUser, cryptoUtil, mKeyProvider);
    }

    public Completable initialize(FirebaseUser user, Context context) {
      if (DEBUG) Log.v(TAG, "Initializing providers");
      return Completable
          .mergeArray(
              mCycleProvider.initCache(user),
              mGoalProvider.init(),
              mProfileProvider.init(context, Profile.SystemGoal.AVOID))
          .andThen(mChartEntryProvider.initCache(mCycleProvider.getAllCycles(user), 2));
    }
  }
}
