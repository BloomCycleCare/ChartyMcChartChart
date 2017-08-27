package com.roamingroths.cmcc;

import com.roamingroths.cmcc.utils.AesCryptoUtil;
import com.roamingroths.cmcc.utils.PbeCryptoUtil;
import com.roamingroths.cmcc.utils.RsaCryptoUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 7/1/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class CryptoTest {

  @Test
  public void testPbeWrappedRsa() throws Exception {
    String password = "password";

    KeyPair originalKeyPair = RsaCryptoUtil.createKeyPair();

    String encryptedPrivateKey =
        PbeCryptoUtil.wrapPrivateKey(password, originalKeyPair.getPrivate());
    PrivateKey decryptedPrivateKey = PbeCryptoUtil.unwrapPrivateKey(password, encryptedPrivateKey);

    if (!decryptedPrivateKey.equals(originalKeyPair.getPrivate())) {
      throw new Exception();
    }
  }

  @Test
  public void testRsaPublicKeyStorage() throws Exception {
    PublicKey originalPublicKey = RsaCryptoUtil.createKeyPair().getPublic();

    PublicKey parsedPublicKey =
        RsaCryptoUtil.parsePublicKey(RsaCryptoUtil.serializePublicKey(originalPublicKey));
    if (!parsedPublicKey.equals(originalPublicKey)) {
      throw new Exception();
    }
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
