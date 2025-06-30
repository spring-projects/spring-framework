/*
 * Copyright 2002-present the original author or authors.
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HierarchicalUriComponents.PathComponent;
import org.springframework.web.util.UriComponents.UriTemplateVariables;

/**
 * Builder for {@link UriComponents}. Use as follows:
 * <ol>
 * <li>Create a builder through a factory method, e.g. {@link #fromUriString(String)}.
 * <li>Set URI components (e.g. scheme, host, path, etc) through instance methods.
 * <li>Build the {@link UriComponents}.</li>
 * <li>Expand URI variables from a map or array or variable values.
 * <li>Encode via {@link UriComponents#encode()}.</li>
 * <li>Use {@link UriComponents#toUri()} or {@link UriComponents#toUriString()}.
 * </ol>
 *
 * <p>By default, URI parsing is based on the {@link ParserType#RFC RFC parser type},
 * which expects input strings to conform to RFC 3986 syntax. The alternative
 * {@link ParserType#WHAT_WG WhatWG parser type}, based on the algorithm from
 * the WhatWG <a href="https://url.spec.whatwg.org">URL Living Standard</a>
 * provides more lenient handling of a wide range of cases that occur in user
 * types URL's.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 3.1
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 */
public class UriComponentsBuilder implements UriBuilder, Cloneable {

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	private static final Object[] EMPTY_VALUES = new Object[0];

	private @Nullable String scheme;

	private @Nullable String ssp;

	private @Nullable String userInfo;

	private @Nullable String host;

	private @Nullable String port;

	private CompositePathComponentBuilder pathBuilder;

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	private @Nullable String fragment;

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
		this.uriVariables.putAll(other.uriVariables);
		this.queryParams.addAll(other.queryParams);
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
	 * Create a builder that is initialized from the given {@code URI}.
	 * <p><strong>Note:</strong> the components in the resulting builder will be
	 * in fully encoded (raw) form and further changes must also supply values
	 * that are fully encoded, for example via methods in {@link UriUtils}.
	 * In addition please use {@link #build(boolean)} with a value of "true" to
	 * build the {@link UriComponents} instance in order to indicate that the
	 * components are encoded.
	 * @param uri the URI to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * Variant of {@link #fromUriString(String, ParserType)} that defaults to
	 * the {@link ParserType#RFC} parsing.
	 */
	public static UriComponentsBuilder fromUriString(String uri) throws InvalidUrlException {
		Assert.notNull(uri, "URI must not be null");
		if (uri.isEmpty()) {
			return new UriComponentsBuilder();
		}
		return fromUriString(uri, ParserType.RFC);
	}

	/**
	 * Create a builder that is initialized by parsing the given URI string.
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
	 * @param parserType the parsing algorithm to use
	 * @return the new {@code UriComponentsBuilder}
	 * @throws InvalidUrlException if {@code uri} cannot be parsed
	 * @since 6.2
	 */
	public static UriComponentsBuilder fromUriString(String uri, ParserType parserType) throws InvalidUrlException {
		Assert.notNull(uri, "URI must not be null");
		if (uri.isEmpty()) {
			return new UriComponentsBuilder();
		}
		UriComponentsBuilder builder = new UriComponentsBuilder();
		return switch (parserType) {
			case RFC -> {
				RfcUriParser.UriRecord record = RfcUriParser.parse(uri);
				yield builder.rfcUriRecord(record);
			}
			case WHAT_WG -> {
				WhatWgUrlParser.UrlRecord record =
						WhatWgUrlParser.parse(uri, WhatWgUrlParser.EMPTY_RECORD, null, null);
				yield builder.whatWgUrlRecord(record);
			}
		};
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
	 * {@link UriComponents#encode()} is useful when intentionally expanding URI
	 * variables that contain reserved characters.
	 * <p>For example ';' is legal in a path but has reserved meaning. This
	 * method replaces ";" with "%3B" in URI variables but not in the URI
	 * template. By contrast, {@link UriComponents#encode()} never replaces ";"
	 * since it is a legal character in a path.
	 * <p>When not expanding URI variables at all, prefer use of
	 * {@link UriComponents#encode()} since that will also encode anything that
	 * incidentally looks like a URI variable.
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
	 * Variant of {@link #build()} to create a {@link UriComponents} instance
	 * when components are already fully encoded. This is useful for example if
	 * the builder was created via {@link UriComponentsBuilder#fromUri(URI)}.
	 * @param encoded whether the components in this builder are already encoded
	 * @return the URI components
	 * @throws IllegalArgumentException if any of the components contain illegal
	 * characters that should have been encoded.
	 */
	public UriComponents build(boolean encoded) {
		return buildInternal(encoded ? EncodingHint.FULLY_ENCODED :
				(this.encodeTemplate ? EncodingHint.ENCODE_TEMPLATE : EncodingHint.NONE));
	}

	private UriComponents buildInternal(EncodingHint hint) {
		UriComponents result;
		if (this.ssp != null) {
			result = new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
		}
		else {
			MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(this.queryParams);
			HierarchicalUriComponents uric = new HierarchicalUriComponents(this.scheme, this.fragment,
					this.userInfo, this.host, this.port, this.pathBuilder.build(), queryParams,
					hint == EncodingHint.FULLY_ENCODED);
			result = (hint == EncodingHint.ENCODE_TEMPLATE ? uric.encodeTemplate(this.charset) : uric);
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
	public UriComponents buildAndExpand(@Nullable Object... uriVariableValues) {
		return build().expand(uriVariableValues);
	}

	@Override
	public URI build(@Nullable Object... uriVariables) {
		return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
	}

	@Override
	public URI build(Map<String, ? extends @Nullable Object> uriVariables) {
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
	@Override
	public String toUriString() {
		return (this.uriVariables.isEmpty() ?
				build().encode().toUriString() :
				buildInternal(EncodingHint.ENCODE_TEMPLATE).toUriString());
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
	 * Internal method to initialize this builder from an RFC {@code UriRecord}.
	 */
	private UriComponentsBuilder rfcUriRecord(RfcUriParser.UriRecord record) {
		scheme(record.scheme());
		if (record.isOpaque()) {
			if (record.path() != null) {
				schemeSpecificPart(record.path());
			}
		}
		else {
			userInfo(record.user());
			host(record.host());
			port(record.port());
			if (record.path() != null) {
				path(record.path());
			}
			query(record.query());
		}
		fragment(record.fragment());
		return this;
	}

	/**
	 * Internal method to initialize this builder from a WhatWG {@code UrlRecord}.
	 */
	private UriComponentsBuilder whatWgUrlRecord(WhatWgUrlParser.UrlRecord record) {
		if (!record.scheme().isEmpty()) {
			scheme(record.scheme());
		}
		if (record.path().isOpaque()) {
			String ssp = record.path() + record.search();
			schemeSpecificPart(ssp);
		}
		else {
			userInfo(record.userInfo());
			String hostname = record.hostname();
			if (StringUtils.hasText(hostname)) {
				host(hostname);
			}
			if (record.port() != null) {
				port(record.portString());
			}
			path(record.path().toString());
			query(record.query());
		}
		if (StringUtils.hasText(record.fragment())) {
			fragment(record.fragment());
		}
		return this;
	}

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

	@Override
	public UriComponentsBuilder userInfo(@Nullable String userInfo) {
		this.userInfo = userInfo;
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder host(@Nullable String host) {
		this.host = host;
		if (host != null) {
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder port(int port) {
		Assert.isTrue(port >= -1, "Port must be >= -1");
		this.port = String.valueOf(port);
		if (port > -1) {
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder port(@Nullable String port) {
		this.port = port;
		if (port != null) {
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder path(String path) {
		this.pathBuilder.addPath(path);
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
		this.pathBuilder.addPathSegments(pathSegments);
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder replacePath(@Nullable String path) {
		this.pathBuilder = new CompositePathComponentBuilder();
		if (path != null) {
			this.pathBuilder.addPath(path);
		}
		resetSchemeSpecificPart();
		return this;
	}

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
			resetSchemeSpecificPart();
		}
		else {
			this.queryParams.clear();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder replaceQuery(@Nullable String query) {
		this.queryParams.clear();
		if (query != null) {
			query(query);
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder queryParam(String name, @Nullable Object... values) {
		Assert.notNull(name, "Name must not be null");
		if (!ObjectUtils.isEmpty(values)) {
			for (Object value : values) {
				String valueAsString = getQueryParamValue(value);
				this.queryParams.add(name, valueAsString);
			}
		}
		else {
			this.queryParams.add(name, null);
		}
		resetSchemeSpecificPart();
		return this;
	}

	private @Nullable String getQueryParamValue(@Nullable Object value) {
		if (value != null) {
			return (value instanceof Optional<?> optional ?
					optional.map(Object::toString).orElse(null) :
					value.toString());
		}
		return null;
	}

	@Override
	public UriComponentsBuilder queryParam(String name, @Nullable Collection<?> values) {
		return queryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
	}

	@Override
	public UriComponentsBuilder queryParamIfPresent(String name, Optional<?> value) {
		value.ifPresent(v -> {
			if (v instanceof Collection<?> values) {
				queryParam(name, values);
			}
			else {
				queryParam(name, v);
			}
		});
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @since 4.0
	 */
	@Override
	public UriComponentsBuilder queryParams(@Nullable MultiValueMap<String, String> params) {
		if (params != null) {
			this.queryParams.addAll(params);
			resetSchemeSpecificPart();
		}
		return this;
	}

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

	@Override
	public UriComponentsBuilder replaceQueryParam(String name, @Nullable Collection<?> values) {
		return replaceQueryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
	}

	/**
	 * {@inheritDoc}
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

	void resetPortIfDefaultForScheme() {
		if (this.scheme != null &&
				(((this.scheme.equals("http") || this.scheme.equals("ws")) && "80".equals(this.port)) ||
						((this.scheme.equals("https") || this.scheme.equals("wss")) && "443".equals(this.port)))) {
			port(null);
		}
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


	/**
	 * Enum to provide a choice of URI parsers to use in {@link #fromUriString(String, ParserType)}.
	 * @since 6.2
	 */
	public enum ParserType {

		/**
		 * This parser type expects URI's to conform to RFC 3986 syntax.
		 */
		RFC,

		/**
		 * This parser follows the
		 * <a href="https://url.spec.whatwg.org/#url-parsing">URL parsing algorithm</a>
		 * in the WhatWG URL Living standard that browsers implement to align on
		 * lenient handling of user typed URL's that may not conform to RFC syntax.
		 * @see <a href="https://url.spec.whatwg.org">URL Living Standard</a>
		 * @see <a href="https://github.com/web-platform-tests/wpt/tree/master/url">URL tests</a>
		 */
		WHAT_WG

	}


	private interface PathComponentBuilder {

		@Nullable PathComponent build();

		PathComponentBuilder cloneBuilder();
	}


	private static class CompositePathComponentBuilder implements PathComponentBuilder {

		private final Deque<PathComponentBuilder> builders = new ArrayDeque<>();

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
		private <T> @Nullable T getLastBuilder(Class<T> builderClass) {
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
		public @Nullable PathComponent build() {
			if (this.path.isEmpty()) {
				return null;
			}
			String sanitized = getSanitizedPath(this.path);
			return new HierarchicalUriComponents.FullPathComponent(sanitized);
		}

		private static String getSanitizedPath(final StringBuilder path) {
			int index = path.indexOf("//");
			if (index >= 0) {
				StringBuilder sanitized = new StringBuilder(path);
				while (index != -1) {
					sanitized.deleteCharAt(index);
					index = sanitized.indexOf("//", index);
				}
				return sanitized.toString();
			}
			return path.toString();
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

		private final List<String> pathSegments = new ArrayList<>();

		public void append(String... pathSegments) {
			for (String pathSegment : pathSegments) {
				if (StringUtils.hasText(pathSegment)) {
					this.pathSegments.add(pathSegment);
				}
			}
		}

		@Override
		public @Nullable PathComponent build() {
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
