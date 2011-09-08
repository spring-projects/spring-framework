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
import java.util.EnumSet;
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
		String port = portAsString();
		String path = null;
		if (!pathSegments.isEmpty()) {
			StringBuilder pathBuilder = new StringBuilder();
			for (String pathSegment : pathSegments) {
				boolean startsWithSlash = pathSegment.charAt(0) == '/';
				boolean endsWithSlash = pathBuilder.length() > 0 && pathBuilder.charAt(pathBuilder.length() - 1) == '/';

				if (!endsWithSlash && !startsWithSlash) {
					pathBuilder.append('/');
				}
				else if (endsWithSlash && startsWithSlash) {
					pathSegment = pathSegment.substring(1);
				}
				pathBuilder.append(pathSegment);
			}
			path = pathBuilder.toString();
		}
		String query = queryAsString();

		String uri = UriUtils.buildUri(scheme, null, userInfo, host, port, path, query, fragment);

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
		return buildFromMap(uriVariables, true);
	}

	/**
	 * Builds a URI with the given URI template variables. Any template variable definitions found will be expanded with the
	 * given variables map. All variable values will not be encoded.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the resulting URI
	 */
	public URI buildFromEncoded(Map<String, ?> uriVariables) {
		return buildFromMap(uriVariables, false);
	}

	private URI buildFromMap(Map<String, ?> uriVariables, boolean encodeUriVariableValues) {
		if (CollectionUtils.isEmpty(uriVariables)) {
			return build();
		}
		String scheme = expand(this.scheme, UriComponent.SCHEME, uriVariables, encodeUriVariableValues);
		String userInfo = expand(this.userInfo, UriComponent.USER_INFO, uriVariables, encodeUriVariableValues);
		String host = expand(this.host, UriComponent.HOST, uriVariables, encodeUriVariableValues);
		String port = expand(this.portAsString(), UriComponent.PORT, uriVariables, encodeUriVariableValues);
		String path = null;
		if (!this.pathSegments.isEmpty()) {
			StringBuilder pathBuilder = new StringBuilder();
			for (String pathSegment : this.pathSegments) {
				boolean startsWithSlash = pathSegment.charAt(0) == '/';
				boolean endsWithSlash = pathBuilder.length() > 0 && pathBuilder.charAt(pathBuilder.length() - 1) == '/';

				if (!endsWithSlash && !startsWithSlash) {
					pathBuilder.append('/');
				}
				else if (endsWithSlash && startsWithSlash) {
					pathSegment = pathSegment.substring(1);
				}
				pathSegment = expand(pathSegment, UriComponent.PATH_SEGMENT, uriVariables, encodeUriVariableValues);
				pathBuilder.append(pathSegment);
			}
			path = pathBuilder.toString();
		}
		String query = expand(this.queryAsString(), UriComponent.QUERY, uriVariables, encodeUriVariableValues);
		String fragment = expand(this.fragment, UriComponent.FRAGMENT, uriVariables, encodeUriVariableValues);

		String uri = UriUtils.buildUri(scheme, null, userInfo, host, port, path, query, fragment);
		return URI.create(uri);
	}

	private String expand(String source,
						  UriComponent uriComponent,
						  Map<String, ?> uriVariables,
						  boolean encodeUriVariableValues) {
		if (source == null) {
			return null;
		}
		if (source.indexOf('{') == -1) {
			return source;
		}
		UriTemplate template = new UriComponentTemplate(source, uriComponent, encodeUriVariableValues);
		return template.expandAsString(uriVariables);
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
			this.scheme = encodeUriComponent(scheme, UriComponent.SCHEME);
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
			this.userInfo = encodeUriComponent(userInfo, UriComponent.USER_INFO);
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
			this.host = encodeUriComponent(host, UriComponent.HOST);
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

	private String portAsString() {
		return this.port != -1 ? Integer.toString(this.port) : null;
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
			this.pathSegments.add(encodeUriComponent(segment, UriComponent.PATH_SEGMENT));
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

		String encodedName = encodeUriComponent(name, UriComponent.QUERY_PARAM);

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
					queryBuilder.append(encodeUriComponent(valueAsString, UriComponent.QUERY_PARAM));
				}

			}
		}
		return this;
	}

	private String queryAsString() {
		return queryBuilder.length() != 0 ? queryBuilder.toString() : null;
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
			this.fragment = encodeUriComponent(fragment, UriComponent.FRAGMENT);
		}
		else {
			this.fragment = null;
		}
		return this;
	}


	private String encodeUriComponent(String source, UriComponent uriComponent) {
		return UriUtils.encodeUriComponent(source, uriComponent, EnumSet.of(UriUtils.EncodingOption.ALLOW_TEMPLATE_VARS));
	}


}
