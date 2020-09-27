package com.bloomcyclecare.cmcc.data;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.StringUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.BitSet;
import java.util.zip.Deflater;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
  @Test
  public void addition_isCorrect() {
    BitSet bitSet = new BitSet(43);
    bitSet.set(0, true);
    bitSet.set(31, true);
    bitSet.set(32, true);
    bitSet.set(41, true);
    bitSet.set(42, true);

    byte[] out = bitSet.toByteArray();
    String str = Base64.getUrlEncoder().withoutPadding().encodeToString(out);

    Deflater compressor = new Deflater();
    compressor.setLevel(Deflater.BEST_COMPRESSION);
    compressor.finish();

    ByteArrayOutputStream bos = new ByteArrayOutputStream(out.length);
    byte[] buf = new byte[1024];
    while (!compressor.finished()) {
      int count = compressor.deflate(buf);
      bos.write(buf, 0, count);
    }
    try {
      bos.close();
    } catch (IOException e) {}
    // Get the compressed data
    byte[] compressedData = bos.toByteArray();


    byte[] in = Base64.getUrlDecoder().decode(str);
    BitSet newSet = BitSet.valueOf(in);
    int l = newSet.length();

    BitSet foo = new BitSet(43);
    BitSet bar = new BitSet(43);
    BitSet baz = new BitSet(43);
    BitSet buz = new BitSet(43);
    for (int i=0; i < 43; i++) {
      foo.set(i, false);
      bar.set(i, true);
      baz.set(i, i % 2 == 0);
      baz.set(i, i % 3 == 0);
    }
    String fooStr = Base64.getUrlEncoder().withoutPadding().encodeToString(foo.toByteArray());
    String barStr = Base64.getUrlEncoder().withoutPadding().encodeToString(bar.toByteArray());
    String bazStr = Base64.getUrlEncoder().withoutPadding().encodeToString(baz.toByteArray());
    String buzStr = Base64.getUrlEncoder().withoutPadding().encodeToString(buz.toByteArray());

    Base32 b32 = new Base32();
    String str32 = StringUtils.newStringUtf8(b32.encode(baz.toByteArray()));
    while (str32.endsWith("=")) {
      str32 = str32.substring(0, str32.length() - 1);
    }

    BitSet set32 = BitSet.valueOf(b32.decode(str32));

    boolean eq = bitSet.equals(newSet);

  }
}