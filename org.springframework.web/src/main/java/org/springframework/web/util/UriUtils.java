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
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Utility class for URI encoding and decoding based on RFC 3986. Offers encoding methods for the various URI
 * components.
 *
 * <p>All {@code encode*(String, String} methods in this class operate in a similar way:
 * <ul>
 *     <li>Valid characters for the specific URI component as defined in RFC 3986 stay the same.</li>
 *     <li>All other characters are converted into one or more bytes in the given encoding scheme. Each of the
 *     resulting bytes is written as a hexadecimal string in the "<code>%<i>xy</i></code>" format.</li>
 * </ul>
 *
 * @author Arjen Poutsma
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 * @since 3.0
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
			"^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
					PATH_PATTERN + "(\\?" + LAST_PATTERN + ")?");
	

	// Parsing

	/**
	 * Parses the given source URI into a mapping of URI components to string values.
	 *
	 * <p>The returned map will contain keys for
	 * <ul>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#SCHEME}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#AUTHORITY}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#USER_INFO}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#HOST}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#PORT}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#PATH}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#QUERY}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#FRAGMENT}</li>
	 * </ul>
	 * though the values assigned to these keys is {@code null} if they do not occur in the given source URI.
	 *
	 * <p><strong>Note</strong> that the returned map will never contain mappings for {@link org.springframework.web.util.UriComponents.Type#PATH_SEGMENT},
	 * nor {@link org.springframework.web.util.UriComponents.Type#QUERY_PARAM}, since those components can occur multiple times in the URI.
	 *
	 * @param uri the source URI
	 * @return the URI components of the URI
	 */
	public static Map<UriComponents.Type, String> parseUriComponents(String uri) {
		Assert.notNull(uri, "'uri' must not be null");
		Matcher m = URI_PATTERN.matcher(uri);
		if (m.matches()) {
			Map<UriComponents.Type, String> result = new EnumMap<UriComponents.Type, String>(UriComponents.Type.class);

			result.put(UriComponents.Type.SCHEME, m.group(2));
			result.put(UriComponents.Type.AUTHORITY, m.group(3));
			result.put(UriComponents.Type.USER_INFO, m.group(5));
			result.put(UriComponents.Type.HOST, m.group(6));
			result.put(UriComponents.Type.PORT, m.group(8));
			result.put(UriComponents.Type.PATH, m.group(9));
			result.put(UriComponents.Type.QUERY, m.group(11));
			result.put(UriComponents.Type.FRAGMENT, m.group(13));

			return result;
		}
		else {
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * Parses the given source HTTP URL into a mapping of URI components to string values.
	 *
	 *
	 * <p>The returned map will contain keys for
	 * <ul>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#SCHEME}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#AUTHORITY}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#USER_INFO}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#HOST}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#PORT}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#PATH}</li>
	 *     <li>{@link org.springframework.web.util.UriComponents.Type#QUERY}</li>
	 * </ul>
	 * though the values assigned to these keys is {@code null} if they do not occur in the given source URI.
	 *
	 * <p><strong>Note</strong> that the returned map will never contain mappings for {@link org.springframework.web.util.UriComponents.Type#PATH_SEGMENT},
	 * nor {@link org.springframework.web.util.UriComponents.Type#QUERY_PARAM}, since those components can occur multiple times in the URI. Nor does it
	 * contain a mapping for {@link org.springframework.web.util.UriComponents.Type#FRAGMENT}, as fragments are not supposed to be sent to the server, but
	 * retained by the client.
	 *
	 * @param httpUrl the source URI
	 * @return the URI components of the URI
	 */
	public static Map<UriComponents.Type, String> parseHttpUrlComponents(String httpUrl) {
		Assert.notNull(httpUrl, "'httpUrl' must not be null");
		Matcher m = HTTP_URL_PATTERN.matcher(httpUrl);
		if (m.matches()) {
			Map<UriComponents.Type, String> result = new EnumMap<UriComponents.Type, String>(UriComponents.Type.class);

			result.put(UriComponents.Type.SCHEME, m.group(1));
			result.put(UriComponents.Type.AUTHORITY, m.group(2));
			result.put(UriComponents.Type.USER_INFO, m.group(4));
			result.put(UriComponents.Type.HOST, m.group(5));
			result.put(UriComponents.Type.PORT, m.group(7));
			result.put(UriComponents.Type.PATH, m.group(8));
			result.put(UriComponents.Type.QUERY, m.group(10));

			return result;
		}
		else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}

	// building

	/**
	 * Builds a URI from the given URI components. The given map should contain at least one entry.
	 *
	 * <p><strong>Note</strong> that {@link org.springframework.web.util.UriComponents.Type#PATH_SEGMENT} and {@link org.springframework.web.util.UriComponents.Type#QUERY_PARAM} keys (if any)
	 * will not be used to build the URI, in favor of {@link org.springframework.web.util.UriComponents.Type#PATH} and {@link org.springframework.web.util.UriComponents.Type#QUERY}
	 * respectively.
	 *
	 * @param uriComponents the components to build the URI out of
	 * @return the URI created from the given components
	 */
	public static String buildUri(Map<UriComponents.Type, String> uriComponents) {
		Assert.notEmpty(uriComponents, "'uriComponents' must not be empty");

		return buildUri(uriComponents.get(UriComponents.Type.SCHEME), uriComponents.get(
				UriComponents.Type.AUTHORITY),
				uriComponents.get(UriComponents.Type.USER_INFO), uriComponents.get(UriComponents.Type.HOST),
				uriComponents.get(UriComponents.Type.PORT), uriComponents.get(UriComponents.Type.PATH),
				uriComponents.get(UriComponents.Type.QUERY), uriComponents.get(UriComponents.Type.FRAGMENT));
	}

	/**
	 * Builds a URI from the given URI component parameters. All parameters can be {@code null}.
	 *
	 * @param scheme the scheme
	 * @param authority the authority
	 * @param userinfo the user info
	 * @param host the host
	 * @param port the port
	 * @param path the path
	 * @param query the query
	 * @param fragment the fragment
	 * @return the URI created from the given components
	 */
	public static String buildUri(String scheme,
								  String authority,
								  String userinfo,
								  String host,
								  String port,
								  String path,
								  String query,
								  String fragment) {
		StringBuilder uriBuilder = new StringBuilder();

		if (scheme != null) {
			uriBuilder.append(scheme);
			uriBuilder.append(':');
		}

		if (userinfo != null || host != null || port != null) {
			uriBuilder.append("//");
			if (userinfo != null) {
				uriBuilder.append(userinfo);
				uriBuilder.append('@');
			}
			if (host != null) {
				uriBuilder.append(host);
			}
			if (port != null) {
				uriBuilder.append(':');
				uriBuilder.append(port);
			}
		}
		else if (authority != null) {
			uriBuilder.append("//");
			uriBuilder.append(authority);
		}

		if (path != null) {
			uriBuilder.append(path);
		}

		if (query != null) {
			uriBuilder.append('?');
			uriBuilder.append(query);
		}

		if (fragment != null) {
			uriBuilder.append('#');
			uriBuilder.append(fragment);
		}

		return uriBuilder.toString();
	}

	// encoding

	/**
	 * Encodes the given source URI into an encoded String. All various URI components are encoded according to their
	 * respective valid character sets.
	 *
	 * @param uri the URI to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUri(String uri, String encoding) throws UnsupportedEncodingException {
		Map<UriComponents.Type, String> uriComponents = parseUriComponents(uri);
		return encodeUriComponents(uriComponents, encoding);
	}

	/**
	 * Encodes the given HTTP URI into an encoded String. All various URI components are encoded according to their
	 * respective valid character sets. <p><strong>Note</strong> that this method does not support fragments ({@code #}),
	 * as these are not supposed to be sent to the server, but retained by the client.
	 *
	 * @param httpUrl the HTTP URL to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded URL
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeHttpUrl(String httpUrl, String encoding) throws UnsupportedEncodingException {
		Map<UriComponents.Type, String> uriComponents = parseHttpUrlComponents(httpUrl);
		return encodeUriComponents(uriComponents, encoding);
	}

	/**
	 * Encodes the given source URI components into an encoded String. All various URI components are optional, but encoded
	 * according to their respective valid character sets.
	 *
	 * @param uriComponents the URI components
	 * @param encoding the character encoding to encode to
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUriComponents(Map<UriComponents.Type, String> uriComponents,
											 String encoding) throws UnsupportedEncodingException {
		Assert.notEmpty(uriComponents, "'uriComponents' must not be empty");
		Assert.hasLength(encoding, "'encoding' must not be empty");

		Map<UriComponents.Type, String> encodedUriComponents = new EnumMap<UriComponents.Type, String>(UriComponents.Type.class);
		for (Map.Entry<UriComponents.Type, String> entry : uriComponents.entrySet()) {
			if (entry.getValue() != null) {
				String encodedValue = encodeUriComponent(entry.getValue(), encoding, entry.getKey(), null);
				encodedUriComponents.put(entry.getKey(), encodedValue);
			}
		}
		return buildUri(encodedUriComponents);
	}

	/**
	 * Encodes the given source URI components into an encoded String. All various URI components are optional, but encoded
	 * according to their respective valid character sets.
	 *
	 * @param scheme the scheme
	 * @param authority the authority
	 * @param userInfo  the user info
	 * @param host the host
	 * @param port the port
	 * @param path the path
	 * @param query the query
	 * @param fragment the fragment
	 * @param encoding the character encoding to encode to
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUriComponents(String scheme,
											 String authority,
											 String userInfo ,
											 String host,
											 String port,
											 String path,
											 String query,
											 String fragment,
											 String encoding) throws UnsupportedEncodingException {
		Assert.hasLength(encoding, "'encoding' must not be empty");

		if (scheme != null) {
			scheme = encodeScheme(scheme, encoding);
		}
		if (authority != null) {
			authority = encodeAuthority(authority, encoding);
		}
		if (userInfo != null) {
			userInfo = encodeUserInfo(userInfo, encoding);
		}
		if (host != null) {
			host = encodeHost(host, encoding);
		}
		if (port != null) {
			port = encodePort(port, encoding);
		}
		if (path != null) {
			path = encodePath(path, encoding);
		}
		if (query != null) {
			query = encodeQuery(query, encoding);
		}
		if (fragment != null) {
			fragment = encodeFragment(fragment, encoding);
		}
		return buildUri(scheme, authority, userInfo, host, port, path, query, fragment);
	}

	/**
	 * Encodes the given source into an encoded String using the rules specified by the given component.
	 *
	 * @param source the source string
	 * @param uriComponent the URI component for the source
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 */
	public static String encodeUriComponent(String source, UriComponents.Type uriComponent) {
		return encodeUriComponent(source, uriComponent, null);
	}

	/**
	 * Encodes the given source into an encoded String using the rules specified by the given component and with the
	 * given options.
	 *
	 * @param source the source string
	 * @param encoding the encoding of the source string
	 * @param uriComponent the URI component for the source
	 * @param encodingOptions the options used when encoding. May be {@code null}.
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @see EncodingOption
	 */
	public static String encodeUriComponent(String source,
											UriComponents.Type uriComponent,
											Set<EncodingOption> encodingOptions) {
		try {
			return encodeUriComponent(source, DEFAULT_ENCODING, uriComponent, encodingOptions);
		}
		catch (UnsupportedEncodingException ex) {
			throw new InternalError("\"" + DEFAULT_ENCODING + "\" not supported");
		}
	}


	/**
	 * Encodes the given source into an encoded String using the rules specified by the given component.
	 *
	 * @param source the source string
	 * @param encoding the encoding of the source string
	 * @param uriComponent the URI component for the source
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 */
	public static String encodeUriComponent(String source,
											String encoding,
											UriComponents.Type uriComponent) throws UnsupportedEncodingException {
		return encodeUriComponent(source, encoding, uriComponent, null);
	}

	/**
	 * Encodes the given source into an encoded String using the rules specified by the given component and with the
	 * given options.
	 *
	 * @param source the source string
	 * @param encoding the encoding of the source string
	 * @param uriComponent the URI component for the source
	 * @param encodingOptions the options used when encoding. May be {@code null}.
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @see EncodingOption
	 */
	public static String encodeUriComponent(String source,
											String encoding,
											UriComponents.Type uriComponent,
											Set<EncodingOption> encodingOptions) throws UnsupportedEncodingException {
		Assert.hasLength(encoding, "'encoding' must not be empty");

		byte[] bytes = encodeInternal(source.getBytes(encoding), uriComponent, encodingOptions);
		return new String(bytes, "US-ASCII");
	}

	private static byte[] encodeInternal(byte[] source,
										 UriComponents.Type uriComponent,
										 Set<EncodingOption> encodingOptions) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(uriComponent, "'uriComponent' must not be null");

		if (encodingOptions == null) {
			encodingOptions = Collections.emptySet();
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length);
		for (int i = 0; i < source.length; i++) {
			int b = source[i];
			if (b < 0) {
				b += 256;
			}
			if (uriComponent.isAllowed(b)) {
				bos.write(b);
			}
			else if (encodingOptions.contains(EncodingOption.ALLOW_TEMPLATE_VARS) && (b == '{' || b == '}')) {
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

	// encoding convenience methods

	/**
	 * Encodes the given URI scheme with the given encoding.
	 *
	 * @param scheme the scheme to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded scheme
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeScheme(String scheme, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(scheme, encoding, UriComponents.Type.SCHEME, null);
	}

	/**
	 * Encodes the given URI authority with the given encoding.
	 *
	 * @param authority the authority to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded authority
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeAuthority(String authority, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(authority, encoding, UriComponents.Type.AUTHORITY, null);
	}

	/**
	 * Encodes the given URI user info with the given encoding.
	 *
	 * @param userInfo the user info to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded user info
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUserInfo(String userInfo, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(userInfo, encoding, UriComponents.Type.USER_INFO, null);
	}

	/**
	 * Encodes the given URI host with the given encoding.
	 *
	 * @param host the host to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded host
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeHost(String host, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(host, encoding, UriComponents.Type.HOST, null);
	}

	/**
	 * Encodes the given URI port with the given encoding.
	 *
	 * @param port the port to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded port
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePort(String port, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(port, encoding, UriComponents.Type.PORT, null);
	}

	/**
	 * Encodes the given URI path with the given encoding.
	 *
	 * @param path the path to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded path
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePath(String path, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(path, encoding, UriComponents.Type.PATH, null);
	}

	/**
	 * Encodes the given URI path segment with the given encoding.
	 *
	 * @param segment the segment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded segment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePathSegment(String segment, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(segment, encoding, UriComponents.Type.PATH_SEGMENT, null);
	}

	/**
	 * Encodes the given URI query with the given encoding.
	 *
	 * @param query the query to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQuery(String query, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(query, encoding, UriComponents.Type.QUERY, null);
	}

	/**
	 * Encodes the given URI query parameter with the given encoding.
	 *
	 * @param queryParam the query parameter to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query parameter
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQueryParam(String queryParam, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(queryParam, encoding, UriComponents.Type.QUERY_PARAM, null);
	}

	/**
	 * Encodes the given URI fragment with the given encoding.
	 *
	 * @param fragment the fragment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded fragment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeFragment(String fragment, String encoding) throws UnsupportedEncodingException {
		return encodeUriComponent(fragment, encoding, UriComponents.Type.FRAGMENT, null);
	}


	// decoding

	/**
	 * Decodes the given encoded source String into an URI. Based on the following rules:
	 * <ul>
	 *     <li>Alphanumeric characters {@code "a"} through {@code "z"}, {@code "A"} through {@code "Z"}, and
	 *     {@code "0"} through {@code "9"} stay the same.</li>
	 *     <li>Special characters {@code "-"}, {@code "_"}, {@code "."}, and {@code "*"} stay the same.</li>
	 *     <li>A sequence "<code>%<i>xy</i></code>" is interpreted as a hexadecimal representation of the character.</li>
 	 * </ul>
	 *
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
	 * Enumeration used to control how URIs are encoded.
	 */
	public enum EncodingOption {

		/**
		 * Allow for URI template variables to occur in the URI component (i.e. '{foo}')
		 */
		ALLOW_TEMPLATE_VARS

	}

}
