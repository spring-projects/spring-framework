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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Builder for {@link URI} objects.
 *
 * <p>Typical usage involves:
 * <ol>
 *     <li>Create a {@code UriBuilder} with one of the static factory methods (such as {@link #fromPath(String)} or
 *     {@link #fromUri(URI)})</li>
 *     <li>Set the various URI components through the respective methods ({@link #scheme(String)},
 *     {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 *     {@link #pathSegment(String...)}, {@link #queryParam(String, Object...)}, and {@link #fragment(String)}.</li>
 *     <li>Build the URI with one of the {@link #build} method variants.</li>
 * </ol>
 *
 * <p>Most of the URI component methods accept URI template variables (i.e. {@code "{foo}"}), which are expanded by
 * calling {@code build}.
 * one , are allowed in most components of a URI but their value is
 * restricted to a particular component. E.g.
 * <blockquote><code>UriBuilder.fromPath("{arg1}").build("foo#bar");</code></blockquote>
 * would result in encoding of the '#' such that the resulting URI is
 * "foo%23bar". To create a URI "foo#bar" use
 * <blockquote><code>UriBuilder.fromPath("{arg1}").fragment("{arg2}").build("foo", "bar")</code></blockquote>
 * instead. URI template names and delimiters are never encoded but their
 * values are encoded when a URI is built.
 * Template parameter regular expressions are ignored when building a URI, i.e.
 * no validation is performed.
 *
 * <p>Inspired by {@link javax.ws.rs.core.UriBuilder}.
 *
 * @author Arjen Poutsma
 * @since 3.1
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 */
public class UriBuilder {

	private String scheme;

	private String userInfo;

	private String host;

	private int port = -1;

	private final List<String> pathSegments = new ArrayList<String>();

	private final StringBuilder queryBuilder = new StringBuilder();

	private String fragment;

	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 *
	 * @see #newInstance()
	 * @see #fromPath(String)
	 * @see #fromUri(URI)
	 */
	protected UriBuilder() {
	}

	// Factory methods

	/**
	 * Returns a new, empty URI builder.
	 *
	 * @return the new {@code UriBuilder}
	 */
	public static UriBuilder newInstance() {
		return new UriBuilder();
	}

	/**
	 * Returns a URI builder that is initialized with the given path.
	 *
	 * @param path the path to initialize with
	 * @return the new {@code UriBuilder}
	 */
	public static UriBuilder fromPath(String path) {
		UriBuilder builder = new UriBuilder();
		builder.path(path);
		return builder;
	}

	/**
	 * Returns a URI builder that is initialized with the given {@code URI}.
	 *
	 * @param uri the URI to initialize with
	 * @return the new {@code UriBuilder}
	 */
	public static UriBuilder fromUri(URI uri) {
		UriBuilder builder = new UriBuilder();
		builder.uri(uri);
		return builder;
	}

	// build methods

	/**
	 * Builds a URI with no URI template variables. Any template variable definitions found will be encoded (i.e.
	 * {@code "/{foo}"} will result in {@code "/%7Bfoo%7D"}.
	 * @return the resulting URI
	 */
	public URI build() {
		StringBuilder uriBuilder = new StringBuilder();

		if (scheme != null) {
			uriBuilder.append(scheme);
			uriBuilder.append(':');
		}

		if (userInfo != null || host != null || port != -1) {
			uriBuilder.append("//");

			if (StringUtils.hasLength(userInfo)) {
				uriBuilder.append(userInfo);
				uriBuilder.append('@');
			}

			if (host != null) {
				uriBuilder.append(host);
			}

			if (port != -1) {
				uriBuilder.append(':');
				uriBuilder.append(port);
			}
		}

		if (!pathSegments.isEmpty()) {
			for (String pathSegment : pathSegments) {
				boolean startsWithSlash = pathSegment.charAt(0) == '/';
				boolean endsWithSlash = uriBuilder.length() > 0 && uriBuilder.charAt(uriBuilder.length() - 1) == '/';

				if (!endsWithSlash && !startsWithSlash) {
					uriBuilder.append('/');
				}
				else if (endsWithSlash && startsWithSlash) {
					pathSegment = pathSegment.substring(1);
				}
				uriBuilder.append(pathSegment);
			}
		}

		if (queryBuilder.length() > 0) {
			uriBuilder.append('?');
			uriBuilder.append(queryBuilder);
		}

		if (StringUtils.hasLength(fragment)) {
			uriBuilder.append('#');
			uriBuilder.append(fragment);
		}
		String uri = uriBuilder.toString();

		uri = StringUtils.replace(uri, "{", "%7B");
		uri = StringUtils.replace(uri, "}", "%7D");

		return URI.create(uri);
	}

	/**
	 * Builds a URI with the given URI template variables. Any template variable definitions found will be expanded with
	 * the given variables map. All variable values will be encoded in accordance with the encoding rules for the URI
	 * component they occur in.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the resulting URI
	 */
	public URI build(Map<String, ?> uriVariables) {
		return buildFromMap(true, uriVariables);
	}

	/**
	 * Builds a URI with the given URI template variables. Any template variable definitions found will be expanded with the
	 * given variables map. All variable values will not be encoded.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the resulting URI
	 */
	public URI buildFromEncoded(Map<String, ?> uriVariables) {
		return buildFromMap(false, uriVariables);
	}

	private URI buildFromMap(boolean encodeUriVariableValues, Map<String, ?> uriVariables) {
		if (CollectionUtils.isEmpty(uriVariables)) {
			return build();
		}

		StringBuilder uriBuilder = new StringBuilder();

		UriTemplate template;

		if (scheme != null) {
			template = new UriComponentTemplate(scheme, UriComponent.SCHEME, encodeUriVariableValues);
			uriBuilder.append(template.expandAsString(uriVariables));
			uriBuilder.append(':');
		}

		if (userInfo != null || host != null || port != -1) {
			uriBuilder.append("//");

			if (StringUtils.hasLength(userInfo)) {
				template = new UriComponentTemplate(userInfo, UriComponent.USER_INFO, encodeUriVariableValues);
				uriBuilder.append(template.expandAsString(uriVariables));
				uriBuilder.append('@');
			}

			if (host != null) {
				template = new UriComponentTemplate(host, UriComponent.HOST, encodeUriVariableValues);
				uriBuilder.append(template.expandAsString(uriVariables));
			}

			if (port != -1) {
				uriBuilder.append(':');
				uriBuilder.append(port);
			}
		}

		if (!pathSegments.isEmpty()) {
			for (String pathSegment : pathSegments) {
				boolean startsWithSlash = pathSegment.charAt(0) == '/';
				boolean endsWithSlash = uriBuilder.length() > 0 && uriBuilder.charAt(uriBuilder.length() - 1) == '/';

				if (!endsWithSlash && !startsWithSlash) {
					uriBuilder.append('/');
				}
				else if (endsWithSlash && startsWithSlash) {
					pathSegment = pathSegment.substring(1);
				}
				template = new UriComponentTemplate(pathSegment, UriComponent.PATH_SEGMENT, encodeUriVariableValues);
				uriBuilder.append(template.expandAsString(uriVariables));
			}
		}
		if (queryBuilder.length() > 0) {
			uriBuilder.append('?');
			template = new UriComponentTemplate(queryBuilder.toString(), UriComponent.QUERY, encodeUriVariableValues);
			uriBuilder.append(template.expandAsString(uriVariables));
		}

		if (StringUtils.hasLength(fragment)) {
			uriBuilder.append('#');
			template = new UriComponentTemplate(fragment, UriComponent.FRAGMENT, encodeUriVariableValues);
			uriBuilder.append(template.expandAsString(uriVariables));
		}

		return URI.create(uriBuilder.toString());
	}

	/**
	 * Builds a URI with the given URI template variable values. Any template variable definitions found will be expanded
	 * with the given variables. All variable values will be encoded in accordance with the encoding rules for the URI
	 * component they occur in.
	 *
	 * @param uriVariableValues the array of URI variables
	 * @return the resulting URI
	 */
	public URI build(Object... uriVariableValues) {
		return buildFromVarArg(true, uriVariableValues);
	}

	/**
	 * Builds a URI with the given URI template variable values. Any template variable definitions found will be expanded
	 * with the given variables. All variable values will not be encoded.
	 *
	 * @param uriVariableValues the array of URI variables
	 * @return the resulting URI
	 */
	public URI buildFromEncoded(Object... uriVariableValues) {
		return buildFromVarArg(false, uriVariableValues);
	}

	private URI buildFromVarArg(boolean encodeUriVariableValues, Object... uriVariableValues) {
		if (ObjectUtils.isEmpty(uriVariableValues)) {
			return build();
		}

		StringBuilder uriBuilder = new StringBuilder();

		UriTemplate template;

		if (scheme != null) {
			template = new UriComponentTemplate(scheme, UriComponent.SCHEME, encodeUriVariableValues);
			uriBuilder.append(template.expandAsString(uriVariableValues));
			uriBuilder.append(':');
		}

		if (userInfo != null || host != null || port != -1) {
			uriBuilder.append("//");

			if (StringUtils.hasLength(userInfo)) {
				template = new UriComponentTemplate(userInfo, UriComponent.USER_INFO, encodeUriVariableValues);
				uriBuilder.append(template.expandAsString(uriVariableValues));
				uriBuilder.append('@');
			}

			if (host != null) {
				template = new UriComponentTemplate(host, UriComponent.HOST, encodeUriVariableValues);
				uriBuilder.append(template.expandAsString(uriVariableValues));
			}

			if (port != -1) {
				uriBuilder.append(':');
				uriBuilder.append(port);
			}
		}

		if (!pathSegments.isEmpty()) {
			for (String pathSegment : pathSegments) {
				boolean startsWithSlash = pathSegment.charAt(0) == '/';
				boolean endsWithSlash = uriBuilder.length() > 0 && uriBuilder.charAt(uriBuilder.length() - 1) == '/';

				if (!endsWithSlash && !startsWithSlash) {
					uriBuilder.append('/');
				}
				else if (endsWithSlash && startsWithSlash) {
					pathSegment = pathSegment.substring(1);
				}
				template = new UriComponentTemplate(pathSegment, UriComponent.PATH_SEGMENT, encodeUriVariableValues);
				uriBuilder.append(template.expandAsString(uriVariableValues));
			}
		}

		if (queryBuilder.length() > 0) {
			uriBuilder.append('?');
			template = new UriComponentTemplate(queryBuilder.toString(), UriComponent.QUERY, encodeUriVariableValues);
			uriBuilder.append(template.expandAsString(uriVariableValues));
		}

		if (StringUtils.hasLength(fragment)) {
			uriBuilder.append('#');
			template = new UriComponentTemplate(fragment, UriComponent.FRAGMENT, encodeUriVariableValues);
			uriBuilder.append(template.expandAsString(uriVariableValues));
		}

		return URI.create(uriBuilder.toString());
	}

	// URI components methods

	/**
	 * Initializes all components of this URI builder with the components of the given URI.
	 *
	 * @param uri the URI
	 * @return this UriBuilder
	 */
	public UriBuilder uri(URI uri) {
		Assert.notNull(uri, "'uri' must not be null");
		Assert.isTrue(!uri.isOpaque(), "Opaque URI [" + uri + "] not supported");

		this.scheme = uri.getScheme();

		if (uri.getRawUserInfo() != null) {
			this.userInfo = uri.getRawUserInfo();
		}
		if (uri.getHost() != null) {
			this.host = uri.getHost();
		}
		if (uri.getPort() != -1) {
			this.port = uri.getPort();
		}
		if (StringUtils.hasLength(uri.getRawPath())) {
			String[] pathSegments = StringUtils.tokenizeToStringArray(uri.getRawPath(), "/");

			this.pathSegments.clear();
			Collections.addAll(this.pathSegments, pathSegments);
		}
		if (StringUtils.hasLength(uri.getRawQuery())) {
			this.queryBuilder.setLength(0);
			this.queryBuilder.append(uri.getRawQuery());
		}
		if (uri.getRawFragment() != null) {
			this.fragment = uri.getRawFragment();
		}
		return this;
	}

	/**
	 * Sets the URI scheme. The given scheme may contain URI template variables, and may also be {@code null} to clear the
	 * scheme of this builder.
	 *
	 * @param scheme the URI scheme
	 * @return this UriBuilder
	 */
	public UriBuilder scheme(String scheme) {
		if (scheme != null) {
			Assert.hasLength(scheme, "'scheme' must not be empty");
			this.scheme = UriUtils.encode(scheme, UriComponent.SCHEME, true);
		}
		else {
			this.scheme = null;
		}
		return this;
	}

	/**
	 * Sets the URI user info. The given user info may contain URI template variables, and may also be {@code null} to
	 * clear the user info of this builder.
	 *
	 * @param userInfo the URI user info
	 * @return this UriBuilder
	 */
	public UriBuilder userInfo(String userInfo) {
		if (userInfo != null) {
			Assert.hasLength(userInfo, "'userInfo' must not be empty");
			this.userInfo = UriUtils.encode(userInfo, UriComponent.USER_INFO, true);
		}
		else {
			this.userInfo = null;
		}
		return this;
	}

	/**
	 * Sets the URI host. The given host may contain URI template variables, and may also be {@code null} to clear the host
	 * of this builder.
	 *
	 * @param host the URI host
	 * @return this UriBuilder
	 */
	public UriBuilder host(String host) {
		if (host != null) {
			Assert.hasLength(host, "'host' must not be empty");
			this.host = UriUtils.encode(host, UriComponent.HOST, true);
		}
		else {
			this.host = null;
		}
		return this;
	}

	/**
	 * Sets the URI port. Passing {@code -1} will clear the port of this builder.
	 *
	 * @param port the URI port
	 * @return this UriBuilder
	 */
	public UriBuilder port(int port) {
		Assert.isTrue(port >= -1, "'port' must not be < -1");
		this.port = port;
		return this;
	}

	/**
	 * Appends the given path to the existing path of this builder. The given path may contain URI template variables.
	 *
	 * @param path the URI path
	 * @return this UriBuilder
	 */
	public UriBuilder path(String path) {
		Assert.notNull(path, "path must not be null");

		String[] pathSegments = StringUtils.tokenizeToStringArray(path, "/");
		return pathSegment(pathSegments);
	}

	/**
	 * Appends the given path segments to the existing path of this builder. Each given path segments may contain URI
	 * template variables.
	 *
	 * @param segments the URI path segments
	 * @return this UriBuilder
	 */
	public UriBuilder pathSegment(String... segments) throws IllegalArgumentException {
		Assert.notNull(segments, "'segments' must not be null");
		for (String segment : segments) {
			this.pathSegments.add(UriUtils.encode(segment, UriComponent.PATH_SEGMENT, true));
		}

		return this;
	}

	/**
	 * Appends the given query parameter to the existing query parameters. The given name or any of the values may contain
	 * URI template variables. If no values are given, the resulting URI will contain the query parameter name only (i.e.
	 * {@code ?foo} instead of {@code ?foo=bar}.
	 *
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @return this UriBuilder
	 */
	public UriBuilder queryParam(String name, Object... values) {
		Assert.notNull(name, "'name' must not be null");

		String encodedName = UriUtils.encode(name, UriComponent.QUERY_PARAM, true);

		if (ObjectUtils.isEmpty(values)) {
			if (queryBuilder.length() != 0) {
				queryBuilder.append('&');
			}
			queryBuilder.append(encodedName);
		}
		else {
			for (Object value : values) {
				if (queryBuilder.length() != 0) {
					queryBuilder.append('&');
				}
				queryBuilder.append(encodedName);

				String valueAsString = value != null ? value.toString() : "";
				if (valueAsString.length() != 0) {
					queryBuilder.append('=');
					queryBuilder.append(UriUtils.encode(valueAsString, UriComponent.QUERY_PARAM, true));
				}

			}
		}
		return this;
	}

	/**
	 * Sets the URI fragment. The given fragment may contain URI template variables, and may also be {@code null} to clear
	 * the fragment of this builder.
	 *
	 * @param fragment the URI fragment
	 * @return this UriBuilder
	 */
	public UriBuilder fragment(String fragment) {
		if (fragment != null) {
			Assert.hasLength(fragment, "'fragment' must not be empty");
			this.fragment = UriUtils.encode(fragment, UriComponent.FRAGMENT, true);
		}
		else {
			this.fragment = null;
		}
		return this;
	}


}
