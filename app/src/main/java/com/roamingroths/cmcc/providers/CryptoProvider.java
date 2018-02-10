package com.roamingroths.cmcc.providers;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CachingCryptoUtil;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.crypto.KeyUtil;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;


/**
 * Created by parkeroth on 10/22/17.
 */

public class CryptoProvider {

  private static final String TAG = CryptoProvider.class.getSimpleName();
  private static final String PRIVATE_KEY_ALIAS = "PersonalPrivateKey";
  private static final String KEY_STORE_ALIAS = "AndroidKeyStore";

  private final FirebaseDatabase mDb;

  public CryptoProvider(FirebaseDatabase db) {
    mDb = db;
  }

  public static CryptoProvider forDb(FirebaseDatabase db) {
    return new CryptoProvider(db);
  }

  public Single<CryptoUtil> createCryptoUtil(FirebaseUser user, Maybe<String> phoneNumber) {
    Maybe<String> cachedPhoneNumber = phoneNumber.cache();
    return tryCreateFromKeyStore()
        .switchIfEmpty(tryCreateFromDb(user, cachedPhoneNumber))
        .switchIfEmpty(createNewAndStore(user, cachedPhoneNumber).toMaybe())
        .toSingle();
  }

  public Maybe<CryptoUtil> tryCreateFromKeyStore() {
    return Single.fromCallable(getKeyStore()).flatMapMaybe(new Function<KeyStore, MaybeSource<? extends KeyPair>>() {
      @Override
      public MaybeSource<? extends KeyPair> apply(KeyStore keyStore) throws Exception {
        return getKeyPairFromKeyStore(keyStore);
      }
    }).map(createUtil());
  }

  private Maybe<CryptoUtil> tryCreateFromDb(FirebaseUser user, Maybe<String> phoneNumber) {
    return getKeyPairFromDb(user, phoneNumber)
        .doOnSuccess(storeKeyInKeystore())
        .map(createUtil());
  }

  private Single<CryptoUtil> createNewAndStore(final FirebaseUser user, Maybe<String> phoneNumber) {
    return phoneNumber.flatMapSingle(new Function<String, SingleSource<? extends KeyPair>>() {
      @Override
      public SingleSource<KeyPair> apply(@NonNull String phoneNumber) throws Exception {
        Log.v(TAG, "Creating new KeyPair");
        KeyPair keyPair = KeyUtil.createKeyPair();
        Log.v(TAG, "Storing KeyPair in DB");
        Map<String, Object> updates = new HashMap<>();
        updates.put("pub-key", KeyUtil.serializePublicKey(keyPair.getPublic()));
        updates.put("private-key", KeyUtil.wrapKey(keyPair.getPrivate(), phoneNumber));
        return RxFirebaseDatabase.updateChildren(
            mDb.getReference("keys").child(user.getUid()), updates).andThen(Single.just(keyPair));
      }
    }).doOnSuccess(storeKeyInKeystore()).map(createUtil());
  }

  private Callable<KeyStore> getKeyStore() {
    return new Callable<KeyStore>() {
      @Override
      public KeyStore call() throws Exception {
        return KeyStore.getInstance(KEY_STORE_ALIAS);
      }
    };
  }

  private static Function<KeyPair, CryptoUtil> createUtil() {
    return new Function<KeyPair, CryptoUtil>() {
      @Override
      public CryptoUtil apply(KeyPair keyPair) throws Exception {
        return new CachingCryptoUtil(keyPair);
      }
    };
  }

  private Consumer<KeyPair> storeKeyInKeystore() {
    return new Consumer<KeyPair>() {
      public void accept(@NonNull KeyPair keyPair) throws Exception {
        KeyStore ks = getKeyStore().call();
        Log.v(TAG, "Storing KeyPair in KeyStore");
        ks.load(null);
        Certificate cert = KeyUtil.createCertificate(keyPair);
        ks.setKeyEntry(
            PRIVATE_KEY_ALIAS, keyPair.getPrivate(), null, new Certificate[]{cert});
      }
    };
  }

  private Maybe<KeyPair> getKeyPairFromKeyStore(final KeyStore keyStore) {
    return Maybe.create(new MaybeOnSubscribe<KeyPair>() {
      @Override
      public void subscribe(@NonNull MaybeEmitter<KeyPair> emitter) throws Exception {
        keyStore.load(null);
        if (keyStore.containsAlias(PRIVATE_KEY_ALIAS)) {
          Log.v(TAG, "Found KeyPair in KeyStore");
          KeyStore.PrivateKeyEntry entry =
              (KeyStore.PrivateKeyEntry) keyStore.getEntry(PRIVATE_KEY_ALIAS, null);
          emitter.onSuccess(
              new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey()));
        } else {
          Log.v(TAG, "No KeyPair in KeyStore");
        }
        emitter.onComplete();
      }
    });
  }

  private Maybe<KeyPair> getKeyPairFromDb(FirebaseUser user, Maybe<String> phoneNumber) {
    return RxFirebaseDatabase.observeSingleValueEvent(mDb.getReference("keys").child(user.getUid()))
        .zipWith(phoneNumber, new BiFunction<DataSnapshot, String, KeyPair>() {
          @Override
          public KeyPair apply(DataSnapshot snapshot, String phoneNumber) throws Exception {
            Log.v(TAG, "Found KeyPair in DB");
            String publicKeyStr = snapshot.child("pub-key").getValue(String.class);
            Preconditions.checkArgument(!Strings.isNullOrEmpty(publicKeyStr));
            String privateKeyStr = snapshot.child("private-key").getValue(String.class);
            Preconditions.checkArgument(!Strings.isNullOrEmpty(privateKeyStr));
            return KeyUtil.parseKeyPair(publicKeyStr, privateKeyStr, phoneNumber);
          }
        })
        .switchIfEmpty(Maybe.create(new MaybeOnSubscribe<KeyPair>() {
          @Override
          public void subscribe(MaybeEmitter<KeyPair> e) throws Exception {
            Log.v(TAG, "FOO");
            e.onComplete();
          }
        }))
        .doOnSubscribe(new Consumer<Disposable>() {
          @Override
          public void accept(Disposable disposable) throws Exception {
            Log.v(TAG, "Loading KeyPair from DB");
          }
        });
  }
}
