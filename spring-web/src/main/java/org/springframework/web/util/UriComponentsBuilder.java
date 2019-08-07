/*
 * Copyright 2002-2019 the original author or authors.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HierarchicalUriComponents.PathComponent;
import org.springframework.web.util.UriComponents.UriTemplateVariables;

/**
 * Builder for {@link UriComponents}.
 *
 * <p>Typical usage involves:
 * <ol>
 * <li>Create a {@code UriComponentsBuilder} with one of the static factory methods
 * (such as {@link #fromPath(String)} or {@link #fromUri(URI)})</li>
 * <li>Set the various URI components through the respective methods ({@link #scheme(String)},
 * {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 * {@link #pathSegment(String...)}, {@link #queryParam(String, Object...)}, and
 * {@link #fragment(String)}.</li>
 * <li>Build the {@link UriComponents} instance with the {@link #build()} method.</li>
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 3.1
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 */
public class UriComponentsBuilder implements UriBuilder, Cloneable {

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	private static final String HTTP_PATTERN = "(?i)(http|https):";

	private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

	private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

	private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}\\:\\.]*[%\\p{Alnum}]*\\]";

	private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

	private static final String PORT_PATTERN = "(\\d*(?:\\{[^/]+?\\})?)";

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

	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?");

	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?");


	@Nullable
	private String scheme;

	@Nullable
	private String ssp;

	@Nullable
	private String userInfo;

	@Nullable
	private String host;

	@Nullable
	private String port;

	private CompositePathComponentBuilder pathBuilder;

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	@Nullable
	private String fragment;

	private final Map<String, Object> uriVariables = new HashMap<>(4);

	private boolean encodeTemplate;

	private Charset charset = StandardCharsets.UTF_8;


	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 * @see #newInstance()
	 * @see #fromPath(String)
	 * @see #fromUri(URI)
	 */
	protected UriComponentsBuilder() {
		this.pathBuilder = new CompositePathComponentBuilder();
	}

	/**
	 * Create a deep copy of the given UriComponentsBuilder.
	 * @param other the other builder to copy from
	 * @since 4.1.3
	 */
	protected UriComponentsBuilder(UriComponentsBuilder other) {
		this.scheme = other.scheme;
		this.ssp = other.ssp;
		this.userInfo = other.userInfo;
		this.host = other.host;
		this.port = other.port;
		this.pathBuilder = other.pathBuilder.cloneBuilder();
		this.queryParams.putAll(other.queryParams);
		this.fragment = other.fragment;
		this.encodeTemplate = other.encodeTemplate;
		this.charset = other.charset;
	}


	// Factory methods

	/**
	 * Create a new, empty builder.
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder newInstance() {
		return new UriComponentsBuilder();
	}

	/**
	 * Create a builder that is initialized with the given path.
	 * @param path the path to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromPath(String path) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.path(path);
		return builder;
	}

	/**
	 * Create a builder that is initialized with the given {@code URI}.
	 * @param uri the URI to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * Create a builder that is initialized with the given URI string.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * @param uri the URI string to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUriString(String uri) {
		Assert.notNull(uri, "URI must not be null");
		Matcher matcher = URI_PATTERN.matcher(uri);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(2);
			String userInfo = matcher.group(5);
			String host = matcher.group(6);
			String port = matcher.group(8);
			String path = matcher.group(9);
			String query = matcher.group(11);
			String fragment = matcher.group(13);
			boolean opaque = false;
			if (StringUtils.hasLength(scheme)) {
				String rest = uri.substring(scheme.length());
				if (!rest.startsWith(":/")) {
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
					builder.port(port);
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
	 * Create a URI components builder from the given HTTP URL String.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * String urlString = &quot;https://example.com/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromHttpUrl(urlString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * @param httpUrl the source URI
	 * @return the URI components of the URI
	 */
	public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
		Assert.notNull(httpUrl, "HTTP URL must not be null");
		Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(1);
			builder.scheme(scheme != null ? scheme.toLowerCase() : null);
			builder.userInfo(matcher.group(4));
			String host = matcher.group(5);
			if (StringUtils.hasLength(scheme) && !StringUtils.hasLength(host)) {
				throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
			}
			builder.host(host);
			String port = matcher.group(7);
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			builder.path(matcher.group(8));
			builder.query(matcher.group(10));
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}

	/**
	 * Create a new {@code UriComponents} object from the URI associated with
	 * the given HttpRequest while also overlaying with values from the headers
	 * "Forwarded" (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 * @param request the source request
	 * @return the URI components of the URI
	 * @since 4.1.5
	 */
	public static UriComponentsBuilder fromHttpRequest(HttpRequest request) {
		return fromUri(request.getURI()).adaptFromForwardedHeaders(request.getHeaders());
	}

	/**
	 * Create an instance by parsing the "Origin" header of an HTTP request.
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
	 */
	public static UriComponentsBuilder fromOriginHeader(String origin) {
		Matcher matcher = URI_PATTERN.matcher(origin);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(2);
			String host = matcher.group(6);
			String port = matcher.group(8);
			if (StringUtils.hasLength(scheme)) {
				builder.scheme(scheme);
			}
			builder.host(host);
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + origin + "] is not a valid \"Origin\" header value");
		}
	}


	// Encode methods

	/**
	 * Request to have the URI template pre-encoded at build time, and
	 * URI variables encoded separately when expanded.
	 * <p>In comparison to {@link UriComponents#encode()}, this method has the
	 * same effect on the URI template, i.e. each URI component is encoded by
	 * replacing non-ASCII and illegal (within the URI component type) characters
	 * with escaped octets. However URI variables are encoded more strictly, by
	 * also escaping characters with reserved meaning.
	 * <p>For most cases, this method is more likely to give the expected result
	 * because in treats URI variables as opaque data to be fully encoded, while
	 * {@link UriComponents#encode()} is useful only if intentionally expanding
	 * URI variables that contain reserved characters.
	 * <p>For example ';' is legal in a path but has reserved meaning. This
	 * method replaces ";" with "%3B" in URI variables but not in the URI
	 * template. By contrast, {@link UriComponents#encode()} never replaces ";"
	 * since it is a legal character in a path.
	 * @since 5.0.8
	 */
	public final UriComponentsBuilder encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * A variant of {@link #encode()} with a charset other than "UTF-8".
	 * @param charset the charset to use for encoding
	 * @since 5.0.8
	 */
	public UriComponentsBuilder encode(Charset charset) {
		this.encodeTemplate = true;
		this.charset = charset;
		return this;
	}


	// Build methods

	/**
	 * Build a {@code UriComponents} instance from the various components contained in this builder.
	 * @return the URI components
	 */
	public UriComponents build() {
		return build(false);
	}

	/**
	 * Build a {@code UriComponents} instance from the various components
	 * contained in this builder.
	 * @param encoded whether all the components set in this builder are
	 * encoded ({@code true}) or not ({@code false})
	 * @return the URI components
	 */
	public UriComponents build(boolean encoded) {
		return buildInternal(encoded ?
				EncodingHint.FULLY_ENCODED :
				this.encodeTemplate ? EncodingHint.ENCODE_TEMPLATE : EncodingHint.NONE);
	}

	private UriComponents buildInternal(EncodingHint hint) {
		UriComponents result;
		if (this.ssp != null) {
			result = new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
		}
		else {
			HierarchicalUriComponents uric = new HierarchicalUriComponents(this.scheme, this.fragment,
					this.userInfo, this.host, this.port, this.pathBuilder.build(), this.queryParams,
					hint == EncodingHint.FULLY_ENCODED);

			result = hint == EncodingHint.ENCODE_TEMPLATE ? uric.encodeTemplate(this.charset) : uric;
		}
		if (!this.uriVariables.isEmpty()) {
			result = result.expand(name -> this.uriVariables.getOrDefault(name, UriTemplateVariables.SKIP_VALUE));
		}
		return result;
	}

	/**
	 * Build a {@code UriComponents} instance and replaces URI template variables
	 * with the values from a map. This is a shortcut method which combines
	 * calls to {@link #build()} and then {@link UriComponents#expand(Map)}.
	 * @param uriVariables the map of URI variables
	 * @return the URI components with expanded values
	 */
	public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
		return build().expand(uriVariables);
	}

	/**
	 * Build a {@code UriComponents} instance and replaces URI template variables
	 * with the values from an array. This is a shortcut method which combines
	 * calls to {@link #build()} and then {@link UriComponents#expand(Object...)}.
	 * @param uriVariableValues the URI variable values
	 * @return the URI components with expanded values
	 */
	public UriComponents buildAndExpand(Object... uriVariableValues) {
		return build().expand(uriVariableValues);
	}

	@Override
	public URI build(Object... uriVariables) {
		return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
	}

	@Override
	public URI build(Map<String, ?> uriVariables) {
		return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
	}

	/**
	 * Build a URI String.
	 * <p>Effectively, a shortcut for building, encoding, and returning the
	 * String representation:
	 * <pre class="code">
	 * String uri = builder.build().encode().toUriString()
	 * </pre>
	 * <p>However if {@link #uriVariables(Map) URI variables} have been provided
	 * then the URI template is pre-encoded separately from URI variables (see
	 * {@link #encode()} for details), i.e. equivalent to:
	 * <pre>
	 * String uri = builder.encode().build().toUriString()
	 * </pre>
	 * @since 4.1
	 * @see UriComponents#toUriString()
	 */
	public String toUriString() {
		return this.uriVariables.isEmpty() ?
				build().encode().toUriString() :
				buildInternal(EncodingHint.ENCODE_TEMPLATE).toUriString();
	}


	// Instance methods

	/**
	 * Initialize components of this builder from components of the given URI.
	 * @param uri the URI
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
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
				this.port = String.valueOf(uri.getPort());
			}
			if (StringUtils.hasLength(uri.getRawPath())) {
				this.pathBuilder = new CompositePathComponentBuilder();
				this.pathBuilder.addPath(uri.getRawPath());
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

	/**
	 * Set or append individual URI components of this builder from the values
	 * of the given {@link UriComponents} instance.
	 * <p>For the semantics of each component (i.e. set vs append) check the
	 * builder methods on this class. For example {@link #host(String)} sets
	 * while {@link #path(String)} appends.
	 * @param uriComponents the UriComponents to copy from
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
		Assert.notNull(uriComponents, "UriComponents must not be null");
		uriComponents.copyToUriComponentsBuilder(this);
		return this;
	}

	/**
	 * Set the URI scheme. The given scheme may contain URI template variables,
	 * and may also be {@code null} to clear the scheme of this builder.
	 * @param scheme the URI scheme
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder scheme(@Nullable String scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * Set the URI scheme-specific-part. When invoked, this method overwrites
	 * {@linkplain #userInfo(String) user-info}, {@linkplain #host(String) host},
	 * {@linkplain #port(int) port}, {@linkplain #path(String) path}, and
	 * {@link #query(String) query}.
	 * @param ssp the URI scheme-specific-part, may contain URI template parameters
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		resetHierarchicalComponents();
		return this;
	}

	/**
	 * Set the URI user info. The given user info may contain URI template variables,
	 * and may also be {@code null} to clear the user info of this builder.
	 * @param userInfo the URI user info
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder userInfo(@Nullable String userInfo) {
		this.userInfo = userInfo;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI host. The given host may contain URI template variables,
	 * and may also be {@code null} to clear the host of this builder.
	 * @param host the URI host
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder host(@Nullable String host) {
		this.host = host;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI port. Passing {@code -1} will clear the port of this builder.
	 * @param port the URI port
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder port(int port) {
		Assert.isTrue(port >= -1, "Port must be >= -1");
		this.port = String.valueOf(port);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI port. Use this method only when the port needs to be
	 * parameterized with a URI variable. Otherwise use {@link #port(int)}.
	 * Passing {@code null} will clear the port of this builder.
	 * @param port the URI port
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder port(@Nullable String port) {
		this.port = port;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given path to the existing path of this builder.
	 * The given path may contain URI template variables.
	 * @param path the URI path
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder path(String path) {
		this.pathBuilder.addPath(path);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append path segments to the existing path. Each path segment may contain
	 * URI template variables and should not contain any slashes.
	 * Use {@code path("/")} subsequently to ensure a trailing slash.
	 * @param pathSegments the URI path segments
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
		this.pathBuilder.addPathSegments(pathSegments);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the path of this builder overriding all existing path and path segment values.
	 * @param path the URI path (a {@code null} value results in an empty path)
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder replacePath(@Nullable String path) {
		this.pathBuilder = new CompositePathComponentBuilder();
		if (path != null) {
			this.pathBuilder.addPath(path);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given query to the existing query of this builder.
	 * The given query may contain URI template variables.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * UriComponentsBuilder.fromUriString(&quot;/hotels/42&quot;)
	 * 	.query(&quot;filter={value}&quot;)
	 * 	.buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * @param query the query string
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder query(@Nullable String query) {
		if (query != null) {
			Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = matcher.group(1);
				String eq = matcher.group(2);
				String value = matcher.group(3);
				queryParam(name, (value != null ? value : (StringUtils.hasLength(eq) ? "" : null)));
			}
		}
		else {
			this.queryParams.clear();
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the query of this builder overriding all existing query parameters.
	 * @param query the query string; a {@code null} value removes all query parameters.
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder replaceQuery(@Nullable String query) {
		this.queryParams.clear();
		if (query != null) {
			query(query);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given query parameter to the existing query parameters. The
	 * given name or any of the values may contain URI template variables. If no
	 * values are given, the resulting URI will contain the query parameter name
	 * only (i.e. {@code ?foo} instead of {@code ?foo=bar}).
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder queryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		if (!ObjectUtils.isEmpty(values)) {
			for (Object value : values) {
				String valueAsString = (value != null ? value.toString() : null);
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
	 * Add the given query parameters.
	 * @param params the params
	 * @return this UriComponentsBuilder
	 * @since 4.0
	 */
	@Override
	public UriComponentsBuilder queryParams(@Nullable MultiValueMap<String, String> params) {
		if (params != null) {
			this.queryParams.addAll(params);
		}
		return this;
	}

	/**
	 * Set the query parameter values overriding all existing query values for
	 * the same parameter. If no values are given, the query parameter is removed.
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		this.queryParams.remove(name);
		if (!ObjectUtils.isEmpty(values)) {
			queryParam(name, values);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the query parameter values overriding all existing query values.
	 * @param params the query parameter name
	 * @return this UriComponentsBuilder
	 * @since 4.2
	 */
	@Override
	public UriComponentsBuilder replaceQueryParams(@Nullable MultiValueMap<String, String> params) {
		this.queryParams.clear();
		if (params != null) {
			this.queryParams.putAll(params);
		}
		return this;
	}

	/**
	 * Set the URI fragment. The given fragment may contain URI template variables,
	 * and may also be {@code null} to clear the fragment of this builder.
	 * @param fragment the URI fragment
	 * @return this UriComponentsBuilder
	 */
	@Override
	public UriComponentsBuilder fragment(@Nullable String fragment) {
		if (fragment != null) {
			Assert.hasLength(fragment, "Fragment must not be empty");
			this.fragment = fragment;
		}
		else {
			this.fragment = null;
		}
		return this;
	}

	/**
	 * Configure URI variables to be expanded at build time.
	 * <p>The provided variables may be a subset of all required ones. At build
	 * time, the available ones are expanded, while unresolved URI placeholders
	 * are left in place and can still be expanded later.
	 * <p>In contrast to {@link UriComponents#expand(Map)} or
	 * {@link #buildAndExpand(Map)}, this method is useful when you need to
	 * supply URI variables without building the {@link UriComponents} instance
	 * just yet, or perhaps pre-expand some shared default values such as host
	 * and port.
	 * @param uriVariables the URI variables to use
	 * @return this UriComponentsBuilder
	 * @since 5.0.8
	 */
	public UriComponentsBuilder uriVariables(Map<String, Object> uriVariables) {
		this.uriVariables.putAll(uriVariables);
		return this;
	}

	/**
	 * Adapt this builder's scheme+host+port from the given headers, specifically
	 * "Forwarded" (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>,
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 * <p><strong>Note:</strong> this method uses values from forwarded headers,
	 * if present, in order to reflect the client-originated protocol and address.
	 * Consider using the {@code ForwardedHeaderFilter} in order to choose from a
	 * central place whether to extract and use, or to discard such headers.
	 * See the Spring Framework reference for more on this filter.
	 * @param headers the HTTP headers to consider
	 * @return this UriComponentsBuilder
	 * @since 4.2.7
	 */
	UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
		try {
			String forwardedHeader = headers.getFirst("Forwarded");
			if (StringUtils.hasText(forwardedHeader)) {
				String forwardedToUse = StringUtils.tokenizeToStringArray(forwardedHeader, ",")[0];
				Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedToUse);
				if (matcher.find()) {
					scheme(matcher.group(1).trim());
					port(null);
				}
				matcher = FORWARDED_HOST_PATTERN.matcher(forwardedToUse);
				if (matcher.find()) {
					adaptForwardedHost(matcher.group(1).trim());
				}
			}
			else {
				String protocolHeader = headers.getFirst("X-Forwarded-Proto");
				if (StringUtils.hasText(protocolHeader)) {
					scheme(StringUtils.tokenizeToStringArray(protocolHeader, ",")[0]);
					port(null);
				}

				String hostHeader = headers.getFirst("X-Forwarded-Host");
				if (StringUtils.hasText(hostHeader)) {
					adaptForwardedHost(StringUtils.tokenizeToStringArray(hostHeader, ",")[0]);
				}

				String portHeader = headers.getFirst("X-Forwarded-Port");
				if (StringUtils.hasText(portHeader)) {
					port(Integer.parseInt(StringUtils.tokenizeToStringArray(portHeader, ",")[0]));
				}
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Failed to parse a port from \"forwarded\"-type headers. " +
					"If not behind a trusted proxy, consider using ForwardedHeaderFilter " +
					"with the removeOnly=true. Request headers: " + headers);
		}

		if (this.scheme != null && ((this.scheme.equals("http") && "80".equals(this.port)) ||
				(this.scheme.equals("https") && "443".equals(this.port)))) {
			port(null);
		}

		return this;
	}

	private void adaptForwardedHost(String hostToUse) {
		int portSeparatorIdx = hostToUse.lastIndexOf(':');
		if (portSeparatorIdx > hostToUse.lastIndexOf(']')) {
			host(hostToUse.substring(0, portSeparatorIdx));
			port(Integer.parseInt(hostToUse.substring(portSeparatorIdx + 1)));
		}
		else {
			host(hostToUse);
			port(null);
		}
	}

	private void resetHierarchicalComponents() {
		this.userInfo = null;
		this.host = null;
		this.port = null;
		this.pathBuilder = new CompositePathComponentBuilder();
		this.queryParams.clear();
	}

	private void resetSchemeSpecificPart() {
		this.ssp = null;
	}


	/**
	 * Public declaration of Object's {@code clone()} method.
	 * Delegates to {@link #cloneBuilder()}.
	 */
	@Override
	public Object clone() {
		return cloneBuilder();
	}

	/**
	 * Clone this {@code UriComponentsBuilder}.
	 * @return the cloned {@code UriComponentsBuilder} object
	 * @since 4.2.7
	 */
	public UriComponentsBuilder cloneBuilder() {
		return new UriComponentsBuilder(this);
	}


	private interface PathComponentBuilder {

		@Nullable
		PathComponent build();

		PathComponentBuilder cloneBuilder();
	}


	private static class CompositePathComponentBuilder implements PathComponentBuilder {

		private final LinkedList<PathComponentBuilder> builders = new LinkedList<>();

		public void addPathSegments(String... pathSegments) {
			if (!ObjectUtils.isEmpty(pathSegments)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				if (psBuilder == null) {
					psBuilder = new PathSegmentComponentBuilder();
					this.builders.add(psBuilder);
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
					path = (path.startsWith("/") ? path : "/" + path);
				}
				if (fpBuilder == null) {
					fpBuilder = new FullPathComponentBuilder();
					this.builders.add(fpBuilder);
				}
				fpBuilder.append(path);
			}
		}

		@SuppressWarnings("unchecked")
		@Nullable
		private <T> T getLastBuilder(Class<T> builderClass) {
			if (!this.builders.isEmpty()) {
				PathComponentBuilder last = this.builders.getLast();
				if (builderClass.isInstance(last)) {
					return (T) last;
				}
			}
			return null;
		}

		@Override
		public PathComponent build() {
			int size = this.builders.size();
			List<PathComponent> components = new ArrayList<>(size);
			for (PathComponentBuilder componentBuilder : this.builders) {
				PathComponent pathComponent = componentBuilder.build();
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

		@Override
		public CompositePathComponentBuilder cloneBuilder() {
			CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
			for (PathComponentBuilder builder : this.builders) {
				compositeBuilder.builders.add(builder.cloneBuilder());
			}
			return compositeBuilder;
		}
	}


	private static class FullPathComponentBuilder implements PathComponentBuilder {

		private final StringBuilder path = new StringBuilder();

		public void append(String path) {
			this.path.append(path);
		}

		@Override
		public PathComponent build() {
			if (this.path.length() == 0) {
				return null;
			}
			String path = this.path.toString();
			while (true) {
				int index = path.indexOf("//");
				if (index == -1) {
					break;
				}
				path = path.substring(0, index) + path.substring(index + 1);
			}
			return new HierarchicalUriComponents.FullPathComponent(path);
		}

		public void removeTrailingSlash() {
			int index = this.path.length() - 1;
			if (this.path.charAt(index) == '/') {
				this.path.deleteCharAt(index);
			}
		}

		@Override
		public FullPathComponentBuilder cloneBuilder() {
			FullPathComponentBuilder builder = new FullPathComponentBuilder();
			builder.append(this.path.toString());
			return builder;
		}
	}


	private static class PathSegmentComponentBuilder implements PathComponentBuilder {

		private final List<String> pathSegments = new LinkedList<>();

		public void append(String... pathSegments) {
			for (String pathSegment : pathSegments) {
				if (StringUtils.hasText(pathSegment)) {
					this.pathSegments.add(pathSegment);
				}
			}
		}

		@Override
		public PathComponent build() {
			return (this.pathSegments.isEmpty() ? null :
					new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
		}

		@Override
		public PathSegmentComponentBuilder cloneBuilder() {
			PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
			builder.pathSegments.addAll(this.pathSegments);
			return builder;
		}
	}


	private enum EncodingHint { ENCODE_TEMPLATE, FULLY_ENCODED, NONE }

}
