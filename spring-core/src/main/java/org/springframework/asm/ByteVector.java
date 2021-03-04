// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.springframework.asm;

/**
 * A dynamically extensible vector of bytes. This class is roughly equivalent to a DataOutputStream
 * on top of a ByteArrayOutputStream, but is more efficient.
 *
 * @author Eric Bruneton
 */
public class ByteVector {

  /** The content of this vector. Only the first {@link #length} bytes contain real data. */
  byte[] data;

  /** The actual number of bytes in this vector. */
  int length;

  /** Constructs a new {@link ByteVector} with a default initial capacity. */
  public ByteVector() {
    data = new byte[64];
  }

  /**
   * Constructs a new {@link ByteVector} with the given initial capacity.
   *
   * @param initialCapacity the initial capacity of the byte vector to be constructed.
   */
  public ByteVector(final int initialCapacity) {
    data = new byte[initialCapacity];
  }

  /**
   * Constructs a new {@link ByteVector} from the given initial data.
   *
   * @param data the initial data of the new byte vector.
   */
  ByteVector(final byte[] data) {
    this.data = data;
    this.length = data.length;
  }

  /**
   * Puts a byte into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param byteValue a byte.
   * @return this byte vector.
   */
  public ByteVector putByte(final int byteValue) {
    int currentLength = length;
    if (currentLength + 1 > data.length) {
      enlarge(1);
    }
    data[currentLength++] = (byte) byteValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts two bytes into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param byteValue1 a byte.
   * @param byteValue2 another byte.
   * @return this byte vector.
   */
  final ByteVector put11(final int byteValue1, final int byteValue2) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    length = currentLength;
    return this;
  }

  /**
   * Puts a short into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param shortValue a short.
   * @return this byte vector.
   */
  public ByteVector putShort(final int shortValue) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts a byte and a short into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param byteValue a byte.
   * @param shortValue a short.
   * @return this byte vector.
   */
  final ByteVector put12(final int byteValue, final int shortValue) {
    int currentLength = length;
    if (currentLength + 3 > data.length) {
      enlarge(3);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts two bytes and a short into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param byteValue1 a byte.
   * @param byteValue2 another byte.
   * @param shortValue a short.
   * @return this byte vector.
   */
  final ByteVector put112(final int byteValue1, final int byteValue2, final int shortValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts an int into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param intValue an int.
   * @return this byte vector.
   */
  public ByteVector putInt(final int intValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts one byte and two shorts into this byte vector. The byte vector is automatically enlarged
   * if necessary.
   *
   * @param byteValue a byte.
   * @param shortValue1 a short.
   * @param shortValue2 another short.
   * @return this byte vector.
   */
  final ByteVector put122(final int byteValue, final int shortValue1, final int shortValue2) {
    int currentLength = length;
    if (currentLength + 5 > data.length) {
      enlarge(5);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue1 >>> 8);
    currentData[currentLength++] = (byte) shortValue1;
    currentData[currentLength++] = (byte) (shortValue2 >>> 8);
    currentData[currentLength++] = (byte) shortValue2;
    length = currentLength;
    return this;
  }

  /**
   * Puts a long into this byte vector. The byte vector is automatically enlarged if necessary.
   *
   * @param longValue a long.
   * @return this byte vector.
   */
  public ByteVector putLong(final long longValue) {
    int currentLength = length;
    if (currentLength + 8 > data.length) {
      enlarge(8);
    }
    byte[] currentData = data;
    int intValue = (int) (longValue >>> 32);
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    intValue = (int) longValue;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param stringValue a String whose UTF8 encoded length must be less than 65536.
   * @return this byte vector.
   */
  // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
  public ByteVector putUTF8(final String stringValue) {
    int charLength = stringValue.length();
    if (charLength > 65535) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    int currentLength = length;
    if (currentLength + 2 + charLength > data.length) {
      enlarge(2 + charLength);
    }
    byte[] currentData = data;
    // Optimistic algorithm: instead of computing the byte length and then serializing the string
    // (which requires two loops), we assume the byte length is equal to char length (which is the
    // most frequent case), and we start serializing the string right away. During the
    // serialization, if we find that this assumption is wrong, we continue with the general method.
    currentData[currentLength++] = (byte) (charLength >>> 8);
    currentData[currentLength++] = (byte) charLength;
    for (int i = 0; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= '\u0001' && charValue <= '\u007F') {
        currentData[currentLength++] = (byte) charValue;
      } else {
        length = currentLength;
        return encodeUtf8(stringValue, i, 65535);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * Puts an UTF8 string into this byte vector. The byte vector is automatically enlarged if
   * necessary. The string length is encoded in two bytes before the encoded characters, if there is
   * space for that (i.e. if this.length - offset - 2 &gt;= 0).
   *
   * @param stringValue the String to encode.
   * @param offset the index of the first character to encode. The previous characters are supposed
   *     to have already been encoded, using only one byte per character.
   * @param maxByteLength the maximum byte length of the encoded string, including the already
   *     encoded characters.
   * @return this byte vector.
   */
  final ByteVector encodeUtf8(final String stringValue, final int offset, final int maxByteLength) {
    int charLength = stringValue.length();
    int byteLength = offset;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        byteLength++;
      } else if (charValue <= 0x07FF) {
        byteLength += 2;
      } else {
        byteLength += 3;
      }
    }
    if (byteLength > maxByteLength) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    // Compute where 'byteLength' must be stored in 'data', and store it at this location.
    int byteLengthOffset = length - offset - 2;
    if (byteLengthOffset >= 0) {
      data[byteLengthOffset] = (byte) (byteLength >>> 8);
      data[byteLengthOffset + 1] = (byte) byteLength;
    }
    if (length + byteLength - offset > data.length) {
      enlarge(byteLength - offset);
    }
    int currentLength = length;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        data[currentLength++] = (byte) charValue;
      } else if (charValue <= 0x07FF) {
        data[currentLength++] = (byte) (0xC0 | charValue >> 6 & 0x1F);
        data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      } else {
        data[currentLength++] = (byte) (0xE0 | charValue >> 12 & 0xF);
        data[currentLength++] = (byte) (0x80 | charValue >> 6 & 0x3F);
        data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * Puts an array of bytes into this byte vector. The byte vector is automatically enlarged if
   * necessary.
   *
   * @param byteArrayValue an array of bytes. May be {@literal null} to put {@code byteLength} null
   *     bytes into this byte vector.
   * @param byteOffset index of the first byte of byteArrayValue that must be copied.
   * @param byteLength number of bytes of byteArrayValue that must be copied.
   * @return this byte vector.
   */
  public ByteVector putByteArray(
      final byte[] byteArrayValue, final int byteOffset, final int byteLength) {
    if (length + byteLength > data.length) {
      enlarge(byteLength);
    }
    if (byteArrayValue != null) {
      System.arraycopy(byteArrayValue, byteOffset, data, length, byteLength);
    }
    length += byteLength;
    return this;
  }

  /**
   * Enlarges this byte vector so that it can receive 'size' more bytes.
   *
   * @param size number of additional bytes that this byte vector should be able to receive.
   */
  private void enlarge(final int size) {
    int doubleCapacity = 2 * data.length;
    int minimalCapacity = length + size;
    byte[] newData = new byte[doubleCapacity > minimalCapacity ? doubleCapacity : minimalCapacity];
    System.arraycopy(data, 0, newData, 0, length);
    data = newData;
  }
}
