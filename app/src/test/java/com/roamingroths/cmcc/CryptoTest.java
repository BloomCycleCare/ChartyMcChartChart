package com.roamingroths.cmcc;

import com.roamingroths.cmcc.utils.CryptoUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 7/1/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class CryptoTest {

  @Test
  public void testB1A() throws Exception {
    SecretKey key = CryptoUtil.createSecretKey();
  }
}
