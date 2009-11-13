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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.springframework.util.Assert;

/**
 * Utility class for URI encoding and decoding based on RFC 3986. Offers encoding methods for
 * the various URI components.
 *
 * <p>All {@code encode*(String, String} methods in this class operate in a similar way:
 * <ul>
 * <li>Valid characters for the specific URI component as defined in RFC 3986 stay the same.
 * <li>All other characters are converted into one or more bytes in the given encoding scheme.
 * Each of the resulting bytes is written as a hexadecimal string in the "<code>%<i>xy</i></code>" format.
 * </ul>
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 */
public abstract class UriUtils {

	private static final BitSet SCHEME;
	
	private static final BitSet USER_INFO;

	private static final BitSet HOST;

	private static final BitSet PORT;

	private static final BitSet PATH;

	private static final BitSet SEGMENT;

	private static final BitSet QUERY;

	private static final BitSet QUERY_PARAM;

	private static final BitSet FRAGMENT;

	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	private static final String USERINFO_PATTERN = "([^@]*)";

	private static final String HOST_PATTERN = "([^/?#:]*)";

	private static final String PORT_PATTERN = "(\\d*)";

	private static final String PATH_PATTERN = "([^?#]*)";

	private static final String QUERY_PATTERN = "([^#]*)";

	private static final String FRAGMENT_PATTERN = "(.*)";

	// Regex patterns that matches URIs. See RFC 3986, appendix B
	private static final Pattern URI_PATTERN =
			Pattern.compile("^(" + SCHEME_PATTERN + ")?" +
					"(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?"
					+ PATH_PATTERN +
					"(\\?" + QUERY_PATTERN + ")?" +
					"(#" + FRAGMENT_PATTERN + ")?");

	static {
		// variable names refer to RFC 3986, appendix A
		BitSet alpha = new BitSet(256);
		for (int i = 'a'; i <= 'z'; i++) {
			alpha.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			alpha.set(i);
		}
		BitSet digit = new BitSet(256);
		for (int i = '0'; i <= '9'; i++) {
			digit.set(i);
		}

		BitSet gendelims = new BitSet(256);
		gendelims.set(':');
		gendelims.set('/');
		gendelims.set('?');
		gendelims.set('#');
		gendelims.set('[');
		gendelims.set(']');
		gendelims.set('@');

		BitSet subdelims = new BitSet(256);
		subdelims.set('!');
		subdelims.set('$');
		subdelims.set('&');
		subdelims.set('\'');
		subdelims.set('(');
		subdelims.set(')');
		subdelims.set('*');
		subdelims.set('+');
		subdelims.set(',');
		subdelims.set(';');
		subdelims.set('=');

		BitSet reserved = new BitSet(256);
		reserved.or(gendelims);
		reserved.or(subdelims);

		BitSet unreserved = new BitSet(256);
		unreserved.or(alpha);
		unreserved.or(digit);
		unreserved.set('-');
		unreserved.set('.');
		unreserved.set('_');
		unreserved.set('~');

		SCHEME = new BitSet(256);
		SCHEME.or(alpha);
		SCHEME.or(digit);
		SCHEME.set('+');
		SCHEME.set('-');
		SCHEME.set('.');

		USER_INFO = new BitSet(256);
		USER_INFO.or(unreserved);
		USER_INFO.or(subdelims);
		USER_INFO.set(':');

		HOST = new BitSet(256);
		HOST.or(unreserved);
		HOST.or(subdelims);

		PORT = new BitSet(256);
		PORT.or(digit);

		BitSet pchar = new BitSet(256);
		pchar.or(unreserved);
		pchar.or(subdelims);
		pchar.set(':');
		pchar.set('@');

		SEGMENT = new BitSet(256);
		SEGMENT.or(pchar);

		PATH = new BitSet(256);
		PATH.or(SEGMENT);
		PATH.set('/');

		QUERY = new BitSet(256);
		QUERY.or(pchar);
		QUERY.set('/');
		QUERY.set('?');

		QUERY_PARAM = new BitSet(256);
		QUERY_PARAM.or(pchar);
		QUERY_PARAM.set('/');
		QUERY_PARAM.set('?');
		QUERY_PARAM.clear('=');
		QUERY_PARAM.clear('+');
		QUERY_PARAM.clear('&');

		FRAGMENT = new BitSet(256);
		FRAGMENT.or(pchar);
		FRAGMENT.set('/');
		FRAGMENT.set('?');
	}

	/**
	 * Encodes the given source URI into an encoded String. All various URI components are encoded according to
	 * their respective valid character sets.
	 *
	 * @param uri the URI to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUri(String uri, String encoding) throws UnsupportedEncodingException {
		Assert.notNull(uri, "'uri' must not be null");
		Assert.hasLength(encoding, "'encoding' must not be empty");
		Matcher m = URI_PATTERN.matcher(uri);
		if (m.matches()) {
			String scheme = m.group(2);
			String authority = m.group(3);
			String userinfo = m.group(5);
			String host = m.group(6);
			String port = m.group(8);
			String path = m.group(9);
			String query = m.group(11);
			String fragment = m.group(13);

			StringBuilder sb = new StringBuilder();

			if (scheme != null) {
				sb.append(encodeScheme(scheme, encoding));
				sb.append(':');
			}

			if (authority != null) {
				sb.append("//");
				if (userinfo != null) {
					sb.append(encodeUserInfo(userinfo, encoding));
					sb.append('@');
				}
				if (host != null) {
					sb.append(encodeHost(host, encoding));
				}
				if (port != null) {
					sb.append(':');
					sb.append(encodePort(port, encoding));
				}
			}

			sb.append(encodePath(path, encoding));

			if (query != null) {
				sb.append('?');
				sb.append(encodeQuery(query, encoding));
			}

			if (fragment != null) {
				sb.append('#');
				sb.append(encodeFragment(fragment, encoding));
			}

			return sb.toString();
		} else {
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * Encodes the given URI scheme.
	 *
	 * @param scheme the scheme to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded scheme
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeScheme(String scheme, String encoding) throws UnsupportedEncodingException {
		return encode(scheme, encoding, SCHEME);
	}

	/**
	 * Encodes the given URI user info.
	 *
	 * @param userInfo the user info to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded user info
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUserInfo(String userInfo, String encoding) throws UnsupportedEncodingException {
		return encode(userInfo, encoding, USER_INFO);
	}

	/**
	 * Encodes the given URI host.
	 *
	 * @param host the host to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded host
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeHost(String host, String encoding) throws UnsupportedEncodingException {
		return encode(host, encoding, HOST);
	}

	/**
	 * Encodes the given URI port.
	 *
	 * @param port the port to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded port
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePort(String port, String encoding) throws UnsupportedEncodingException {
		return encode(port, encoding, PORT);
	}

	/**
	 * Encodes the given URI path.
	 *
	 * @param path the path to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded path
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePath(String path, String encoding) throws UnsupportedEncodingException {
		return encode(path, encoding, PATH);
	}

	/**
	 * Encodes the given URI path segment.
	 *
	 * @param segment the segment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded segment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePathSegment(String segment, String encoding) throws UnsupportedEncodingException {
		return encode(segment, encoding, SEGMENT);
	}

	/**
	 * Encodes the given URI query.
	 *
	 * @param query the query to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQuery(String query, String encoding) throws UnsupportedEncodingException {
		return encode(query, encoding, QUERY);
	}

	/**
	 * Encodes the given URI query parameter.
	 *
	 * @param query the query parameter to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query parameter
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQueryParam(String queryParam, String encoding) throws UnsupportedEncodingException {
		return encode(queryParam, encoding, QUERY_PARAM);
	}

	/**
	 * Encodes the given URI fragment.
	 *
	 * @param fragment the fragment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded fragment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeFragment(String fragment, String encoding) throws UnsupportedEncodingException {
		return encode(fragment, encoding, FRAGMENT);
	}

	private static String encode(String source, String encoding, BitSet notEncoded) throws UnsupportedEncodingException {
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
