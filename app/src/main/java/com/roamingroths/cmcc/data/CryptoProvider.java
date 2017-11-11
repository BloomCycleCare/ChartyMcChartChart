package com.roamingroths.cmcc.data;

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

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
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
    try {
      KeyStore ks = KeyStore.getInstance(KEY_STORE_ALIAS);
      return getUserKeyPair(user, phoneNumber, ks)
          .map(new Function<KeyPair, CryptoUtil>() {
            @Override
            public CryptoUtil apply(@NonNull KeyPair keyPair) throws Exception {
              return new CachingCryptoUtil(keyPair);
            }
          });
    } catch (KeyStoreException kse) {
      return Single.error(kse);
    }
  }

  private Single<KeyPair> getUserKeyPair(FirebaseUser user, Maybe<String> phoneNumber, KeyStore ks) {
    Maybe<String> cachedPhoneNumber = phoneNumber.cache();
    return getKeyPairFromKeyStore(ks)
        .switchIfEmpty(getKeyPairFromDb(user, cachedPhoneNumber).doOnSuccess(storeKeyInKeystore(ks)))
        .switchIfEmpty(createAndStoreKeyPair(user, cachedPhoneNumber).doOnSuccess(storeKeyInKeystore(ks)))
        .toSingle();
  }

  private Maybe<KeyPair> createAndStoreKeyPair(final FirebaseUser user, Maybe<String> phoneNumber) {
    try {
      Log.v(TAG, "Creating KeyPair");
      final KeyPair keyPair = KeyUtil.createKeyPair();
      return phoneNumber.flatMapCompletable(new Function<String, CompletableSource>() {
        @Override
        public CompletableSource apply(@NonNull String phoneNumber) throws Exception {
          Log.v(TAG, "Storing KeyPair in DB");
          Map<String, Object> updates = new HashMap<>();
          updates.put("pub-key", KeyUtil.serializePublicKey(keyPair.getPublic()));
          updates.put("private-key", KeyUtil.wrapKey(keyPair.getPrivate(), phoneNumber));
          return RxFirebaseDatabase.updateChildren(
              mDb.getReference("users").child(user.getUid()), updates);
        }
      }).andThen(Maybe.just(keyPair));
    } catch (Exception e) {
      return Maybe.error(e);
    }
  }

  private Consumer<KeyPair> storeKeyInKeystore(final KeyStore ks) {
    return new Consumer<KeyPair>() {
      @Override
      public void accept(@NonNull KeyPair keyPair) throws Exception {
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
        }
        emitter.onComplete();
      }
    });
  }

  private Maybe<KeyPair> getKeyPairFromDb(FirebaseUser user, Maybe<String> phoneNumber) {
    Maybe<DataSnapshot> databaseResult =
        RxFirebaseDatabase.observeSingleValueEvent(mDb.getReference("users").child(user.getUid()));
    return Maybe.zip(databaseResult, phoneNumber, new BiFunction<DataSnapshot, String, KeyPair>() {
      @Override
      public KeyPair apply(@NonNull DataSnapshot snapshot, @NonNull String phoneNumber) throws Exception {
        Log.v(TAG, "Loading KeyPair from DB");
        String publicKeyStr = snapshot.child("pub-key").getValue(String.class);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(publicKeyStr));
        String privateKeyStr = snapshot.child("private-key").getValue(String.class);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(privateKeyStr));
        return KeyUtil.parseKeyPair(publicKeyStr, privateKeyStr, phoneNumber);
      }
    });
  }
}
