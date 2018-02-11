package com.roamingroths.cmcc.providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.profile.Profile;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 1/13/18.
 */

public class ProfileProvider {

  private static final boolean DEBUG = true;
  private static final String TAG = ProfileProvider.class.getSimpleName();
  private static final LocalDate EPOCH = new LocalDate(1970, 1, 1);

  private enum PrefKey {
    preferred_name, achieve_avoid_key, date_of_birth_key, height_key, weight_key
  }

  private final FirebaseDatabase mDb;
  private final FirebaseUser mUser;
  private final CryptoUtil mCryptoUtil;
  private final KeyProvider mKeyProvider;

  public ProfileProvider(FirebaseDatabase mDb, FirebaseUser mUser, CryptoUtil mCryptoUtil, KeyProvider mKeyProvider) {
    this.mDb = mDb;
    this.mUser = mUser;
    this.mCryptoUtil = mCryptoUtil;
    this.mKeyProvider = mKeyProvider;
  }

  public Completable init(final Context context, Profile.SystemGoal goal) {
    Log.i(TAG, "Initializing.");
    return getProfileFromRemote().switchIfEmpty(create(context, goal).toMaybe()).ignoreElement();
  }

  public Single<Profile> create(final Context context, final Profile.SystemGoal goal) {
    Action setPrefs = new Action() {
      @Override
      public void run() throws Exception {
        Log.i(TAG, "Setting preference values.");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PrefKey.preferred_name.name(), mUser.getDisplayName());
        editor.putString(PrefKey.achieve_avoid_key.name(), goal.name());
        editor.putInt(PrefKey.height_key.name(), 100);
        editor.putInt(PrefKey.weight_key.name(), 50);
        editor.commit();
      }
    };

    return Completable.fromAction(setPrefs).andThen(mKeyProvider.createAndStoreProfileKey().flatMap(new Function<SecretKey, Single<Profile>>() {
      @Override
      public Single<Profile> apply(SecretKey key) throws Exception {
        Log.i(TAG, "Storing profile in DB.");
        Profile profile = getProfileFromPreferences(context, key);
        return putProfile(profile).andThen(Single.just(profile));
      }
    }));
  }

  public Completable maybeUpdateProfile(final SharedPreferences prefs, String updatedKey) {
    for (PrefKey key : PrefKey.values()) {
      if (updatedKey.equals(key.name())) {
        return mKeyProvider.getProfileKey().flatMapCompletable(new Function<SecretKey, CompletableSource>() {
          @Override
          public CompletableSource apply(SecretKey secretKey) throws Exception {
            return putProfile(profileFromPrefs(prefs, secretKey));
          }
        });
      }
    }
    return Completable.complete();
  }

  public Completable putProfile(final Profile profile) {
    Single<Profile> profileWithKey = profile.hasKey() ? Single.just(profile) :
        mKeyProvider.createAndStoreProfileKey().map(new Function<SecretKey, Profile>() {
          @Override
          public Profile apply(SecretKey secretKey) throws Exception {
            profile.swapKey(secretKey);
            return profile;
          }
        });
    return profileWithKey.flatMap(mCryptoUtil.encrypt()).flatMapCompletable(new Function<String, CompletableSource>() {
      @Override
      public CompletableSource apply(String encryptedProfile) throws Exception {
        return RxFirebaseDatabase.setValue(getProfileReference(), encryptedProfile);
      }
    });
  }

  private DatabaseReference getProfileReference() {
    return mDb.getReference(String.format("profiles/%s", mUser.getUid()));
  }

  public static Function<ProfileProvider, Single<Profile>> getProfileFn(final Context context) {
    return new Function<ProfileProvider, Single<Profile>>() {
      @Override
      public Single<Profile> apply(ProfileProvider profileProvider) throws Exception {
        return profileProvider.getProfile(context);
      }
    };
  }

  public Single<Profile> getProfile(final Context context) {
    return mKeyProvider.getProfileKey().toSingle().map(new Function<SecretKey, Profile>() {
      @Override
      public Profile apply(SecretKey key) throws Exception {
        return getProfileFromPreferences(context, key);
      }
    });
  }

  private Maybe<Profile> getProfileFromRemote() {
    Maybe<SecretKey> key = mKeyProvider.getProfileKey();
    Maybe<String> encryptedProfile = RxFirebaseDatabase.observeSingleValueEvent(getProfileReference(), String.class);
    return Maybe.merge(Maybe.zip(key, encryptedProfile, new BiFunction<SecretKey, String, MaybeSource<Profile>>() {
      @Override
      public MaybeSource<Profile> apply(SecretKey secretKey, String s) throws Exception {
        if (DEBUG) Log.v(TAG, "Decrypting profile");
        return mCryptoUtil.decrypt(s, secretKey, Profile.class).toMaybe();
      }
    }));
  }

  private Profile getProfileFromPreferences(Context context, SecretKey key) {
    return profileFromPrefs(PreferenceManager.getDefaultSharedPreferences(context), key);
  }

  private static Profile profileFromPrefs(SharedPreferences prefs, SecretKey key) {
    Profile profile = new Profile(key);
    profile.mPreferredName = prefs.getString(PrefKey.preferred_name.name(), "Not Specified");
    profile.mGoal = Profile.SystemGoal.valueOf(
        prefs.getString(PrefKey.achieve_avoid_key.name(), Profile.SystemGoal.UNSPECIFIED.name()).toUpperCase());
    profile.heightCm = prefs.getInt(PrefKey.height_key.name(), -1);
    profile.weightKg = prefs.getInt(PrefKey.weight_key.name(), -1);
    profile.mDateOfBirth = EPOCH.plusDays(prefs.getInt(PrefKey.date_of_birth_key.name(), -1));
    return profile;
  }
}
