/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HierarchicalUriComponents.PathComponent;

/**
 * Builder for {@link UriComponents}.
 *
 * <p></p>Typical usage involves:
 * <ol>
 *     <li>Create a {@code UriComponentsBuilder} with one of the static factory methods (such as
 *     {@link #fromPath(String)} or {@link #fromUri(URI)})</li>
 *     <li>Set the various URI components through the respective methods ({@link #scheme(String)},
 *     {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 *     {@link #pathSegment(String...)}, {@link #queryParam(String, Object...)}, and
 *     {@link #fragment(String)}.</li>
 *     <li>Build the {@link UriComponents} instance with the {@link #build()} method.</li>
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @since 3.1
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 */
public class UriComponentsBuilder {

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

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


	private String scheme;

	private String ssp;

	private String userInfo;

	private String host;

	private int port = -1;

	private CompositePathComponentBuilder pathBuilder = new CompositePathComponentBuilder();

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();

	private String fragment;

	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 *
	 * @see #newInstance()
	 * @see #fromPath(String)
	 * @see #fromUri(URI)
	 */
	protected UriComponentsBuilder() {
	}

	// Factory methods

	/**
	 * Returns a new, empty builder.
	 *
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder newInstance() {
		return new UriComponentsBuilder();
	}

	/**
	 * Returns a builder that is initialized with the given path.
	 *
	 * @param path the path to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromPath(String path) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.path(path);
		return builder;
	}

	/**
	 * Returns a builder that is initialized with the given {@code URI}.
	 *
	 * @param uri the URI to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * Returns a builder that is initialized with the given URI string.
	 *
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 *
	 * <pre>
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param uri
	 *            the URI string to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUriString(String uri) {
		Assert.hasLength(uri, "'uri' must not be empty");
		Matcher m = URI_PATTERN.matcher(uri);
		if (m.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();

			String scheme = m.group(2);
			String userInfo = m.group(5);
			String host = m.group(6);
			String port = m.group(8);
			String path = m.group(9);
			String query = m.group(11);
			String fragment = m.group(13);

			boolean opaque = false;

			if (StringUtils.hasLength(scheme)) {
				String s = uri.substring(scheme.length());
				if (!s.startsWith(":/")) {
					opaque = true;
				}
			}

			builder.scheme(scheme);


			if (opaque) {
				String ssp = uri.substring(scheme.length()).substring(1);
				if (StringUtils.hasLength(fragment)) {
					ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
				}
				builder.schemeSpecificPart(ssp);
			}
			else {
				builder.userInfo(userInfo);
				builder.host(host);
				if (StringUtils.hasLength(port)) {
					builder.port(Integer.parseInt(port));
				}
				builder.path(path);
				builder.query(query);
			}

			if (StringUtils.hasText(fragment)) {
				builder.fragment(fragment);
			}

			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * Creates a new {@code UriComponents} object from the string HTTP URL.
	 *
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 *
	 * <pre>
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param httpUrl the source URI
	 * @return the URI components of the URI
	 */
	public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
		Assert.notNull(httpUrl, "'httpUrl' must not be null");
		Matcher m = HTTP_URL_PATTERN.matcher(httpUrl);
		if (m.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();

			builder.scheme(m.group(1));
			builder.userInfo(m.group(4));
			builder.host(m.group(5));
			String port = m.group(7);
			if (StringUtils.hasLength(port)) {
				builder.port(Integer.parseInt(port));
			}
			builder.path(m.group(8));
			builder.query(m.group(10));

			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}



	// build methods

	/**
	 * Builds a {@code UriComponents} instance from the various components contained in this builder.
	 *
	 * @return the URI components
	 */
	public UriComponents build() {
		return build(false);
	}

	/**
	 * Builds a {@code UriComponents} instance from the various components
	 * contained in this builder.
	 *
	 * @param encoded whether all the components set in this builder are
	 * 	encoded ({@code true}) or not ({@code false}).
	 * @return the URI components
	 */
	public UriComponents build(boolean encoded) {
		if (ssp != null) {
			return new OpaqueUriComponents(scheme, ssp, fragment);
		}
		else {
			return new HierarchicalUriComponents(
					scheme, userInfo, host, port, pathBuilder.build(), queryParams, fragment, encoded, true);
		}
	}

	/**
	 * Builds a {@code UriComponents} instance and replaces URI template variables
	 * with the values from a map. This is a shortcut method, which combines
	 * calls to {@link #build()} and then {@link UriComponents#expand(Map)}.
	 *
	 * @param uriVariables the map of URI variables
	 * @return the URI components with expanded values
	 */
	public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
		return build(false).expand(uriVariables);
	}

	/**
	 * Builds a {@code UriComponents} instance and replaces URI template variables
	 * with the values from an array. This is a shortcut method, which combines
	 * calls to {@link #build()} and then {@link UriComponents#expand(Object...)}.
	 *
	 * @param uriVariableValues URI variable values
	 * @return the URI components with expanded values
	 */
	public UriComponents buildAndExpand(Object... uriVariableValues) {
		return build(false).expand(uriVariableValues);
	}

	// URI components methods

	/**
	 * Initializes all components of this URI builder with the components of the given URI.
	 *
	 * @param uri the URI
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uri(URI uri) {
		Assert.notNull(uri, "'uri' must not be null");

		this.scheme = uri.getScheme();

		if (uri.isOpaque()) {
			this.ssp = uri.getRawSchemeSpecificPart();
			resetHierarchicalComponents();
		}
		else {
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
				this.pathBuilder = new CompositePathComponentBuilder(uri.getRawPath());
			}
			if (StringUtils.hasLength(uri.getRawQuery())) {
				this.queryParams.clear();
				query(uri.getRawQuery());
			}
			resetSchemeSpecificPart();
		}
		if (uri.getRawFragment() != null) {
			this.fragment = uri.getRawFragment();
		}
		return this;
	}

	private void resetHierarchicalComponents() {
		this.userInfo = null;
		this.host = null;
		this.port = -1;
		this.pathBuilder = new CompositePathComponentBuilder();
		this.queryParams.clear();
	}

	private void resetSchemeSpecificPart() {
		this.ssp = null;
	}

	/**
	 * Sets the URI scheme. The given scheme may contain URI template variables,
	 * and may also be {@code null} to clear the scheme of this builder.
	 *
	 * @param scheme
	 *            the URI scheme
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder scheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * Set the URI scheme-specific-part. When invoked, this method overwrites
	 * {@linkplain #userInfo(String) user-info}, {@linkplain #host(String) host},
	 * {@linkplain #port(int) port}, {@linkplain #path(String) path}, and
	 * {@link #query(String) query}.
	 *
	 * @param ssp the URI scheme-specific-part, may contain URI template parameters
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		resetHierarchicalComponents();
		return this;
	}

	/**
	 * Sets the URI user info. The given user info may contain URI template
	 * variables, and may also be {@code null} to clear the user info of this
	 * builder.
	 *
	 * @param userInfo the URI user info
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder userInfo(String userInfo) {
		this.userInfo = userInfo;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Sets the URI host. The given host may contain URI template variables, and
	 * may also be {@code null} to clear the host of this builder.
	 *
	 * @param host the URI host
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder host(String host) {
		this.host = host;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Sets the URI port. Passing {@code -1} will clear the port of this builder.
	 *
	 * @param port the URI port
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder port(int port) {
		Assert.isTrue(port >= -1, "'port' must not be < -1");
		this.port = port;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Appends the given path to the existing path of this builder. The given
	 * path may contain URI template variables.
	 *
	 * @param path the URI path
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder path(String path) {
		this.pathBuilder.addPath(path);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Sets the path of this builder overriding all existing path and path segment values.
	 *
	 * @param path the URI path; a {@code null} value results in an empty path.
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replacePath(String path) {
		this.pathBuilder = new CompositePathComponentBuilder(path);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Appends the given path segments to the existing path of this builder. Each given
	 * path segments may contain URI template variables.
	 *
	 * @param pathSegments the URI path segments
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
		Assert.notNull(pathSegments, "'segments' must not be null");
		this.pathBuilder.addPathSegments(pathSegments);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Appends the given query to the existing query of this builder.
	 * The given query may contain URI template variables.
	 *
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 *
	 * <pre>
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param query the query string
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder query(String query) {
		if (query != null) {
			Matcher m = QUERY_PARAM_PATTERN.matcher(query);
			while (m.find()) {
				String name = m.group(1);
				String eq = m.group(2);
				String value = m.group(3);
				queryParam(name, (value != null ? value :
					(StringUtils.hasLength(eq) ? "" : null)));
			}
		}
		else {
			this.queryParams.clear();
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Sets the query of this builder overriding all existing query parameters.
	 *
	 * @param query the query string; a {@code null} value removes all query parameters.
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replaceQuery(String query) {
		this.queryParams.clear();
		query(query);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Appends the given query parameter to the existing query parameters. The
	 * given name or any of the values may contain URI template variables. If no
	 * values are given, the resulting URI will contain the query parameter name
	 * only (i.e. {@code ?foo} instead of {@code ?foo=bar}.
	 *
	 * @param name
	 *            the query parameter name
	 * @param values
	 *            the query parameter values
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder queryParam(String name, Object... values) {
		Assert.notNull(name, "'name' must not be null");
		if (!ObjectUtils.isEmpty(values)) {
			for (Object value : values) {
				String valueAsString = value != null ? value.toString() : null;
				this.queryParams.add(name, valueAsString);
			}
		}
		else {
			this.queryParams.add(name, null);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Sets the query parameter values overriding all existing query values for
	 * the same parameter. If no values are given, the query parameter is
	 * removed.
	 *
	 * @param name
	 *            the query parameter name
	 * @param values
	 *            the query parameter values
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
		Assert.notNull(name, "'name' must not be null");
		this.queryParams.remove(name);
		if (!ObjectUtils.isEmpty(values)) {
			queryParam(name, values);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Sets the URI fragment. The given fragment may contain URI template
	 * variables, and may also be {@code null} to clear the fragment of this
	 * builder.
	 *
	 * @param fragment
	 *            the URI fragment
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder fragment(String fragment) {
		if (fragment != null) {
			Assert.hasLength(fragment, "'fragment' must not be empty");
			this.fragment = fragment;
		}
		else {
			this.fragment = null;
		}
		return this;
	}


	private interface PathComponentBuilder {
		PathComponent build();
	}

	private static class CompositePathComponentBuilder implements PathComponentBuilder {

		private LinkedList<PathComponentBuilder> componentBuilders = new LinkedList<PathComponentBuilder>();

		public CompositePathComponentBuilder() {
		}

		public CompositePathComponentBuilder(String path) {
			addPath(path);
		}

		public void addPathSegments(String... pathSegments) {
			if (!ObjectUtils.isEmpty(pathSegments)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				if (psBuilder == null) {
					psBuilder = new PathSegmentComponentBuilder();
					this.componentBuilders.add(psBuilder);
					if (fpBuilder != null) {
						fpBuilder.removeTrailingSlash();
					}
				}
				psBuilder.append(pathSegments);
			}
		}

		public void addPath(String path) {
			if (StringUtils.hasText(path)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				if (psBuilder != null) {
					path = path.startsWith("/") ? path : "/" + path;
				}
				if (fpBuilder == null) {
					fpBuilder = new FullPathComponentBuilder();
					this.componentBuilders.add(fpBuilder);
				}
				fpBuilder.append(path);
			}
		}

		@SuppressWarnings("unchecked")
		private <T> T getLastBuilder(Class<T> builderClass) {
			if (!this.componentBuilders.isEmpty()) {
				PathComponentBuilder last = this.componentBuilders.getLast();
				if (builderClass.isInstance(last)) {
					return (T) last;
				}
			}
			return null;
		}

		public PathComponent build() {
			int size = this.componentBuilders.size();
			List<PathComponent> components = new ArrayList<PathComponent>(size);
			for (int i = 0; i < size; i++) {
				PathComponent pathComponent = this.componentBuilders.get(i).build();
				if (pathComponent != null) {
					components.add(pathComponent);
				}
			}
			if (components.isEmpty()) {
				return HierarchicalUriComponents.NULL_PATH_COMPONENT;
			}
			if (components.size() == 1) {
				return components.get(0);
			}
			return new HierarchicalUriComponents.PathComponentComposite(components);
		}
	}

	private static class FullPathComponentBuilder implements PathComponentBuilder {

		private StringBuilder path = new StringBuilder();

		public void append(String path) {
			this.path.append(path);
		}

		public PathComponent build() {
			if (this.path.length() == 0) {
				return null;
			}
			String path = this.path.toString().replace("//", "/");
			return new HierarchicalUriComponents.FullPathComponent(path);
		}

		public void removeTrailingSlash() {
			int index = this.path.length() - 1;
			if (this.path.charAt(index) == '/') {
				this.path.deleteCharAt(index);
			}
		}
	}

	private static class PathSegmentComponentBuilder implements PathComponentBuilder {

		private List<String> pathSegments = new LinkedList<String>();

		public void append(String... pathSegments) {
			for (String pathSegment : pathSegments) {
				if (StringUtils.hasText(pathSegment)) {
					this.pathSegments.add(pathSegment);
				}
			}
		}

		public PathComponent build() {
			return this.pathSegments.isEmpty() ?
					null : new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments);
		}
	}

}
