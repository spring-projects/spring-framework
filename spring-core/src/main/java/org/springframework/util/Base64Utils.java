/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.util;

import java.nio.charset.Charset;
import java.util.Base64;

import org.springframework.lang.UsesJava8;

/**
 * A simple utility class for Base64 encoding and decoding.
 *
 * <p>Adapts to either Java 8's {@link java.util.Base64} class or Apache
 * Commons Codec's {@link org.apache.commons.codec.binary.Base64} class.
 * With neither Java 8 nor Commons Codec present, encode/decode calls
 * will fail with an IllegalStateException.
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see java.util.Base64
 * @see org.apache.commons.codec.binary.Base64
 */
public abstract class Base64Utils {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


	private static final Base64Delegate delegate;

	static {
		Base64Delegate delegateToUse = null;
		// JDK 8's java.util.Base64 class present?
		if (ClassUtils.isPresent("java.util.Base64", Base64Utils.class.getClassLoader())) {
			delegateToUse = new JdkBase64Delegate();
		}
		// Apache Commons Codec present on the classpath?
		else if (ClassUtils.isPresent("org.apache.commons.codec.binary.Base64", Base64Utils.class.getClassLoader())) {
			delegateToUse = new CommonsCodecBase64Delegate();
		}
		delegate = delegateToUse;
	}

	/**
	 * Assert that Byte64 encoding is actually supported.
	 * @throws IllegalStateException if neither Java 8 nor Apache Commons Codec is present
	 */
	private static void assertSupported() {
		Assert.state(delegate != null, "Neither Java 8 nor Apache Commons Codec found - Base64 encoding not supported");
	}


	/**
	 * Base64-encode the given byte array.
	 * @param src the original byte array (may be {@code null})
	 * @return the encoded byte array (or {@code null} if the input was {@code null})
	 * @throws IllegalStateException if Base64 encoding is not supported,
	 * i.e. neither Java 8 nor Apache Commons Codec is present at runtime
	 */
	public static byte[] encode(byte[] src) {
		assertSupported();
		return delegate.encode(src);
	}

	/**
	 * Base64-encode the given byte array to a String.
	 * @param src the original byte array (may be {@code null})
	 * @return the encoded byte array as a UTF-8 String
	 * (or {@code null} if the input was {@code null})
	 * @throws IllegalStateException if Base64 encoding is not supported,
	 * i.e. neither Java 8 nor Apache Commons Codec is present at runtime
	 */
	public static String encodeToString(byte[] src) {
		assertSupported();
		if (src == null) {
			return null;
		}
		if (src.length == 0) {
			return "";
		}
		return new String(delegate.encode(src), DEFAULT_CHARSET);
	}

	/**
	 * Base64-decode the given byte array.
	 * @param src the encoded byte array (may be {@code null})
	 * @return the original byte array (or {@code null} if the input was {@code null})
	 * @throws IllegalStateException if Base64 encoding is not supported,
	 * i.e. neither Java 8 nor Apache Commons Codec is present at runtime
	 */
	public static byte[] decode(byte[] src) {
		assertSupported();
		return delegate.decode(src);
	}

	/**
	 * Base64-decode the given byte array from an UTF-8 String.
	 * @param src the encoded UTF-8 String (may be {@code null})
	 * @return the original byte array (or {@code null} if the input was {@code null})
	 * @throws IllegalStateException if Base64 encoding is not supported,
	 * i.e. neither Java 8 nor Apache Commons Codec is present at runtime
	 */
	public static byte[] decodeFromString(String src) {
		assertSupported();
		if (src == null) {
			return null;
		}
		if (src.length() == 0) {
			return new byte[0];
		}
		return delegate.decode(src.getBytes(DEFAULT_CHARSET));
	}


	private interface Base64Delegate {

		byte[] encode(byte[] src);

		byte[] decode(byte[] src);
	}


	@UsesJava8
	private static class JdkBase64Delegate implements Base64Delegate {

		public byte[] encode(byte[] src) {
			if (src == null || src.length == 0) {
				return src;
			}
			return Base64.getEncoder().encode(src);
		}

		public byte[] decode(byte[] src) {
			if (src == null || src.length == 0) {
				return src;
			}
			return Base64.getDecoder().decode(src);
		}
	}


	private static class CommonsCodecBase64Delegate implements Base64Delegate {

		private final org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();

		public byte[] encode(byte[] src) {
			return this.base64.encode(src);
		}

		public byte[] decode(byte[] src) {
			return this.base64.decode(src);
		}
	}

}
