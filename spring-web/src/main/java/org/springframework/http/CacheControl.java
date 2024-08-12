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

package org.springframework.http;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * A builder for creating "Cache-Control" HTTP response headers. As of Spring
 * Framework 6.2, this class is immutable.
 *
 * <p>Adding Cache-Control directives to HTTP responses can significantly improve the client
 * experience when interacting with a web application. This builder creates opinionated
 * "Cache-Control" headers with response directives only, with several use cases in mind.
 *
 * <ul>
 * <li>Caching HTTP responses with {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS)}
 * will result in {@code Cache-Control: "max-age=3600"}</li>
 * <li>Preventing cache with {@code CacheControl cc = CacheControl.noStore()}
 * will result in {@code Cache-Control: "no-store"}</li>
 * <li>Advanced cases like {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform().cachePublic()}
 * will result in {@code Cache-Control: "max-age=3600, no-transform, public"}</li>
 * </ul>
 *
 * <p>Note that to be efficient, Cache-Control headers should be written along HTTP validators
 * such as "Last-Modified" or "ETag" headers.
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.2
 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2">rfc7234 section 5.2.2</a>
 * @see <a href="https://developers.google.com/web/fundamentals/performance/optimizing-content-efficiency/http-caching">
 * HTTP caching - Google developers reference</a>
 * @see <a href="https://www.mnot.net/cache_docs/">Mark Nottingham's cache documentation</a>
 */
public class CacheControl {

	private static final CacheControl EMPTY = new CacheControl();

	@Nullable
	private final Duration maxAge;

	private final boolean noCache;

	private final boolean noStore;

	private final boolean mustRevalidate;

	private final boolean noTransform;

	private final boolean cachePublic;

	private final boolean cachePrivate;

	private final boolean proxyRevalidate;

	@Nullable
	private final Duration staleWhileRevalidate;

	@Nullable
	private final Duration staleIfError;

	@Nullable
	private final Duration sMaxAge;

	private final boolean immutable;

	/**
	 * Create an empty CacheControl instance.
	 * @see #empty()
	 */
	protected CacheControl() {
		this(null, false, false, false, false, false, false, false, null, null, null, false);
	}

	private CacheControl(@Nullable Duration maxAge, boolean noCache, boolean noStore,
			boolean mustRevalidate, boolean noTransform, boolean cachePublic,
			boolean cachePrivate, boolean proxyRevalidate, @Nullable Duration staleWhileRevalidate,
			@Nullable Duration staleIfError, @Nullable Duration sMaxAge, boolean immutable) {
		this.maxAge = maxAge;
		this.noCache = noCache;
		this.noStore = noStore;
		this.mustRevalidate = mustRevalidate;
		this.noTransform = noTransform;
		this.cachePublic = cachePublic;
		this.cachePrivate = cachePrivate;
		this.proxyRevalidate = proxyRevalidate;
		this.staleWhileRevalidate = staleWhileRevalidate;
		this.staleIfError = staleIfError;
		this.sMaxAge = sMaxAge;
		this.immutable = immutable;
	}

	/**
	 * Return an empty directive.
	 * <p>This is well suited for using other optional directives without "max-age",
	 * "no-cache" or "no-store".
	 * @return an empty directive
	 */
	public static CacheControl empty() {
		return EMPTY;
	}

	/**
	 * Return a "max-age=" directive.
	 * <p>This directive is well suited for publicly caching resources, knowing that
	 * they won't change within the configured amount of time. Additional directives
	 * can be also used, in case resources shouldn't be cached ({@link #cachePrivate()})
	 * or transformed ({@link #noTransform()}) by shared caches.
	 * <p>In order to prevent caches to reuse the cached response even when it has
	 * become stale (i.e. the "max-age" delay is passed), the "must-revalidate"
	 * directive should be set ({@link #mustRevalidate()}
	 * @param maxAge the maximum time the response should be cached
	 * @param unit the time unit of the {@code maxAge} argument
	 * @return a CacheControl instance with a "max-age" directive
	 * @see #maxAge(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 section 5.2.2.8</a>
	 */
	public static CacheControl maxAge(long maxAge, TimeUnit unit) {
		return maxAge(Duration.ofSeconds(unit.toSeconds(maxAge)));
	}

	/**
	 * Return a "max-age=" directive.
	 * <p>This directive is well suited for publicly caching resources, knowing that
	 * they won't change within the configured amount of time. Additional directives
	 * can be also used, in case resources shouldn't be cached ({@link #cachePrivate()})
	 * or transformed ({@link #noTransform()}) by shared caches.
	 * <p>In order to prevent caches to reuse the cached response even when it has
	 * become stale (i.e. the "max-age" delay is passed), the "must-revalidate"
	 * directive should be set ({@link #mustRevalidate()}
	 * @param maxAge the maximum time the response should be cached
	 * @return a CacheControl instance with a "max-age" directive
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 section 5.2.2.8</a>
	 */
	public static CacheControl maxAge(Duration maxAge) {
		return new CacheControl(maxAge, false, false, false, false, false, false, false,
				null, null, null, false);
	}

	/**
	 * Return a "no-cache" directive.
	 * <p>This directive is well suited for telling caches that the response
	 * can be reused only if the client revalidates it with the server.
	 * This directive won't disable cache altogether and may result with clients
	 * sending conditional requests (with "ETag", "If-Modified-Since" headers)
	 * and the server responding with "304 - Not Modified" status.
	 * <p>In order to disable caching and minimize requests/responses exchanges,
	 * the {@link #noStore()} directive should be used instead of {@code #noCache()}.
	 * @return a CacheControl instance with a "no-cache" directive
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.2">rfc7234 section 5.2.2.2</a>
	 */
	public static CacheControl noCache() {
		return new CacheControl(null, true, false, false, false, false, false, false,
				null, null, null, false);
	}

	/**
	 * Return a "no-store" directive.
	 * <p>This directive is well suited for preventing caches (browsers and proxies)
	 * to cache the content of responses.
	 * @return a CacheControl instance with a "no-store" directive
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.3">rfc7234 section 5.2.2.3</a>
	 */
	public static CacheControl noStore() {
		return new CacheControl(null, false, true, false, false, false, false, false,
				null, null, null, false);
	}


	/**
	 * Return a new instance with an additional "must-revalidate" directive.
	 * <p>This directive indicates that once it has become stale, a cache MUST NOT
	 * use the response to satisfy subsequent requests without successful validation
	 * on the origin server.
	 * @return a new CacheControl instance with an additional "must-revalidate" directive
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.1">rfc7234 section 5.2.2.1</a>
	 */
	public CacheControl mustRevalidate() {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, true, this.noTransform,
				this.cachePublic, this.cachePrivate, this.proxyRevalidate, this.staleWhileRevalidate,
				this.staleIfError, this.sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "no-transform" directive.
	 * <p>This directive indicates that intermediaries (caches and others) should
	 * not transform the response content. This can be useful to force caches and
	 * CDNs not to automatically gzip or optimize the response content.
	 * @return a new CacheControl instance with an additional "no-transform" directive
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.4">rfc7234 section 5.2.2.4</a>
	 */
	public CacheControl noTransform() {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, true,
				this.cachePublic, this.cachePrivate, this.proxyRevalidate, this.staleWhileRevalidate,
				this.staleIfError, this.sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "public" directive.
	 * <p>This directive indicates that any cache MAY store the response,
	 * even if the response would normally be non-cacheable or cacheable
	 * only within a private cache.
	 * @return a new CacheControl instance with an additional "public" directive
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.5">rfc7234 section 5.2.2.5</a>
	 */
	public CacheControl cachePublic() {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, this.noTransform,
				true, this.cachePrivate, this.proxyRevalidate, this.staleWhileRevalidate,
				this.staleIfError, this.sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "private" directive.
	 * <p>This directive indicates that the response message is intended
	 * for a single user and MUST NOT be stored by a shared cache.
	 * @return a new CacheControl instance with an additional "private" directive
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.6">rfc7234 section 5.2.2.6</a>
	 */
	public CacheControl cachePrivate() {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, this.noTransform,
				this.cachePublic, true, this.proxyRevalidate, this.staleWhileRevalidate,
				this.staleIfError, this.sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "proxy-revalidate" directive.
	 * <p>This directive has the same meaning as the "must-revalidate" directive,
	 * except that it does not apply to private caches (i.e. browsers, HTTP clients).
	 * @return a new CacheControl instance with an additional "proxy-revalidate" directive
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.7">rfc7234 section 5.2.2.7</a>
	 */
	public CacheControl proxyRevalidate() {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, this.noTransform,
				this.cachePublic, this.cachePrivate, true, this.staleWhileRevalidate,
				this.staleIfError, this.sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "s-maxage" directive.
	 * <p>This directive indicates that, in shared caches, the maximum age specified
	 * by this directive overrides the maximum age specified by other directives.
	 * @param sMaxAge the maximum time the response should be cached
	 * @param unit the time unit of the {@code sMaxAge} argument
	 * @return a new CacheControl instance with an additional "s-maxage" directive
	 * @see #sMaxAge(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 section 5.2.2.9</a>
	 */
	public CacheControl sMaxAge(long sMaxAge, TimeUnit unit) {
		return sMaxAge(Duration.ofSeconds(unit.toSeconds(sMaxAge)));
	}

	/**
	 * Return a new instance with an additional "s-maxage" directive.
	 * <p>This directive indicates that, in shared caches, the maximum age specified
	 * by this directive overrides the maximum age specified by other directives.
	 * @param sMaxAge the maximum time the response should be cached
	 * @return a new CacheControl instance with an additional "s-maxage" directive
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 section 5.2.2.9</a>
	 */
	public CacheControl sMaxAge(Duration sMaxAge) {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, this.noTransform,
				this.cachePublic, this.cachePrivate, this.proxyRevalidate, this.staleWhileRevalidate,
				this.staleIfError, sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "stale-while-revalidate" directive.
	 * <p>This directive indicates that caches MAY serve the response in which it
	 * appears after it becomes stale, up to the indicated number of seconds.
	 * If a cached response is served stale due to the presence of this extension,
	 * the cache SHOULD attempt to revalidate it while still serving stale responses
	 * (i.e. without blocking).
	 * @param staleWhileRevalidate the maximum time the response should be used while being revalidated
	 * @param unit the time unit of the {@code staleWhileRevalidate} argument
	 * @return a new CacheControl instance with an additional "stale-while-revalidate" directive
	 * @see #staleWhileRevalidate(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 section 3</a>
	 */
	public CacheControl staleWhileRevalidate(long staleWhileRevalidate, TimeUnit unit) {
		return staleWhileRevalidate(Duration.ofSeconds(unit.toSeconds(staleWhileRevalidate)));
	}

	/**
	 * Return a new instance with an additional "stale-while-revalidate" directive.
	 * <p>This directive indicates that caches MAY serve the response in which it
	 * appears after it becomes stale, up to the indicated number of seconds.
	 * If a cached response is served stale due to the presence of this extension,
	 * the cache SHOULD attempt to revalidate it while still serving stale responses
	 * (i.e. without blocking).
	 * @param staleWhileRevalidate the maximum time the response should be used while being revalidated
	 * @return a new CacheControl instance with an additional "stale-while-revalidate" directive
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 section 3</a>
	 */
	public CacheControl staleWhileRevalidate(Duration staleWhileRevalidate) {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, this.noTransform,
				this.cachePublic, this.cachePrivate, this.proxyRevalidate, staleWhileRevalidate,
				this.staleIfError, this.sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "stale-if-error" directive.
	 * <p>This directive indicates that when an error is encountered, a cached stale response
	 * MAY be used to satisfy the request, regardless of other freshness information.
	 * @param staleIfError the maximum time the response should be used when errors are encountered
	 * @param unit the time unit of the {@code staleIfError} argument
	 * @return a new CacheControl instance with an additional "stale-if-error" directive
	 * @see #staleIfError(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 section 4</a>
	 */
	public CacheControl staleIfError(long staleIfError, TimeUnit unit) {
		return staleIfError(Duration.ofSeconds(unit.toSeconds(staleIfError)));
	}

	/**
	 * Return a new instance with an additional "stale-if-error" directive.
	 * <p>This directive indicates that when an error is encountered, a cached stale response
	 * MAY be used to satisfy the request, regardless of other freshness information.
	 * @param staleIfError the maximum time the response should be used when errors are encountered
	 * @return a new CacheControl instance with an additional "stale-if-error" directive
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 section 4</a>
	 */
	public CacheControl staleIfError(Duration staleIfError) {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, this.noTransform,
				this.cachePublic, this.cachePrivate, this.proxyRevalidate, this.staleWhileRevalidate,
				staleIfError, this.sMaxAge, this.immutable);
	}

	/**
	 * Return a new instance with an additional "immutable" directive.
	 * <p>This directive indicates that the origin server will not update the
	 * representation of that resource during the freshness lifetime of the response.
	 * Adding a {@link #maxAge(Duration) max-age} directive is strongly advised
	 * to enforce the actual freshness lifetime.
	 * @return a new CacheControl instance with an additional "immutable" directive
	 * @since 6.0.5
	 * @see <a href="https://tools.ietf.org/html/rfc8246">rfc8246</a>
	 */
	public CacheControl immutable() {
		return new CacheControl(this.maxAge, this.noCache, this.noStore, this.mustRevalidate, this.noTransform,
				this.cachePublic, this.cachePrivate, this.proxyRevalidate, this.staleWhileRevalidate,
				this.staleIfError, this.sMaxAge, true);
	}

	/**
	 * Return the "Cache-Control" header value, if any.
	 * @return the header value, or {@code null} if no directive was added
	 */
	@Nullable
	public String getHeaderValue() {
		String headerValue = toHeaderValue();
		return (StringUtils.hasText(headerValue) ? headerValue : null);
	}

	/**
	 * Return the "Cache-Control" header value.
	 * @return the header value (potentially empty)
	 */
	private String toHeaderValue() {
		StringBuilder headerValue = new StringBuilder();
		if (this.maxAge != null) {
			appendDirective(headerValue, "max-age=" + this.maxAge.getSeconds());
		}
		if (this.noCache) {
			appendDirective(headerValue, "no-cache");
		}
		if (this.noStore) {
			appendDirective(headerValue, "no-store");
		}
		if (this.mustRevalidate) {
			appendDirective(headerValue, "must-revalidate");
		}
		if (this.noTransform) {
			appendDirective(headerValue, "no-transform");
		}
		if (this.cachePublic) {
			appendDirective(headerValue, "public");
		}
		if (this.cachePrivate) {
			appendDirective(headerValue, "private");
		}
		if (this.proxyRevalidate) {
			appendDirective(headerValue, "proxy-revalidate");
		}
		if (this.sMaxAge != null) {
			appendDirective(headerValue, "s-maxage=" + this.sMaxAge.getSeconds());
		}
		if (this.staleIfError != null) {
			appendDirective(headerValue, "stale-if-error=" + this.staleIfError.getSeconds());
		}
		if (this.staleWhileRevalidate != null) {
			appendDirective(headerValue, "stale-while-revalidate=" + this.staleWhileRevalidate.getSeconds());
		}
		if (this.immutable) {
			appendDirective(headerValue, "immutable");
		}
		return headerValue.toString();
	}

	private void appendDirective(StringBuilder builder, String value) {
		if (builder.length() > 0) {
			builder.append(", ");
		}
		builder.append(value);
	}


	@Override
	public String toString() {
		return "CacheControl [" + toHeaderValue() + "]";
	}

}
