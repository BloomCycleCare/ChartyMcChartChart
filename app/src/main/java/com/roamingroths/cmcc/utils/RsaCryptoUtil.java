package com.roamingroths.cmcc.utils;

import android.util.Base64;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

/**
 * Created by parkeroth on 8/27/17.
 */

public class RsaCryptoUtil {

  private static final String TRANSFORM = "RSA/ECB/PKCS1Padding";
  private static final int KEY_SIZE = 2048;

  public static KeyPair createKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(KEY_SIZE);
    return kpg.genKeyPair();
  }

  public static String encrypt(PublicKey publicKey, String rawText) throws Exception {
    Cipher input = Cipher.getInstance(TRANSFORM);
    input.init(Cipher.ENCRYPT_MODE, publicKey);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CipherOutputStream cipherOutputStream = new CipherOutputStream(
        outputStream, input);
    cipherOutputStream.write(rawText.getBytes("UTF-8"));
    cipherOutputStream.close();

    byte[] vals = outputStream.toByteArray();
    return Base64.encodeToString(vals, Base64.DEFAULT);
  }

  public static String decrypt(PrivateKey privateKey, String cipherText) throws Exception {
    Cipher output = Cipher.getInstance(TRANSFORM);
    output.init(Cipher.DECRYPT_MODE, privateKey);

    CipherInputStream cipherInputStream = new CipherInputStream(
        new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
    ArrayList<Byte> values = new ArrayList<>();
    int nextByte;
    while ((nextByte = cipherInputStream.read()) != -1) {
      values.add((byte) nextByte);
    }

    byte[] bytes = new byte[values.size()];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = values.get(i).byteValue();
    }

    return new String(bytes, 0, bytes.length, "UTF-8");
  }

  public static Certificate createCertificate(KeyPair keyPair) throws Exception {
    X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

    X500Principal dnName = new X500Principal("cn=cmcc");

    certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
    certGen.setSubjectDN(new X509Name("dc=name"));
    certGen.setIssuerDN(dnName);
    certGen.setNotBefore(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
    certGen.setNotAfter(new Date(System.currentTimeMillis() + 100 * 365 * 24 * 60 * 60 * 1000));
    certGen.setPublicKey(keyPair.getPublic());
    certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

    return certGen.generate(keyPair.getPrivate(), "BC");
  }

  public static String serializePublicKey(PublicKey key) throws Exception {
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key.getEncoded());
    return Base64.encodeToString(keySpec.getEncoded(), Base64.DEFAULT);
  }

  public static PublicKey parsePublicKey(String keyStr) throws Exception {
    byte[] encodedKey = Base64.decode(keyStr, Base64.DEFAULT);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
    KeyFactory factory = KeyFactory.getInstance("RSA");
    return factory.generatePublic(keySpec);
  }

  public static String serializePrivateKey(PrivateKey key) throws Exception {
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key.getEncoded());
    return Base64.encodeToString(keySpec.getEncoded(), Base64.DEFAULT);
  }

  public static PrivateKey parsePrivateKey(String keyStr) throws Exception {
    byte[] encodedKey = Base64.decode(keyStr, Base64.DEFAULT);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
    KeyFactory factory = KeyFactory.getInstance("RSA");
    return factory.generatePrivate(keySpec);
  }
}
