/*
 * Copyright 2002-2018 the original author or authors.
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

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Utility methods for URI encoding and decoding based on RFC 3986.
 *
 * <p>There are two types of encode methods:
 * <ul>
 * <li>{@code "encodeXyz"} -- these encode a specific URI component (e.g. path,
 * query) by percent encoding illegal characters, which includes non-US-ASCII
 * characters, and also characters that are otherwise illegal within the given
 * URI component type, as defined in RFC 3986. The effect of this method, with
 * regards to encoding, is comparable to using the multi-argument constructor
 * of {@link URI}.
 * <li>{@code "encode"} and {@code "encodeUriVariables"} -- these can be used
 * to encode URI variable values by percent encoding all characters that are
 * either illegal, or have any reserved meaning, anywhere within a URI.
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 */
public abstract class UriUtils {

	/**
	 * Encode the given URI scheme with the given encoding.
	 * @param scheme the scheme to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded scheme
	 */
	public static String encodeScheme(String scheme, String encoding) {
		return encode(scheme, encoding, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * Encode the given URI scheme with the given encoding.
	 * @param scheme the scheme to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded scheme
	 * @since 5.0
	 */
	public static String encodeScheme(String scheme, Charset charset) {
		return encode(scheme, charset, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * Encode the given URI authority with the given encoding.
	 * @param authority the authority to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded authority
	 */
	public static String encodeAuthority(String authority, String encoding) {
		return encode(authority, encoding, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * Encode the given URI authority with the given encoding.
	 * @param authority the authority to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded authority
	 * @since 5.0
	 */
	public static String encodeAuthority(String authority, Charset charset) {
		return encode(authority, charset, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * Encode the given URI user info with the given encoding.
	 * @param userInfo the user info to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded user info
	 */
	public static String encodeUserInfo(String userInfo, String encoding) {
		return encode(userInfo, encoding, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * Encode the given URI user info with the given encoding.
	 * @param userInfo the user info to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded user info
	 * @since 5.0
	 */
	public static String encodeUserInfo(String userInfo, Charset charset) {
		return encode(userInfo, charset, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * Encode the given URI host with the given encoding.
	 * @param host the host to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded host
	 */
	public static String encodeHost(String host, String encoding) {
		return encode(host, encoding, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * Encode the given URI host with the given encoding.
	 * @param host the host to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded host
	 * @since 5.0
	 */
	public static String encodeHost(String host, Charset charset) {
		return encode(host, charset, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * Encode the given URI port with the given encoding.
	 * @param port the port to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded port
	 */
	public static String encodePort(String port, String encoding) {
		return encode(port, encoding, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * Encode the given URI port with the given encoding.
	 * @param port the port to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded port
	 * @since 5.0
	 */
	public static String encodePort(String port, Charset charset) {
		return encode(port, charset, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * Encode the given URI path with the given encoding.
	 * @param path the path to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded path
	 */
	public static String encodePath(String path, String encoding) {
		return encode(path, encoding, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * Encode the given URI path with the given encoding.
	 * @param path the path to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded path
	 * @since 5.0
	 */
	public static String encodePath(String path, Charset charset) {
		return encode(path, charset, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * Encode the given URI path segment with the given encoding.
	 * @param segment the segment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded segment
	 */
	public static String encodePathSegment(String segment, String encoding) {
		return encode(segment, encoding, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * Encode the given URI path segment with the given encoding.
	 * @param segment the segment to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded segment
	 * @since 5.0
	 */
	public static String encodePathSegment(String segment, Charset charset) {
		return encode(segment, charset, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * Encode the given URI query with the given encoding.
	 * @param query the query to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query
	 */
	public static String encodeQuery(String query, String encoding) {
		return encode(query, encoding, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * Encode the given URI query with the given encoding.
	 * @param query the query to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded query
	 * @since 5.0
	 */
	public static String encodeQuery(String query, Charset charset) {
		return encode(query, charset, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * Encode the given URI query parameter with the given encoding.
	 * @param queryParam the query parameter to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query parameter
	 */
	public static String encodeQueryParam(String queryParam, String encoding) {

		return encode(queryParam, encoding, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * Encode the given URI query parameter with the given encoding.
	 * @param queryParam the query parameter to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded query parameter
	 * @since 5.0
	 */
	public static String encodeQueryParam(String queryParam, Charset charset) {
		return encode(queryParam, charset, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * Encode the given URI fragment with the given encoding.
	 * @param fragment the fragment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded fragment
	 */
	public static String encodeFragment(String fragment, String encoding) {
		return encode(fragment, encoding, HierarchicalUriComponents.Type.FRAGMENT);
	}

	/**
	 * Encode the given URI fragment with the given encoding.
	 * @param fragment the fragment to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded fragment
	 * @since 5.0
	 */
	public static String encodeFragment(String fragment, Charset charset) {
		return encode(fragment, charset, HierarchicalUriComponents.Type.FRAGMENT);
	}


	/**
	 * Variant of {@link #decode(String, Charset)} with a String charset.
	 * @param source the String to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded String
	 */
	public static String encode(String source, String encoding) {
		return encode(source, encoding, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * Encode all characters that are either illegal, or have any reserved
	 * meaning, anywhere within a URI, as defined in
	 * <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>.
	 * This is useful to ensure that the given String will be preserved as-is
	 * and will not have any o impact on the structure or meaning of the URI.
	 * @param source the String to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded String
	 * @since 5.0
	 */
	public static String encode(String source, Charset charset) {
		return encode(source, charset, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * Convenience method to apply {@link #encode(String, Charset)} to all
	 * given URI variable values.
	 * @param uriVariables the URI variable values to be encoded
	 * @return the encoded String
	 * @since 5.0
	 */
	public static Map<String, String> encodeUriVariables(Map<String, ?> uriVariables) {
		Map<String, String> result = new LinkedHashMap<>(uriVariables.size());
		uriVariables.forEach((key, value) -> {
			String stringValue = (value != null ? value.toString() : "");
			result.put(key, encode(stringValue, StandardCharsets.UTF_8));
		});
		return result;
	}

	/**
	 * Convenience method to apply {@link #encode(String, Charset)} to all
	 * given URI variable values.
	 * @param uriVariables the URI variable values to be encoded
	 * @return the encoded String
	 * @since 5.0
	 */
	public static Object[] encodeUriVariables(Object... uriVariables) {
		return Arrays.stream(uriVariables)
				.map(value -> {
					String stringValue = (value != null ? value.toString() : "");
					return encode(stringValue, StandardCharsets.UTF_8);
				})
				.toArray();
	}

	private static String encode(String scheme, String encoding, HierarchicalUriComponents.Type type) {
		return HierarchicalUriComponents.encodeUriComponent(scheme, encoding, type);
	}

	private static String encode(String scheme, Charset charset, HierarchicalUriComponents.Type type) {
		return HierarchicalUriComponents.encodeUriComponent(scheme, charset, type);
	}


	/**
	 * Decode the given encoded URI component.
	 * <p>See {@link StringUtils#uriDecode(String, Charset)} for the decoding rules.
	 * @param source the encoded String
	 * @param encoding the character encoding to use
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, String encoding) {
		return StringUtils.uriDecode(source, Charset.forName(encoding));
	}

	/**
	 * Decode the given encoded URI component.
	 * <p>See {@link StringUtils#uriDecode(String, Charset)} for the decoding rules.
	 * @param source the encoded String
	 * @param charset the character encoding to use
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @since 5.0
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, Charset charset) {
		return StringUtils.uriDecode(source, charset);
	}

	/**
	 * Extract the file extension from the given URI path.
	 * @param path the URI path (e.g. "/products/index.html")
	 * @return the extracted file extension (e.g. "html")
	 * @since 4.3.2
	 */
	@Nullable
	public static String extractFileExtension(String path) {
		int end = path.indexOf('?');
		int fragmentIndex = path.indexOf('#');
		if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
			end = fragmentIndex;
		}
		if (end == -1) {
			end = path.length();
		}
		int begin = path.lastIndexOf('/', end) + 1;
		int paramIndex = path.indexOf(';', begin);
		end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
		int extIndex = path.lastIndexOf('.', end);
		if (extIndex != -1 && extIndex > begin) {
			return path.substring(extIndex + 1, end);
		}
		return null;
	}

}
