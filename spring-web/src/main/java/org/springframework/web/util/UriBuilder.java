/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.util;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Builder-style methods to prepare and expand a URI template with variables.
 *
 * <p>Effectively a generalization of {@link UriComponentsBuilder} but with
 * shortcuts to expand directly into {@link URI} rather than
 * {@link UriComponents} and also leaving common concerns such as encoding
 * preferences, a base URI, and others as implementation concerns.
 *
 * <p>Typically obtained via {@link UriBuilderFactory} which serves as a central
 * component configured once and used to create many URLs.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see UriBuilderFactory
 * @see UriComponentsBuilder
 */
public interface UriBuilder {

	/**
	 * Set the URI scheme which may contain URI template variables,
	 * and may also be {@code null} to clear the scheme of this builder.
	 * @param scheme the URI scheme
	 */
	UriBuilder scheme(@Nullable String scheme);

	/**
	 * Set the URI user info which may contain URI template variables, and
	 * may also be {@code null} to clear the user info of this builder.
	 * @param userInfo the URI user info
	 */
	UriBuilder userInfo(@Nullable String userInfo);

	/**
	 * Set the URI host which may contain URI template variables, and may also
	 * be {@code null} to clear the host of this builder.
	 * @param host the URI host
	 */
	UriBuilder host(@Nullable String host);

	/**
	 * Set the URI port. Passing {@code -1} will clear the port of this builder.
	 * @param port the URI port
	 */
	UriBuilder port(int port);

	/**
	 * Set the URI port. Use this method only when the port needs to be
	 * parameterized with a URI variable. Otherwise use {@link #port(int)}.
	 * Passing {@code null} will clear the port of this builder.
	 * @param port the URI port
	 */
	UriBuilder port(@Nullable String port);

	/**
	 * Append to the path of this builder.
	 * <p>The given value is appended as-is to previous {@link #path(String) path}
	 * values without inserting any additional slashes. For example:
	 * <pre class="code">
	 *
	 * builder.path("/first-").path("value/").path("/{id}").build("123")
	 *
	 * // Results is "/first-value/123"
	 * </pre>
	 * <p>By contrast {@link #pathSegment(String...) pathSegment} does insert
	 * slashes between individual path segments. For example:
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value").path("/")
	 *
	 * // Results is "/first-value/second-value/"
	 * </pre>
	 * <p>The resulting full path is normalized to eliminate duplicate slashes.
	 * <p><strong>Note:</strong> When inserting a URI variable value that
	 * contains slashes in a {@link #path(String) path}, whether those are
	 * encoded depends on the configured encoding mode. For more details, see
	 * {@link UriComponentsBuilder#encode()}, or otherwise if building URIs
	 * indirectly via {@code WebClient} or {@code RestTemplate}, see its
	 * {@link DefaultUriBuilderFactory#setEncodingMode encodingMode}.
	 * Also see the <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding">
	 * URI Encoding</a> section of the reference docs.
	 * @param path the URI path
	 */
	UriBuilder path(String path);

	/**
	 * Override the current path.
	 * @param path the URI path, or {@code null} for an empty path
	 */
	UriBuilder replacePath(@Nullable String path);

	/**
	 * Append to the path using path segments. For example:
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value", "{id}").build("123")
	 *
	 * // Results is "/first-value/second-value/123"
	 * </pre>
	 * <p>If slashes are present in a path segment, they are encoded:
	 * <pre class="code">
	 *
	 * builder.pathSegment("ba/z", "{id}").build("a/b")
	 *
	 * // Results is "/ba%2Fz/a%2Fb"
	 * </pre>
	 * To insert a trailing slash, use the {@link #path} builder method:
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value").path("/")
	 *
	 * // Results is "/first-value/second-value/"
	 * </pre>
	 * <p>Empty path segments are ignored and therefore duplicate slashes do not
	 * appear in the resulting full path.
	 * @param pathSegments the URI path segments
	 */
	UriBuilder pathSegment(String... pathSegments) throws IllegalArgumentException;

	/**
	 * Parse the given query string into query parameters where parameters are
	 * separated with {@code '&'} and their values, if any, with {@code '='}.
	 * The query may contain URI template variables.
	 * <p><strong>Note: </strong> please, review the Javadoc of
	 * {@link #queryParam(String, Object...)} for further notes on the treatment
	 * and encoding of individual query parameters.
	 * @param query the query string
	 */
	UriBuilder query(String query);

	/**
	 * Clear existing query parameters and then delegate to {@link #query(String)}.
	 * <p><strong>Note: </strong> please, review the Javadoc of
	 * {@link #queryParam(String, Object...)} for further notes on the treatment
	 * and encoding of individual query parameters.
	 * @param query the query string; a {@code null} value removes all query parameters.
	 */
	UriBuilder replaceQuery(@Nullable String query);

	/**
	 * Append the given query parameter. Both the parameter name and values may
	 * contain URI template variables to be expanded later from values. If no
	 * values are given, the resulting URI will contain the query parameter name
	 * only, e.g. {@code "?foo"} instead of {@code "?foo=bar"}.
	 * <p><strong>Note:</strong> encoding, if applied, will only encode characters
	 * that are illegal in a query parameter name or value such as {@code "="}
	 * or {@code "&"}. All others that are legal as per syntax rules in
	 * <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> are not
	 * encoded. This includes {@code "+"} which sometimes needs to be encoded
	 * to avoid its interpretation as an encoded space. Stricter encoding may
	 * be applied by using a URI template variable along with stricter encoding
	 * on variable values. For more details please read the
	 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding">"URI Encoding"</a>
	 * section of the Spring Framework reference.
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @see #queryParam(String, Collection)
	 */
	UriBuilder queryParam(String name, Object... values);

	/**
	 * Variant of {@link #queryParam(String, Object...)} with a Collection.
	 * <p><strong>Note: </strong> please, review the Javadoc of
	 * {@link #queryParam(String, Object...)} for further notes on the treatment
	 * and encoding of individual query parameters.
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @since 5.2
	 * @see #queryParam(String, Object...)
	 */
	UriBuilder queryParam(String name, @Nullable Collection<?> values);

	/**
	 * Delegates to either {@link #queryParam(String, Object...)} or
	 * {@link #queryParam(String, Collection)} if the given {@link Optional} has
	 * a value, or else if it is empty, no query parameter is added at all.
	 * @param name the query parameter name
	 * @param value an Optional, either empty or holding the query parameter value.
	 * @since 5.3
	 */
	UriBuilder queryParamIfPresent(String name, Optional<?> value);

	/**
	 * Add multiple query parameters and values.
	 * <p><strong>Note: </strong> please, review the Javadoc of
	 * {@link #queryParam(String, Object...)} for further notes on the treatment
	 * and encoding of individual query parameters.
	 * @param params the params
	 */
	UriBuilder queryParams(MultiValueMap<String, String> params);

	/**
	 * Set the query parameter values replacing existing values, or if no
	 * values are given, the query parameter is removed.
	 * <p><strong>Note: </strong> please, review the Javadoc of
	 * {@link #queryParam(String, Object...)} for further notes on the treatment
	 * and encoding of individual query parameters.
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @see #replaceQueryParam(String, Collection)
	 */
	UriBuilder replaceQueryParam(String name, Object... values);

	/**
	 * Variant of {@link #replaceQueryParam(String, Object...)} with a Collection.
	 * <p><strong>Note: </strong> please, review the Javadoc of
	 * {@link #queryParam(String, Object...)} for further notes on the treatment
	 * and encoding of individual query parameters.
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @since 5.2
	 * @see #replaceQueryParam(String, Object...)
	 */
	UriBuilder replaceQueryParam(String name, @Nullable Collection<?> values);

	/**
	 * Set the query parameter values after removing all existing ones.
	 * <p><strong>Note: </strong> please, review the Javadoc of
	 * {@link #queryParam(String, Object...)} for further notes on the treatment
	 * and encoding of individual query parameters.
	 * @param params the query parameter name
	 */
	UriBuilder replaceQueryParams(MultiValueMap<String, String> params);

	/**
	 * Set the URI fragment. The given fragment may contain URI template variables,
	 * and may also be {@code null} to clear the fragment of this builder.
	 * @param fragment the URI fragment
	 */
	UriBuilder fragment(@Nullable String fragment);

	/**
	 * Build a {@link URI} instance and replaces URI template variables
	 * with the values from an array.
	 * @param uriVariables the map of URI variables
	 * @return the URI
	 */
	URI build(Object... uriVariables);

	/**
	 * Build a {@link URI} instance and replaces URI template variables
	 * with the values from a map.
	 * @param uriVariables the map of URI variables
	 * @return the URI
	 */
	URI build(Map<String, ?> uriVariables);

	/**
	 * Return a String representation of the URI by concatenating all URI
	 * component values into the fully formed URI String. Implementing classes
	 * should perform simple String concatenation of current URI component
	 * values to preserve URI template placeholders.
	 * @since 6.1.2
	 */
	String toUriString();

}
