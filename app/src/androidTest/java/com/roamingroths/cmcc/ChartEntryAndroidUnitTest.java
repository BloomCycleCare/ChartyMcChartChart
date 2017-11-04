package com.roamingroths.cmcc;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.crypto.PbeCryptoUtil;
import com.roamingroths.cmcc.crypto.RsaCryptoUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChartEntryAndroidUnitTest {

  @Test
  public void testPbeWrappedRsa() throws Exception {
    String password = "password";

    KeyPair originalKeyPair = RsaCryptoUtil.createKeyPair();

    String encryptedPrivateKey =
        PbeCryptoUtil.wrapPrivateKey(password, originalKeyPair.getPrivate());
    PrivateKey decryptedPrivateKey = PbeCryptoUtil.unwrapPrivateKey(password, encryptedPrivateKey);

    assertEquals(decryptedPrivateKey, originalKeyPair.getPrivate());
  }

  @Test
  public void testRsaPublicKeyStorage() throws Exception {
    PublicKey originalPublicKey = RsaCryptoUtil.createKeyPair().getPublic();

    PublicKey parsedPublicKey =
        RsaCryptoUtil.parsePublicKey(RsaCryptoUtil.serializePublicKey(originalPublicKey).call());
    assertEquals(parsedPublicKey, originalPublicKey);
  }

  @Test
  public void testRsaPrivateKeyStorage() throws Exception {
    PrivateKey originalPrivateKey = RsaCryptoUtil.createKeyPair().getPrivate();

    PrivateKey parsedPrivateKey =
        RsaCryptoUtil.parsePrivateKey(RsaCryptoUtil.serializePrivateKey(originalPrivateKey));
    if (!parsedPrivateKey.equals(originalPrivateKey)) {
      throw new Exception();
    }
  }

  @Test
  public void testAesEncryptDecrypt() throws Exception {
    String secret = "the secret";
    SecretKey key = AesCryptoUtil.createKey();

    String encryptedSecret = AesCryptoUtil.encrypt(key, secret);
    String decryptedSecret = AesCryptoUtil.decrypt(key, encryptedSecret);
    if (!decryptedSecret.equals(secret)) {
      throw new Exception(decryptedSecret + " != " + secret);
    }
  }

  @Test
  public void testAesKeyStorage() throws Exception {
    SecretKey key = AesCryptoUtil.createKey();

    if (!key.equals(AesCryptoUtil.parseKey(AesCryptoUtil.serializeKey(key)))) {
      throw new Exception();
    }
  }
}
