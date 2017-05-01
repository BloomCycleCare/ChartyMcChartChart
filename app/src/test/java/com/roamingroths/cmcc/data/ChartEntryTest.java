package com.roamingroths.cmcc.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.data.DischargeSummary.DischargeType;
import com.roamingroths.cmcc.data.DischargeSummary.MucusModifier;

import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ChartEntryTest {

  private static final ImmutableList<String> OBSERVATION_STRS = ImmutableList.of(
      "H", "VL10CKAD", "0X1", "10WLAD"
  );

  @Test
  public void encryptDecrypt() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privKey = (RSAPrivateKey) keyPair.getPrivate();

    for (String observationStr : OBSERVATION_STRS) {
      ChartEntry entry =
          new ChartEntry(new Date(), Observation.fromString(observationStr), true, false);
      assertEquals(entry, ChartEntry.fromEncryptedText(entry.toEncryptedString(pubKey), privKey));
    }
  }
}