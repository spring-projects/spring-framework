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
 * @since 3.0
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 */
public abstract class UriUtils {

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
	// encoding

	/**
	 * Encodes the given source URI into an encoded String. All various URI components are
	 * encoded according to their respective valid character sets.
	 * <p><strong>Note</strong> that this method does not attempt to encode "=" and "&" 
	 * characters in query parameter names and query parameter values because they cannot 
	 * be parsed in a reliable way. Instead use:
	 * <pre>
	 *  UriComponents uriComponents = UriComponentsBuilder.fromUri("/path?name={value}").buildAndExpand("a=b");
	 *  String encodedUri = uriComponents.encode().toUriString();
	 * </pre>
	 * @param uri the URI to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 * @deprecated in favor of {@link UriComponentsBuilder}; see note about query param encoding
	 */
	@Deprecated
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
	 * Encodes the given HTTP URI into an encoded String. All various URI components are
	 * encoded according to their respective valid character sets.
	 * <p><strong>Note</strong> that this method does not support fragments ({@code #}),
	 * as these are not supposed to be sent to the server, but retained by the client.
	 * <p><strong>Note</strong> that this method does not attempt to encode "=" and "&" 
	 * characters in query parameter names and query parameter values because they cannot 
	 * be parsed in a reliable way. Instead use:
	 * <pre>
	 *  UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl("/path?name={value}").buildAndExpand("a=b");
	 *  String encodedUri = uriComponents.encode().toUriString();
	 * </pre>
	 * @param httpUrl the HTTP URL to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded URL
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 * @deprecated in favor of {@link UriComponentsBuilder}; see note about query param encoding
	 */
	@Deprecated
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
	 * Encodes the given source URI components into an encoded String. All various URI components
	 * are optional, but encoded according to their respective valid character sets.
	 * @param scheme the scheme
	 * @param authority the authority
	 * @param userInfo the user info
	 * @param host the host
	 * @param port the port
	 * @param path the path
	 * @param query the query
	 * @param fragment the fragment
	 * @param encoding the character encoding to encode to
	 * @return the encoded URI
	 * @throws IllegalArgumentException when the given uri parameter is not a valid URI
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 * @deprecated in favor of {@link UriComponentsBuilder}
	 */
	@Deprecated
	public static String encodeUriComponents(String scheme, String authority, String userInfo,
			String host, String port, String path, String query, String fragment, String encoding)
			throws UnsupportedEncodingException {

        Assert.hasLength(encoding, "'encoding' must not be empty");
        StringBuilder sb = new StringBuilder();

        if (scheme != null) {
                sb.append(encodeScheme(scheme, encoding));
                sb.append(':');
        }

        if (authority != null) {
                sb.append("//");
                if (userInfo != null) {
                        sb.append(encodeUserInfo(userInfo, encoding));
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
	}


	// encoding convenience methods

	/**
	 * Encodes the given URI scheme with the given encoding.
	 * @param scheme the scheme to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded scheme
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeScheme(String scheme, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(scheme, encoding,
				HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * Encodes the given URI authority with the given encoding.
	 * @param authority the authority to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded authority
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeAuthority(String authority, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(authority, encoding,
				HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * Encodes the given URI user info with the given encoding.
	 * @param userInfo the user info to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded user info
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeUserInfo(String userInfo, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(userInfo, encoding,
				HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * Encodes the given URI host with the given encoding.
	 * @param host the host to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded host
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeHost(String host, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents
				.encodeUriComponent(host, encoding, HierarchicalUriComponents.Type.HOST);
	}

	/**
	 * Encodes the given URI port with the given encoding.
	 * @param port the port to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded port
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePort(String port, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents
				.encodeUriComponent(port, encoding, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * Encodes the given URI path with the given encoding.
	 * @param path the path to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded path
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePath(String path, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents
				.encodeUriComponent(path, encoding, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * Encodes the given URI path segment with the given encoding.
	 * @param segment the segment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded segment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodePathSegment(String segment, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(segment, encoding,
				HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * Encodes the given URI query with the given encoding.
	 * @param query the query to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQuery(String query, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents
				.encodeUriComponent(query, encoding, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * Encodes the given URI query parameter with the given encoding.
	 * @param queryParam the query parameter to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query parameter
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeQueryParam(String queryParam, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(queryParam, encoding,
				HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * Encodes the given URI fragment with the given encoding.
	 * @param fragment the fragment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded fragment
	 * @throws UnsupportedEncodingException when the given encoding parameter is not supported
	 */
	public static String encodeFragment(String fragment, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(fragment, encoding,
				HierarchicalUriComponents.Type.FRAGMENT);
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
	 * @param source the source string
	 * @param encoding the encoding
	 * @return the decoded URI
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
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
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
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

}
