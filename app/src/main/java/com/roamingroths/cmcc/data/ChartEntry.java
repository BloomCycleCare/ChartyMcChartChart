package com.roamingroths.cmcc.data;

import org.apache.commons.codec.binary.Base64;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ChartEntry implements Parcelable {

  private static final Joiner ON_COMMA = Joiner.on(',');
  private static final SimpleDateFormat WIRE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  public Date date;
  public Observation observation;
  public boolean peakDay;
  public boolean intercourse;

  public ChartEntry() {
    // Required for DataSnapshot.getValue(ChartEntry.class)
  }

  public ChartEntry(Date date, Observation observation, boolean peakDay, boolean intercourse) {
    this.date = date;
    this.observation = Preconditions.checkNotNull(observation);
    this.peakDay = peakDay;
    this.intercourse = intercourse;
  }

  public ChartEntry(Parcel in) {
    try {
      date = parseDate(in.readString());
    } catch (ParseException pe) {
      // TODO: Handle ParseException better
      throw new IllegalStateException(pe);
    }
    observation = in.readParcelable(Observation.class.getClassLoader());
    peakDay = in.readByte() != 0;
    intercourse = in.readByte() != 0;
  }

  public static ChartEntry fromEncryptedText(String encryptedText, RSAPrivateKey privateKey)
      throws Exception {
    Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    output.init(Cipher.DECRYPT_MODE, privateKey);

    String cipherText = encryptedText;
    CipherInputStream cipherInputStream = new CipherInputStream(
        new ByteArrayInputStream(Base64.decodeBase64(cipherText)), output);
    ArrayList<Byte> values = new ArrayList<>();
    int nextByte;
    while ((nextByte = cipherInputStream.read()) != -1) {
      values.add((byte)nextByte);
    }

    byte[] bytes = new byte[values.size()];
    for(int i = 0; i < bytes.length; i++) {
      bytes[i] = values.get(i).byteValue();
    }

    String decryptedText = new String(bytes, 0, bytes.length, "UTF-8");
    return new Gson().fromJson(decryptedText, ChartEntry.class);
  }

  public static final Date parseDate(String dateStr) throws ParseException {
    return WIRE_DATE_FORMAT.parse(dateStr);
  }

  public static final Creator<ChartEntry> CREATOR = new Creator<ChartEntry>() {
    @Override
    public ChartEntry createFromParcel(Parcel in) {
      return new ChartEntry(in);
    }

    @Override
    public ChartEntry[] newArray(int size) {
      return new ChartEntry[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(WIRE_DATE_FORMAT.format(date));
    dest.writeParcelable(observation, flags);
    dest.writeByte((byte) (peakDay ? 1 : 0));
    dest.writeByte((byte) (intercourse ? 1 : 0));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ChartEntry) {
      ChartEntry that = (ChartEntry) o;
      return this.observation.equals(that.observation) &&
          this.peakDay == that.peakDay &&
          this.intercourse == that.intercourse;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(observation, peakDay, intercourse);
  }

  public String toEncryptedString(RSAPublicKey publicKey) throws Exception {
    String initialText = new Gson().toJson(this);
    Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    input.init(Cipher.ENCRYPT_MODE, publicKey);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CipherOutputStream cipherOutputStream = new CipherOutputStream(
        outputStream, input);
    cipherOutputStream.write(initialText.getBytes("UTF-8"));
    cipherOutputStream.close();

    byte [] vals = outputStream.toByteArray();
    return Base64.encodeBase64String(vals);
  }
}
