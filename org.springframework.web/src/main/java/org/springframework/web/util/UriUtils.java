/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final String DEFAULT_ENCODING = "UTF-8";

	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	private static final String HTTP_PATTERN = "(http|https):";

	private static final String USERINFO_PATTERN = "([^@/]*)";

	private static final String HOST_PATTERN = "([^/?#:]*)";

	private static final String PORT_PATTERN = "(\\d*)";

	private static final String PATH_PATTERN = "([^?#]*)";

	private static final String QUERY_PATTERN = "([^#]*)";

	private static final String LAST_PATTERN = "(.*)";

	// Regex patterns that matches URIs. See RFC 3986, appendix B
	private static final Pattern URI_PATTERN = Pattern.compile(
			"^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
					")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
			"^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" +
					")?" + PATH_PATTERN + "(\\?" + LAST_PATTERN + ")?");

	
	/**
	 * Encodes the given source URI into an encoded String. All various URI components
	 * are encoded according to their respective valid character sets.
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

			return encodeUriComponents(scheme, authority, userinfo, host, port, path, query, fragment, encoding);
		}
		else {
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * Encodes the given HTTP URI into an encoded String. All various URI components
	 * are encoded according to their respective valid character sets.
	 * <p><strong>Note</strong> that this method does not support fragments ({@code #}),
	 * as these are not supposed to be sent to the server, but retained by the client.
	 * @param httpUrl  the HTTP URL to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded URL
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeHttpUrl(String httpUrl, String encoding) throws UnsupportedEncodingException {
		Assert.notNull(httpUrl, "'httpUrl' must not be null");
		Assert.hasLength(encoding, "'encoding' must not be empty");
		Matcher m = HTTP_URL_PATTERN.matcher(httpUrl);
		if (m.matches()) {
			String scheme = m.group(1);
			String authority = m.group(2);
			String userinfo = m.group(4);
			String host = m.group(5);
			String portString = m.group(7);
			String path = m.group(8);
			String query = m.group(10);

			return encodeUriComponents(scheme, authority, userinfo, host, portString, path, query, null, encoding);
		}
		else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}

	/**
	 * Encodes the given source URI components into an encoded String.
	 * All various URI components are optional, but encoded according
	 * to their respective valid character sets.
	 * @param scheme the scheme
	 * @param authority the authority
	 * @param userinfo the user info
	 * @param host the host
	 * @param port the port
	 * @param path the path
	 * @param query	the query
	 * @param fragment the fragment
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 */
	public static String encodeUriComponents(String scheme,
											 String authority,
											 String userinfo,
											 String host,
											 String port,
											 String path,
											 String query,
											 String fragment) {
		try {
			return encodeUriComponents(scheme, authority, userinfo, host, port, path, query, fragment,
					DEFAULT_ENCODING);
		}
		catch (UnsupportedEncodingException e) {
			throw new InternalError("'UTF-8' encoding not supported");
		}
	}

	/**
	 * Encodes the given source URI components into an encoded String.
	 * All various URI components are optional, but encoded according
	 * to their respective valid character sets.
	 * @param scheme the scheme
	 * @param authority the authority
	 * @param userinfo the user info
	 * @param host the host
	 * @param port the port
	 * @param path the path
	 * @param query	the query
	 * @param fragment the fragment
	 * @param encoding the character encoding to encode to
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUriComponents(String scheme,
											 String authority,
											 String userinfo,
											 String host,
											 String port,
											 String path,
											 String query,
											 String fragment,
											 String encoding) throws UnsupportedEncodingException {

		Assert.hasLength(encoding, "'encoding' must not be empty");
		StringBuilder sb = new StringBuilder();

		if (scheme != null) {
			sb.append(encodeScheme(scheme, encoding));
			sb.append(':');
		}

		if (userinfo != null || host != null || port != null) {
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
		} else if (authority != null) {
			sb.append("//");
			sb.append(encodeAuthority(authority, encoding));
		}

		if (path != null) {
			sb.append(encodePath(path, encoding));
		}

		if (query != null) {
			sb.append('?');
			sb.append(encodeQuery(query, encoding));
		}

		if (fragment != null) {
			sb.append('#');
			sb.append(encodeFragment(fragment, encoding));
		}

		return sb.toString();
	}

	/**
	 * Encodes the given URI scheme with the given encoding.
	 * @param scheme the scheme to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded scheme
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeScheme(String scheme, String encoding) throws UnsupportedEncodingException {
		return encode(scheme, encoding, SCHEME_COMPONENT, false);
	}

	/**
	 * Encodes the given URI authority with the given encoding.
	 * @param authority the authority to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded authority
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeAuthority(String authority, String encoding) throws UnsupportedEncodingException {
		return encode(authority, encoding, AUTHORITY_COMPONENT, false);
	}

	/**
	 * Encodes the given URI user info with the given encoding.
	 * @param userInfo the user info to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded user info
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUserInfo(String userInfo, String encoding) throws UnsupportedEncodingException {
		return encode(userInfo, encoding, USER_INFO_COMPONENT, false);
	}

	/**
	 * Encodes the given URI host with the given encoding.
	 * @param host the host to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded host
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeHost(String host, String encoding) throws UnsupportedEncodingException {
		return encode(host, encoding, HOST_COMPONENT, false);
	}

	/**
	 * Encodes the given URI port with the given encoding.
	 * @param port the port to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded port
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePort(String port, String encoding) throws UnsupportedEncodingException {
		return encode(port, encoding, PORT_COMPONENT, false);
	}

	/**
	 * Encodes the given URI path with the given encoding.
	 * @param path the path to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded path
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePath(String path, String encoding) throws UnsupportedEncodingException {
		return encode(path, encoding, PATH_COMPONENT, false);
	}

	/**
	 * Encodes the given URI path segment with the given encoding.
	 * @param segment the segment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded segment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePathSegment(String segment, String encoding) throws UnsupportedEncodingException {
		return encode(segment, encoding, PATH_SEGMENT_COMPONENT, false);
	}

	/**
	 * Encodes the given URI query with the given encoding.
	 * @param query	the query to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQuery(String query, String encoding) throws UnsupportedEncodingException {
		return encode(query, encoding, QUERY_COMPONENT, false);
	}

	/**
	 * Encodes the given URI query parameter with the given encoding.
	 * @param queryParam the query parameter to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query parameter
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQueryParam(String queryParam, String encoding) throws UnsupportedEncodingException {
		return encode(queryParam, encoding, QUERY_PARAM_COMPONENT, false);
	}

	/**
	 * Encodes the given URI fragment with the given encoding.
	 * @param fragment the fragment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded fragment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeFragment(String fragment, String encoding) throws UnsupportedEncodingException {
		return encode(fragment, encoding, FRAGMENT_COMPONENT, false);
	}

	/**
	 * Encodes the given source into an encoded String using the rules specified by the given component. This method
	 * encodes with the default encoding (i.e. UTF-8).
	 * @param source the source string
	 * @param uriComponent the URI component for the source
	 * @param allowTemplateVars whether URI template variables are allowed. If {@code true}, '{' and '}' characters
	 * are not encoded, even though they might not be valid for the component
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @see #SCHEME_COMPONENT
	 * @see #AUTHORITY_COMPONENT
	 * @see #USER_INFO_COMPONENT
	 * @see #HOST_COMPONENT
	 * @see #PORT_COMPONENT
	 * @see #PATH_COMPONENT
	 * @see #PATH_SEGMENT_COMPONENT
	 * @see #QUERY_COMPONENT
	 * @see #QUERY_PARAM_COMPONENT
	 * @see #FRAGMENT_COMPONENT
	 */
	public static String encode(String source, UriComponent uriComponent, boolean allowTemplateVars) {
		try {
			return encode(source, DEFAULT_ENCODING, uriComponent, allowTemplateVars);
		}
		catch (UnsupportedEncodingException e) {
			throw new InternalError("'" + DEFAULT_ENCODING + "' encoding not supported");
		}
	}

	/**
	 * Encodes the given source into an encoded String using the rules specified by the given component.
	 * @param source the source string
	 * @param encoding the encoding of the source string
	 * @param uriComponent the URI component for the source
	 * @param allowTemplateVars whether URI template variables are allowed. If {@code true}, '{' and '}' characters
	 * are not encoded, even though they might not be valid for the component
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @see #SCHEME_COMPONENT
	 * @see #AUTHORITY_COMPONENT
	 * @see #USER_INFO_COMPONENT
	 * @see #HOST_COMPONENT
	 * @see #PORT_COMPONENT
	 * @see #PATH_COMPONENT
	 * @see #PATH_SEGMENT_COMPONENT
	 * @see #QUERY_COMPONENT
	 * @see #QUERY_PARAM_COMPONENT
	 * @see #FRAGMENT_COMPONENT
	 */
	public static String encode(String source, String encoding, UriComponent uriComponent, boolean allowTemplateVars)
			throws UnsupportedEncodingException {
		Assert.hasLength(encoding, "'encoding' must not be empty");

		byte[] bytes = encodeInternal(source.getBytes(encoding), uriComponent, allowTemplateVars);
		return new String(bytes, "US-ASCII");
	}

	private static byte[] encodeInternal(byte[] source, UriComponent uriComponent, boolean allowTemplateVars) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(uriComponent, "'uriComponent' must not be null");

		ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length);
		for (int i = 0; i < source.length; i++) {
			int b = source[i];
			if (b < 0) {
				b += 256;
			}
			if (uriComponent.isAllowed(b)) {
				bos.write(b);
			}
			else if (allowTemplateVars && (b == '{' || b == '}')) {
				bos.write(b);
			}
			else {
				bos.write('%');

				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));

				bos.write(hex1);
				bos.write(hex2);
			}
		}
		return bos.toByteArray();
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
	 * @param source the source string
	 * @param encoding the encoding
	 * @return the decoded URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, String encoding) throws UnsupportedEncodingException {
		Assert.notNull(source, "'source' must not be null");
		Assert.hasLength(encoding, "'encoding' must not be empty");
		int length = source.length();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		boolean changed = false;
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
					changed = true;
				}
				else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			}
			else {
				bos.write(ch);
			}
		}
		return changed ? new String(bos.toByteArray(), encoding) : source;
	}

	/**
	 * Defines the contract for an URI component, i.e. scheme, host, path, etc.
	 */
	public interface UriComponent {

		/**
		 * Specifies whether the given character is allowed in this URI component.
		 * @param c the character
		 * @return {@code true} if the character is allowed; {@code false} otherwise
		 */
		boolean isAllowed(int c);

	}

	private static abstract class AbstractUriComponent implements UriComponent {

		protected boolean isAlpha(int c) {
			return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
		}

		protected boolean isDigit(int c) {
			return c >= '0' && c <= '9';
		}

		protected boolean isGenericDelimiter(int c) {
			return ':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c;
		}

		protected boolean isSubDelimiter(int c) {
			return '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
					',' == c || ';' == c || '=' == c;
		}

		protected boolean isReserved(char c) {
			return isGenericDelimiter(c) || isReserved(c);
		}

		protected boolean isUnreserved(int c) {
			return isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c;
		}

		protected boolean isPchar(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
		}

	}

	/** The scheme URI component. */
	public static final UriComponent SCHEME_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
		}
	};

	/** The authority URI component. */
	public static final UriComponent AUTHORITY_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
		}
	};

	/** The user info URI component. */
	public static final UriComponent USER_INFO_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
		}
	};

	/** The host URI component. */
	public static final UriComponent HOST_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isUnreserved(c) || isSubDelimiter(c);
		}
	};

	/** The port URI component. */
	public static final UriComponent PORT_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isDigit(c);
		}
	};

	/** The path URI component. */
	public static final UriComponent PATH_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isPchar(c) || '/' == c;
		}
	};

	/** The path segment URI component. */
	public static final UriComponent PATH_SEGMENT_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isPchar(c);
		}
	};

	/** The query URI component. */
	public static final UriComponent QUERY_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isPchar(c) || '/' == c || '?' == c;
		}
	};

	/** The query parameter URI component. */
	public static final UriComponent QUERY_PARAM_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			if ('=' == c || '+' == c || '&' == c) {
				return false;
			}
			else {
				return isPchar(c) || '/' == c || '?' == c;
			}
		}
	};

	/** The fragment URI component. */
	public static final UriComponent FRAGMENT_COMPONENT = new AbstractUriComponent() {
		public boolean isAllowed(int c) {
			return isPchar(c) || '/' == c || '?' == c;
		}
	};

}
