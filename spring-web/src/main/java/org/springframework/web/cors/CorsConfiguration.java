/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.cors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A container for CORS configuration along with methods to check against the
 * actual origin, HTTP methods, and headers of a given request.
 *
 * <p>By default a newly created {@code CorsConfiguration} does not permit any
 * cross-origin requests and must be configured explicitly to indicate what
 * should be allowed. Use {@link #applyPermitDefaultValues()} to flip the
 * initialization model to start with open defaults that permit all cross-origin
 * requests for GET, HEAD, and POST requests.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Ruslan Akhundov
 * @since 4.2
 * @see <a href="https://www.w3.org/TR/cors/">CORS spec</a>
 */
public class CorsConfiguration {

	/** Wildcard representing <em>all</em> origins, methods, or headers. */
	public static final String ALL = "*";

	private static final List<String> ALL_LIST = Collections.singletonList(ALL);

	private static final OriginPattern ALL_PATTERN = new OriginPattern("*");

	private static final List<OriginPattern> ALL_PATTERN_LIST = Collections.singletonList(ALL_PATTERN);

	private static final List<String> DEFAULT_PERMIT_ALL = Collections.singletonList(ALL);

	private static final List<HttpMethod> DEFAULT_METHODS = List.of(HttpMethod.GET, HttpMethod.HEAD);

	private static final List<String> DEFAULT_PERMIT_METHODS = List.of(HttpMethod.GET.name(),
			HttpMethod.HEAD.name(), HttpMethod.POST.name());


	@Nullable
	private List<String> allowedOrigins;

	@Nullable
	private List<OriginPattern> allowedOriginPatterns;

	@Nullable
	private List<String> allowedMethods;

	@Nullable
	private List<HttpMethod> resolvedMethods = DEFAULT_METHODS;

	@Nullable
	private List<String> allowedHeaders;

	@Nullable
	private List<String> exposedHeaders;

	@Nullable
	private Boolean allowCredentials;

	@Nullable
	private Boolean allowPrivateNetwork;

	@Nullable
	private Long maxAge;


	/**
	 * Construct a new {@code CorsConfiguration} instance with no cross-origin
	 * requests allowed for any origin by default.
	 * @see #applyPermitDefaultValues()
	 */
	public CorsConfiguration() {
	}

	/**
	 * Construct a new {@code CorsConfiguration} instance by copying all
	 * values from the supplied {@code CorsConfiguration}.
	 */
	public CorsConfiguration(CorsConfiguration other) {
		this.allowedOrigins = other.allowedOrigins;
		this.allowedOriginPatterns = other.allowedOriginPatterns;
		this.allowedMethods = other.allowedMethods;
		this.resolvedMethods = other.resolvedMethods;
		this.allowedHeaders = other.allowedHeaders;
		this.exposedHeaders = other.exposedHeaders;
		this.allowCredentials = other.allowCredentials;
		this.allowPrivateNetwork = other.allowPrivateNetwork;
		this.maxAge = other.maxAge;
	}


	/**
	 * A list of origins for which cross-origin requests are allowed where each
	 * value may be one of the following:
	 * <ul>
	 * <li>a specific domain, e.g. {@code "https://domain1.com"}
	 * <li>comma-delimited list of specific domains, e.g.
	 * {@code "https://a1.com,https://a2.com"}; this is convenient when a value
	 * is resolved through a property placeholder, e.g. {@code "${origin}"};
	 * note that such placeholders must be resolved externally.
	 * <li>the CORS defined special value {@code "*"} for all origins
	 * </ul>
	 * <p>For matched pre-flight and actual requests the
	 * {@code Access-Control-Allow-Origin} response header is set either to the
	 * matched domain value or to {@code "*"}. Keep in mind however that the
	 * CORS spec does not allow {@code "*"} when {@link #setAllowCredentials
	 * allowCredentials} is set to {@code true}, and does not recommend {@code "*"}
	 * when {@link #setAllowPrivateNetwork allowPrivateNetwork} is set to {@code true}.
	 * As a consequence, those combinations are rejected in favor of using
	 * {@link #setAllowedOriginPatterns allowedOriginPatterns} instead.
	 * <p>By default this is not set which means that no origins are allowed.
	 * However, an instance of this class is often initialized further, e.g. for
	 * {@code @CrossOrigin}, via {@link #applyPermitDefaultValues()}.
	 */
	public void setAllowedOrigins(@Nullable List<String> origins) {
		if (origins == null) {
			this.allowedOrigins = null;
		}
		else {
			this.allowedOrigins = new ArrayList<>(origins.size());
			for (String origin : origins) {
				addAllowedOrigin(origin);
			}
		}
	}

	private String trimTrailingSlash(String origin) {
		return (origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin);
	}

	/**
	 * Return the configured origins to allow, or {@code null} if none.
	 */
	@Nullable
	public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	/**
	 * Variant of {@link #setAllowedOrigins} for adding one origin at a time.
	 */
	@SuppressWarnings("NullAway")
	public void addAllowedOrigin(@Nullable String origin) {
		if (origin == null) {
			return;
		}
		if (this.allowedOrigins == null) {
			this.allowedOrigins = new ArrayList<>(4);
		}
		else if (this.allowedOrigins == DEFAULT_PERMIT_ALL && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
			setAllowedOrigins(DEFAULT_PERMIT_ALL);
		}
		parseCommaDelimitedOrigin(origin, value -> {
			value = trimTrailingSlash(value);
			this.allowedOrigins.add(value);
		});
	}

	/**
	 * Alternative to {@link #setAllowedOrigins} that supports more flexible
	 * origins patterns with "*" anywhere in the host name in addition to port
	 * lists. Examples:
	 * <ul>
	 * <li>{@literal https://*.domain1.com} -- domains ending with domain1.com
	 * <li>{@literal https://*.domain1.com:[8080,8081]} -- domains ending with
	 * domain1.com on port 8080 or port 8081
	 * <li>{@literal https://*.domain1.com:[*]} -- domains ending with
	 * domain1.com on any port, including the default port
	 * <li>comma-delimited list of patters, e.g.
	 * {@code "https://*.a1.com,https://*.a2.com"}; this is convenient when a
	 * value is resolved through a property placeholder, e.g. {@code "${origin}"};
	 * note that such placeholders must be resolved externally.
	 * </ul>
	 * <p>In contrast to {@link #setAllowedOrigins(List) allowedOrigins} which
	 * only supports "*" and cannot be used with {@code allowCredentials} or
	 * {@code allowPrivateNetwork}, when an {@code allowedOriginPattern} is matched,
	 * the {@code Access-Control-Allow-Origin} response header is set to the
	 * matched origin and not to {@code "*"} nor to the pattern.
	 * Therefore, {@code allowedOriginPatterns} can be used in combination with
	 * {@link #setAllowCredentials} and {@link #setAllowPrivateNetwork} set to
	 * {@code true}.
	 * <p>By default this is not set.
	 * @since 5.3
	 */
	public CorsConfiguration setAllowedOriginPatterns(@Nullable List<String> allowedOriginPatterns) {
		if (allowedOriginPatterns == null) {
			this.allowedOriginPatterns = null;
		}
		else {
			this.allowedOriginPatterns = new ArrayList<>(allowedOriginPatterns.size());
			for (String patternValue : allowedOriginPatterns) {
				addAllowedOriginPattern(patternValue);
			}
		}
		return this;
	}

	/**
	 * Return the configured origins patterns to allow, or {@code null} if none.
	 * @since 5.3
	 */
	@Nullable
	public List<String> getAllowedOriginPatterns() {
		if (this.allowedOriginPatterns == null) {
			return null;
		}
		return this.allowedOriginPatterns.stream()
				.map(OriginPattern::getDeclaredPattern)
				.toList();
	}

	/**
	 * Variant of {@link #setAllowedOriginPatterns} for adding one origin at a time.
	 * @since 5.3
	 */
	@SuppressWarnings("NullAway")
	public void addAllowedOriginPattern(@Nullable String originPattern) {
		if (originPattern == null) {
			return;
		}
		if (this.allowedOriginPatterns == null) {
			this.allowedOriginPatterns = new ArrayList<>(4);
		}
		parseCommaDelimitedOrigin(originPattern, value -> {
			value = trimTrailingSlash(value);
			this.allowedOriginPatterns.add(new OriginPattern(value));
			if (this.allowedOrigins == DEFAULT_PERMIT_ALL) {
				this.allowedOrigins = null;
			}
		});
	}

	private static void parseCommaDelimitedOrigin(String rawValue, Consumer<String> valueConsumer) {
		if (rawValue.indexOf(',') == -1) {
			valueConsumer.accept(rawValue);
			return;
		}
		int start = 0;
		boolean withinPortRange = false;
		for (int current = 0; current < rawValue.length(); current++) {
			switch (rawValue.charAt(current)) {
				case '[' -> withinPortRange = true;
				case ']' -> withinPortRange = false;
				case ',' -> {
					if (!withinPortRange) {
						String originValue = rawValue.substring(start, current).trim();
						valueConsumer.accept(originValue);
						start = current + 1;
					}
				}
			}
		}
		if (start < rawValue.length()) {
			String originValue = rawValue.substring(start).trim();
			valueConsumer.accept(originValue);
		}
	}

	/**
	 * Set the HTTP methods to allow, e.g. {@code "GET"}, {@code "POST"},
	 * {@code "PUT"}, etc. The special value {@code "*"} allows all methods.
	 * <p>{@code Access-Control-Allow-Methods} response header is set either
	 * to the configured method or to {@code "*"}. Keep in mind however that the
	 * CORS spec does not allow {@code "*"} when {@link #setAllowCredentials
	 * allowCredentials} is set to {@code true}, that combination is handled
	 * by copying the method specified in the CORS preflight request.
	 * <p>If not set, only {@code "GET"} and {@code "HEAD"} are allowed.
	 * <p>By default this is not set.
	 * <p><strong>Note:</strong> CORS checks use values from "Forwarded"
	 * (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers,
	 * if present, in order to reflect the client-originated address.
	 * Consider using the {@code ForwardedHeaderFilter} in order to choose from a
	 * central place whether to extract and use, or to discard such headers.
	 * See the Spring Framework reference for more on this filter.
	 */
	public void setAllowedMethods(@Nullable List<String> allowedMethods) {
		this.allowedMethods = (allowedMethods != null ? new ArrayList<>(allowedMethods) : null);
		if (!CollectionUtils.isEmpty(allowedMethods)) {
			this.resolvedMethods = new ArrayList<>(allowedMethods.size());
			for (String method : allowedMethods) {
				if (ALL.equals(method)) {
					this.resolvedMethods = null;
					break;
				}
				this.resolvedMethods.add(HttpMethod.valueOf(method));
			}
		}
		else {
			this.resolvedMethods = DEFAULT_METHODS;
		}
	}

	/**
	 * Return the allowed HTTP methods, or {@code null} in which case
	 * only {@code "GET"} and {@code "HEAD"} allowed.
	 * @see #setAllowedMethods(List)
	 * @see #addAllowedMethod(HttpMethod)
	 * @see #addAllowedMethod(String)
	 */
	@Nullable
	public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	/**
	 * Variant of {@link #setAllowedMethods} for adding one allowed method at a time.
	 */
	public void addAllowedMethod(HttpMethod method) {
		addAllowedMethod(method.name());
	}

	/**
	 * Variant of {@link #setAllowedMethods} for adding one allowed method at a time.
	 */
	public void addAllowedMethod(String method) {
		if (StringUtils.hasText(method)) {
			if (this.allowedMethods == null) {
				this.allowedMethods = new ArrayList<>(4);
				this.resolvedMethods = new ArrayList<>(4);
			}
			else if (this.allowedMethods == DEFAULT_PERMIT_METHODS) {
				setAllowedMethods(DEFAULT_PERMIT_METHODS);
			}
			this.allowedMethods.add(method);
			if (ALL.equals(method)) {
				this.resolvedMethods = null;
			}
			else if (this.resolvedMethods != null) {
				this.resolvedMethods.add(HttpMethod.valueOf(method));
			}
		}
	}

	/**
	 * Set the list of headers that a pre-flight request can list as allowed
	 * for use during an actual request. The special value {@code "*"} allows
	 * actual requests to send any header.
	 * <p>{@code Access-Control-Allow-Headers} response header is set either
	 * to the configured list of headers or to {@code "*"}. Keep in mind however
	 * that the CORS spec does not allow {@code "*"} when {@link #setAllowCredentials
	 * allowCredentials} is set to {@code true}, that combination is handled by
	 * copying the headers specified in the CORS preflight request.
	 * <p>A header name is not required to be listed if it is one of:
	 * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
	 * {@code Last-Modified}, or {@code Pragma}.
	 * <p>By default this is not set.
	 */
	public void setAllowedHeaders(@Nullable List<String> allowedHeaders) {
		this.allowedHeaders = (allowedHeaders != null ? new ArrayList<>(allowedHeaders) : null);
	}

	/**
	 * Return the allowed actual request headers, or {@code null} if none.
	 * @see #addAllowedHeader(String)
	 * @see #setAllowedHeaders(List)
	 */
	@Nullable
	public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	/**
	 * Variant of {@link #setAllowedHeaders(List)} for adding one allowed header at a time.
	 */
	public void addAllowedHeader(String allowedHeader) {
		if (this.allowedHeaders == null) {
			this.allowedHeaders = new ArrayList<>(4);
		}
		else if (this.allowedHeaders == DEFAULT_PERMIT_ALL) {
			setAllowedHeaders(DEFAULT_PERMIT_ALL);
		}
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * Set the list of response headers that an actual response might have
	 * and can be exposed to the client. The special value {@code "*"}
	 * allows all headers to be exposed.
	 * <p>{@code Access-Control-Expose-Headers} response header is set either
	 * to the configured list of headers or to {@code "*"}. While the CORS
	 * spec does not allow {@code "*"} when {@code Access-Control-Allow-Credentials}
	 * is set to {@code true}, most browsers support it and
	 * the response headers are not all available during the CORS processing,
	 * so as a consequence {@code "*"} is the header value used when specified
	 * regardless of the value of the `allowCredentials` property.
	 * <p>A header name is not required to be listed if it is one of:
	 * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
	 * {@code Last-Modified}, or {@code Pragma}.
	 * <p>By default this is not set.
	 */
	public void setExposedHeaders(@Nullable List<String> exposedHeaders) {
		this.exposedHeaders = (exposedHeaders != null ? new ArrayList<>(exposedHeaders) : null);
	}

	/**
	 * Return the configured response headers to expose, or {@code null} if none.
	 * @see #addExposedHeader(String)
	 * @see #setExposedHeaders(List)
	 */
	@Nullable
	public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	/**
	 * Variant of {@link #setExposedHeaders} for adding one exposed header at a time.
	 */
	public void addExposedHeader(String exposedHeader) {
		if (this.exposedHeaders == null) {
			this.exposedHeaders = new ArrayList<>(4);
		}
		this.exposedHeaders.add(exposedHeader);
	}

	/**
	 * Whether user credentials are supported.
	 * <p>Setting this property has an impact on how {@link #setAllowedOrigins(List)
	 * origins}, {@link #setAllowedOriginPatterns(List) originPatterns},
	 * {@link #setAllowedMethods(List)  allowedMethods} and
	 * {@link #setAllowedHeaders(List) allowedHeaders} are processed, see related
	 * API documentation for more details.
	 * <p><strong>NOTE:</strong> Be aware that this option establishes a high
	 * level of trust with the configured domains and also increases the surface
	 * attack of the web application by exposing sensitive user-specific
	 * information such as cookies and CSRF tokens.
	 * <p>By default this is not set (i.e. user credentials are not supported).
	 */
	public void setAllowCredentials(@Nullable Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * Return the configured {@code allowCredentials} flag, or {@code null} if none.
	 * @see #setAllowCredentials(Boolean)
	 */
	@Nullable
	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	/**
	 * Whether private network access is supported for user-agents restricting such access by default.
	 * <p>Private network requests are requests whose target server's IP address is more private than
	 * that from which the request initiator was fetched. For example, a request from a public website
	 * (https://example.com) to a private website (https://router.local), or a request from a private
	 * website to localhost.
	 * <p>Setting this property has an impact on how {@link #setAllowedOrigins(List)
	 * origins} and {@link #setAllowedOriginPatterns(List) originPatterns} are processed,
	 * see related API documentation for more details.
	 * <p>By default this is not set (i.e. private network access is not supported).
	 * @since 5.3.32
	 * @see <a href="https://wicg.github.io/private-network-access/">Private network access specifications</a>
	 */
	public void setAllowPrivateNetwork(@Nullable Boolean allowPrivateNetwork) {
		this.allowPrivateNetwork = allowPrivateNetwork;
	}

	/**
	 * Return the configured {@code allowPrivateNetwork} flag, or {@code null} if none.
	 * @since 5.3.32
	 * @see #setAllowPrivateNetwork(Boolean)
	 */
	@Nullable
	public Boolean getAllowPrivateNetwork() {
		return this.allowPrivateNetwork;
	}

	/**
	 * Configure how long, as a duration, the response from a pre-flight request
	 * can be cached by clients.
	 * @since 5.2
	 * @see #setMaxAge(Long)
	 */
	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge.getSeconds();
	}

	/**
	 * Configure how long, in seconds, the response from a pre-flight request
	 * can be cached by clients.
	 * <p>By default this is not set.
	 */
	public void setMaxAge(@Nullable Long maxAge) {
		this.maxAge = maxAge;
	}

	/**
	 * Return the configured {@code maxAge} value, or {@code null} if none.
	 * @see #setMaxAge(Long)
	 */
	@Nullable
	public Long getMaxAge() {
		return this.maxAge;
	}


	/**
	 * By default {@code CorsConfiguration} does not permit any cross-origin
	 * requests and must be configured explicitly. Use this method to switch to
	 * defaults that permit all cross-origin requests for GET, HEAD, and POST,
	 * but not overriding any values that have already been set.
	 * <p>The following defaults are applied for values that are not set:
	 * <ul>
	 * <li>Allow all origins with the special value {@code "*"} defined in the
	 * CORS spec. This is set only if neither {@link #setAllowedOrigins origins}
	 * nor {@link #setAllowedOriginPatterns originPatterns} are already set.</li>
	 * <li>Allow "simple" methods {@code GET}, {@code HEAD} and {@code POST}.</li>
	 * <li>Allow all headers.</li>
	 * <li>Set max age to 1800 seconds (30 minutes).</li>
	 * </ul>
	 */
	public CorsConfiguration applyPermitDefaultValues() {
		if (this.allowedOrigins == null && this.allowedOriginPatterns == null) {
			this.allowedOrigins = DEFAULT_PERMIT_ALL;
		}
		if (this.allowedMethods == null) {
			this.allowedMethods = DEFAULT_PERMIT_METHODS;
			this.resolvedMethods = DEFAULT_PERMIT_METHODS
					.stream().map(HttpMethod::valueOf).toList();
		}
		if (this.allowedHeaders == null) {
			this.allowedHeaders = DEFAULT_PERMIT_ALL;
		}
		if (this.maxAge == null) {
			this.maxAge = 1800L;
		}
		return this;
	}

	/**
	 * Validate that when {@link #setAllowCredentials allowCredentials} is {@code true},
	 * {@link #setAllowedOrigins allowedOrigins} does not contain the special
	 * value {@code "*"} since in that case the "Access-Control-Allow-Origin"
	 * cannot be set to {@code "*"}.
	 * @throws IllegalArgumentException if the validation fails
	 * @since 5.3
	 */
	public void validateAllowCredentials() {
		if (this.allowCredentials == Boolean.TRUE &&
				this.allowedOrigins != null && this.allowedOrigins.contains(ALL)) {

			throw new IllegalArgumentException(
					"When allowCredentials is true, allowedOrigins cannot contain the special value \"*\" " +
							"since that cannot be set on the \"Access-Control-Allow-Origin\" response header. " +
							"To allow credentials to a set of origins, list them explicitly " +
							"or consider using \"allowedOriginPatterns\" instead.");
		}
	}

	/**
	 * Validate that when {@link #setAllowPrivateNetwork allowPrivateNetwork} is {@code true},
	 * {@link #setAllowedOrigins allowedOrigins} does not contain the special
	 * value {@code "*"} since this is insecure.
	 * @throws IllegalArgumentException if the validation fails
	 * @since 5.3.32
	 */
	public void validateAllowPrivateNetwork() {
		if (this.allowPrivateNetwork == Boolean.TRUE &&
				this.allowedOrigins != null && this.allowedOrigins.contains(ALL)) {

			throw new IllegalArgumentException(
					"When allowPrivateNetwork is true, allowedOrigins cannot contain the special value \"*\" " +
							"as it is not recommended from a security perspective. " +
							"To allow private network access to a set of origins, list them explicitly " +
							"or consider using \"allowedOriginPatterns\" instead.");
		}
	}

	/**
	 * Combine the non-null properties of the supplied
	 * {@code CorsConfiguration} with this one.
	 * <p>When combining single values like {@code allowCredentials} or
	 * {@code maxAge}, {@code this} properties are overridden by non-null
	 * {@code other} properties if any.
	 * <p>Combining lists like {@code allowedOrigins}, {@code allowedMethods},
	 * {@code allowedHeaders} or {@code exposedHeaders} is done in an additive
	 * way. For example, combining {@code ["GET", "POST"]} with
	 * {@code ["PATCH"]} results in {@code ["GET", "POST", "PATCH"]}. However,
	 * combining {@code ["GET", "POST"]} with {@code ["*"]} results in
	 * {@code ["*"]}. Note also that default permit values set by
	 * {@link CorsConfiguration#applyPermitDefaultValues()} are overridden by
	 * any explicitly defined values.
	 * @return the combined {@code CorsConfiguration}, or {@code this}
	 * configuration if the supplied configuration is {@code null}
	 */
	public CorsConfiguration combine(@Nullable CorsConfiguration other) {
		if (other == null) {
			return this;
		}
		// Bypass setAllowedOrigins to avoid re-compiling patterns
		CorsConfiguration config = new CorsConfiguration(this);
		List<String> origins = combine(getAllowedOrigins(), other.getAllowedOrigins());
		List<OriginPattern> patterns = combinePatterns(this.allowedOriginPatterns, other.allowedOriginPatterns);
		config.allowedOrigins = (origins == DEFAULT_PERMIT_ALL && !CollectionUtils.isEmpty(patterns) ? null : origins);
		config.allowedOriginPatterns = patterns;
		config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));
		config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));
		config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));
		Boolean allowCredentials = other.getAllowCredentials();
		if (allowCredentials != null) {
			config.setAllowCredentials(allowCredentials);
		}
		Boolean allowPrivateNetwork = other.getAllowPrivateNetwork();
		if (allowPrivateNetwork != null) {
			config.setAllowPrivateNetwork(allowPrivateNetwork);
		}
		Long maxAge = other.getMaxAge();
		if (maxAge != null) {
			config.setMaxAge(maxAge);
		}
		return config;
	}

	private List<String> combine(@Nullable List<String> source, @Nullable List<String> other) {
		if (other == null) {
			return (source != null ? source : Collections.emptyList());
		}
		if (source == null) {
			return other;
		}
		if (source == DEFAULT_PERMIT_ALL || source == DEFAULT_PERMIT_METHODS) {
			return other;
		}
		if (other == DEFAULT_PERMIT_ALL || other == DEFAULT_PERMIT_METHODS) {
			return source;
		}
		if (source.contains(ALL) || other.contains(ALL)) {
			return ALL_LIST;
		}
		Set<String> combined = CollectionUtils.newLinkedHashSet(source.size() + other.size());
		combined.addAll(source);
		combined.addAll(other);
		return new ArrayList<>(combined);
	}

	private List<OriginPattern> combinePatterns(
			@Nullable List<OriginPattern> source, @Nullable List<OriginPattern> other) {

		if (other == null) {
			return (source != null ? source : Collections.emptyList());
		}
		if (source == null) {
			return other;
		}
		if (source.contains(ALL_PATTERN) || other.contains(ALL_PATTERN)) {
			return ALL_PATTERN_LIST;
		}
		Set<OriginPattern> combined = CollectionUtils.newLinkedHashSet(source.size() + other.size());
		combined.addAll(source);
		combined.addAll(other);
		return new ArrayList<>(combined);
	}


	/**
	 * Check the origin of the request against the configured allowed origins.
	 * @param origin the origin to check
	 * @return the origin to use for the response, or {@code null} which
	 * means the request origin is not allowed
	 */
	@Nullable
	public String checkOrigin(@Nullable String origin) {
		if (!StringUtils.hasText(origin)) {
			return null;
		}
		String originToCheck = trimTrailingSlash(origin);
		if (!ObjectUtils.isEmpty(this.allowedOrigins)) {
			if (this.allowedOrigins.contains(ALL)) {
				validateAllowCredentials();
				validateAllowPrivateNetwork();
				return ALL;
			}
			for (String allowedOrigin : this.allowedOrigins) {
				if (originToCheck.equalsIgnoreCase(allowedOrigin)) {
					return origin;
				}
			}
		}
		if (!ObjectUtils.isEmpty(this.allowedOriginPatterns)) {
			for (OriginPattern p : this.allowedOriginPatterns) {
				if (p.getDeclaredPattern().equals(ALL) || p.getPattern().matcher(originToCheck).matches()) {
					return origin;
				}
			}
		}
		return null;
	}

	/**
	 * Check the HTTP request method (or the method from the
	 * {@code Access-Control-Request-Method} header on a pre-flight request)
	 * against the configured allowed methods.
	 * @param requestMethod the HTTP request method to check
	 * @return the list of HTTP methods to list in the response of a pre-flight
	 * request, or {@code null} if the supplied {@code requestMethod} is not allowed
	 */
	@Nullable
	public List<HttpMethod> checkHttpMethod(@Nullable HttpMethod requestMethod) {
		if (requestMethod == null) {
			return null;
		}
		if (this.resolvedMethods == null) {
			return Collections.singletonList(requestMethod);
		}
		return (this.resolvedMethods.contains(requestMethod) ? this.resolvedMethods : null);
	}

	/**
	 * Check the supplied request headers (or the headers listed in the
	 * {@code Access-Control-Request-Headers} of a pre-flight request) against
	 * the configured allowed headers.
	 * @param requestHeaders the request headers to check
	 * @return the list of allowed headers to list in the response of a pre-flight
	 * request, or {@code null} if none of the supplied request headers is allowed
	 */
	@Nullable
	public List<String> checkHeaders(@Nullable List<String> requestHeaders) {
		if (requestHeaders == null) {
			return null;
		}
		if (requestHeaders.isEmpty()) {
			return Collections.emptyList();
		}
		if (ObjectUtils.isEmpty(this.allowedHeaders)) {
			return null;
		}

		boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
		int maxResultSize = allowAnyHeader ? requestHeaders.size()
				: Math.min(requestHeaders.size(), this.allowedHeaders.size());
		List<String> result = new ArrayList<>(maxResultSize);
		for (String requestHeader : requestHeaders) {
			if (StringUtils.hasText(requestHeader)) {
				requestHeader = requestHeader.trim();
				if (allowAnyHeader) {
					result.add(requestHeader);
				}
				else {
					for (String allowedHeader : this.allowedHeaders) {
						if (requestHeader.equalsIgnoreCase(allowedHeader)) {
							result.add(requestHeader);
							break;
						}
					}
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}


	/**
	 * Contains both the user-declared pattern (e.g. "https://*.domain.com") and
	 * the regex {@link Pattern} derived from it.
	 */
	private static class OriginPattern {

		private static final Pattern PORTS_PATTERN = Pattern.compile("(.*):\\[(\\*|\\d+(,\\d+)*)]");

		private final String declaredPattern;

		private final Pattern pattern;

		OriginPattern(String declaredPattern) {
			this.declaredPattern = declaredPattern;
			this.pattern = initPattern(declaredPattern);
		}

		private static Pattern initPattern(String patternValue) {
			String portList = null;
			Matcher matcher = PORTS_PATTERN.matcher(patternValue);
			if (matcher.matches()) {
				patternValue = matcher.group(1);
				portList = matcher.group(2);
			}

			patternValue = "\\Q" + patternValue + "\\E";
			patternValue = patternValue.replace("*", "\\E.*\\Q");

			if (portList != null) {
				patternValue += (portList.equals(ALL) ? "(:\\d+)?" : ":(" + portList.replace(',', '|') + ")");
			}

			return Pattern.compile(patternValue);
		}

		public String getDeclaredPattern() {
			return this.declaredPattern;
		}

		public Pattern getPattern() {
			return this.pattern;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || !getClass().equals(other.getClass())) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(
					this.declaredPattern, ((OriginPattern) other).declaredPattern);
		}

		@Override
		public int hashCode() {
			return this.declaredPattern.hashCode();
		}

		@Override
		public String toString() {
			return this.declaredPattern;
		}
	}

}
