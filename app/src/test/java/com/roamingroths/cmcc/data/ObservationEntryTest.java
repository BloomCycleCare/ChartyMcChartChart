package com.roamingroths.cmcc.data;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ObservationEntryTest {

  private static final ImmutableList<String> OBSERVATION_STRS = ImmutableList.of(
      "H", "VL10CKAD", "0X1", "10WLAD"
  );

  @Test
  public void encryptDecrypt() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    PublicKey pubKey = keyPair.getPublic();
    PrivateKey privKey = keyPair.getPrivate();
  }
}