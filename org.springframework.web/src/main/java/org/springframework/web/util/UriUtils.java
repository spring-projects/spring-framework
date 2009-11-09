/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;

import org.springframework.util.Assert;

/**
 * Utility class for URI encoding. Based on RFC 2396.
 *
 * <p>Effectively, the encoding and decoding methods in this class
 * are similar to those found in {@link java.net.URLEncoder} and
 * {@link java.net.URLDecoder}, except that the space character
 * is encoded as {@code %20}, not {@code +}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
 */
public abstract class UriUtils {

	private static final BitSet notEncoded = new BitSet(256);

	static {
		for (int i = 'a'; i <= 'z'; i++) {
			notEncoded.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			notEncoded.set(i);
		}
		for (int i = '0'; i <= '9'; i++) {
			notEncoded.set(i);
		}
		notEncoded.set('-');
		notEncoded.set('_');
		notEncoded.set('.');
		notEncoded.set('*');

	}

	/**
	 * Encodes the given source URI into an encoded String. Based on the following
	 * rules:
	 * <ul>
	 * <li>Alphanumeric characters {@code "a"} through {@code "z"},
	 * {@code "A"} through {@code "Z"}, and {@code "0"} through {@code "9"}
	 * stay the same.
	 * <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and
	 * {@code "*"} stay the same.
	 * <li>All other characters are converted into one or more bytes using the
	 * given encoding scheme. Each of the resulting bytes is written as a
	 * hexadecimal string in the "<code>%<i>xy</i></code>" format.
	 * </ul>
	 *
	 * @param source the String to be encoded
	 * @param encoding the encoding to use
	 * @return the encoded string
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 * @see java.net.URLEncoder#encode(String, String)
	 */
	public static String encode(String source, String encoding) throws UnsupportedEncodingException {
		Assert.notNull(source, "'source' must not be null");
		Assert.hasLength(encoding, "'encoding' must not be empty");
		ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length() * 2);

		for (int i = 0; i < source.length(); i++) {
			int ch = source.charAt(i);
			if (notEncoded.get(ch)) {
				bos.write(ch);
			}
			else {
				bos.write('%');
				char hex1 = Character.toUpperCase(Character.forDigit((ch >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(ch & 0xF, 16));
				bos.write(hex1);
				bos.write(hex2);
			}
		}
		return new String(bos.toByteArray(), encoding);
	}

	/**
	 * Decodes the given encoded source String into an URI. Based on the following
	 * rules:
	 * <ul>
	 * <li>Alphanumeric characters {@code "a"} through {@code "z"},
	 * {@code "A"} through {@code "Z"}, and {@code "0"} through {@code "9"}
	 * stay the same.
	 * <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and
	 * {@code "*"} stay the same.
	 * <li>All other characters are converted into one or more bytes using the
	 * given encoding scheme. Each of the resulting bytes is written as a
	 * hexadecimal string in the {@code %xy} format.
	 * <li>A sequence "<code>%<i>xy</i></code>" is interpreted as a hexadecimal
	 * representation of the character.
	 * </ul>
	 * @param source
	 * @param encoding
	 * @return the decoded URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, String encoding) throws UnsupportedEncodingException {
		Assert.notNull(source, "'source' must not be null");
		Assert.hasLength(encoding, "'encoding' must not be empty");
		int length = source.length();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if ((i + 2) < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					bos.write((char) ((u << 4) + l));
					i += 2;
				}
				else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			}
			else {
				bos.write(ch);
			}
		}
		return new String(bos.toByteArray(), encoding);
	}

}
