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

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A data structure representing HTTP request or response headers, mapping String header names
 * to a list of String values, also offering accessors for common application-level data types.
 *
 * <p>In addition to the regular methods defined by {@link Map}, this class offers many common
 * convenience methods, for example:
 * <ul>
 * <li>{@link #getFirst(String)} returns the first value associated with a given header name</li>
 * <li>{@link #add(String, String)} adds a header value to the list of values for a header name</li>
 * <li>{@link #set(String, String)} sets the header value to a single string value</li>
 * </ul>
 *
 * <p>Note that {@code HttpHeaders} instances created by the default constructor
 * treat header names in a case-insensitive manner. Instances created with the
 * {@link #HttpHeaders(MultiValueMap)} constructor like those instantiated
 * internally by the framework to adapt to existing HTTP headers data structures
 * do guarantee per-header get/set/add operations to be case-insensitive as
 * mandated by the HTTP specification. However, it is not necessarily how
 * entries are actually stored, and this can lead to the reported {@code size()}
 * being inflated. Prefer using {@link #headerSet()} or {@link #headerNames()}
 * to ensure a case-insensitive view.
 *
 * <p>This class is meant to reference "well-known" headers supported by Spring
 * Framework. If your application or library relies on other headers defined in RFCs,
 * please use methods that accept the header name as a parameter.
 *
 * <p>Since 7.0, this class no longer implements the {@code MultiValueMap}
 * contract.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Josh Long
 * @author Sam Brannen
 * @author Simon Baslé
 * @since 3.0
 */
public class HttpHeaders implements Serializable {

	private static final long serialVersionUID = -8578554704772377436L;


	/**
	 * The HTTP {@code Accept} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">Section 5.3.2 of RFC 7231</a>
	 */
	public static final String ACCEPT = "Accept";
	/**
	 * The HTTP {@code Accept-Charset} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.3">Section 5.3.3 of RFC 7231</a>
	 */
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	/**
	 * The HTTP {@code Accept-Encoding} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.4">Section 5.3.4 of RFC 7231</a>
	 */
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	/**
	 * The HTTP {@code Accept-Language} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">Section 5.3.5 of RFC 7231</a>
	 */
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	/**
	 * The HTTP {@code Accept-Patch} header field name.
	 * @since 5.3.6
	 * @see <a href="https://tools.ietf.org/html/rfc5789#section-3.1">Section 3.1 of RFC 5789</a>
	 */
	public static final String ACCEPT_PATCH = "Accept-Patch";
	/**
	 * The HTTP {@code Accept-Ranges} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.3">Section 5.3.5 of RFC 7233</a>
	 */
	public static final String ACCEPT_RANGES = "Accept-Ranges";
	/**
	 * The CORS {@code Access-Control-Allow-Credentials} response header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	/**
	 * The CORS {@code Access-Control-Allow-Headers} response header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	/**
	 * The CORS {@code Access-Control-Allow-Methods} response header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	/**
	 * The CORS {@code Access-Control-Allow-Origin} response header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	/**
	 * The CORS {@code Access-Control-Expose-Headers} response header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	/**
	 * The CORS {@code Access-Control-Max-Age} response header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	/**
	 * The CORS {@code Access-Control-Request-Headers} request header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	/**
	 * The CORS {@code Access-Control-Request-Method} request header field name.
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	/**
	 * The HTTP {@code Age} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.1">Section 5.1 of RFC 7234</a>
	 */
	public static final String AGE = "Age";
	/**
	 * The HTTP {@code Allow} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.1">Section 7.4.1 of RFC 7231</a>
	 */
	public static final String ALLOW = "Allow";
	/**
	 * The HTTP {@code Authorization} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">Section 4.2 of RFC 7235</a>
	 */
	public static final String AUTHORIZATION = "Authorization";
	/**
	 * The HTTP {@code Cache-Control} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">Section 5.2 of RFC 7234</a>
	 */
	public static final String CACHE_CONTROL = "Cache-Control";
	/**
	 * The HTTP {@code Connection} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.1">Section 6.1 of RFC 7230</a>
	 */
	public static final String CONNECTION = "Connection";
	/**
	 * The HTTP {@code Content-Encoding} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.2.2">Section 3.1.2.2 of RFC 7231</a>
	 */
	public static final String CONTENT_ENCODING = "Content-Encoding";
	/**
	 * The HTTP {@code Content-Disposition} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
	 */
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	/**
	 * The HTTP {@code Content-Language} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.3.2">Section 3.1.3.2 of RFC 7231</a>
	 */
	public static final String CONTENT_LANGUAGE = "Content-Language";
	/**
	 * The HTTP {@code Content-Length} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">Section 3.3.2 of RFC 7230</a>
	 */
	public static final String CONTENT_LENGTH = "Content-Length";
	/**
	 * The HTTP {@code Content-Location} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.4.2">Section 3.1.4.2 of RFC 7231</a>
	 */
	public static final String CONTENT_LOCATION = "Content-Location";
	/**
	 * The HTTP {@code Content-Range} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.2">Section 4.2 of RFC 7233</a>
	 */
	public static final String CONTENT_RANGE = "Content-Range";
	/**
	 * The HTTP {@code Content-Type} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
	 */
	public static final String CONTENT_TYPE = "Content-Type";
	/**
	 * The HTTP {@code Cookie} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.3.4">Section 4.3.4 of RFC 2109</a>
	 */
	public static final String COOKIE = "Cookie";
	/**
	 * The HTTP {@code Date} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">Section 7.1.1.2 of RFC 7231</a>
	 */
	public static final String DATE = "Date";
	/**
	 * The HTTP {@code ETag} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
	 */
	public static final String ETAG = "ETag";
	/**
	 * The HTTP {@code Expect} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.1">Section 5.1.1 of RFC 7231</a>
	 */
	public static final String EXPECT = "Expect";
	/**
	 * The HTTP {@code Expires} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.3">Section 5.3 of RFC 7234</a>
	 */
	public static final String EXPIRES = "Expires";
	/**
	 * The HTTP {@code From} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.1">Section 5.5.1 of RFC 7231</a>
	 */
	public static final String FROM = "From";
	/**
	 * The HTTP {@code Host} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.4">Section 5.4 of RFC 7230</a>
	 */
	public static final String HOST = "Host";
	/**
	 * The HTTP {@code If-Match} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.1">Section 3.1 of RFC 7232</a>
	 */
	public static final String IF_MATCH = "If-Match";
	/**
	 * The HTTP {@code If-Modified-Since} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.3">Section 3.3 of RFC 7232</a>
	 */
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	/**
	 * The HTTP {@code If-None-Match} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.2">Section 3.2 of RFC 7232</a>
	 */
	public static final String IF_NONE_MATCH = "If-None-Match";
	/**
	 * The HTTP {@code If-Range} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.2">Section 3.2 of RFC 7233</a>
	 */
	public static final String IF_RANGE = "If-Range";
	/**
	 * The HTTP {@code If-Unmodified-Since} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.4">Section 3.4 of RFC 7232</a>
	 */
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	/**
	 * The HTTP {@code Last-Modified} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.2">Section 2.2 of RFC 7232</a>
	 */
	public static final String LAST_MODIFIED = "Last-Modified";
	/**
	 * The HTTP {@code Link} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 */
	public static final String LINK = "Link";
	/**
	 * The HTTP {@code Location} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">Section 7.1.2 of RFC 7231</a>
	 */
	public static final String LOCATION = "Location";
	/**
	 * The HTTP {@code Max-Forwards} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.2">Section 5.1.2 of RFC 7231</a>
	 */
	public static final String MAX_FORWARDS = "Max-Forwards";
	/**
	 * The HTTP {@code Origin} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
	 */
	public static final String ORIGIN = "Origin";
	/**
	 * The HTTP {@code Pragma} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.4">Section 5.4 of RFC 7234</a>
	 */
	public static final String PRAGMA = "Pragma";
	/**
	 * The HTTP {@code Proxy-Authenticate} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.3">Section 4.3 of RFC 7235</a>
	 */
	public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
	/**
	 * The HTTP {@code Proxy-Authorization} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.4">Section 4.4 of RFC 7235</a>
	 */
	public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
	/**
	 * The HTTP {@code Range} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.1">Section 3.1 of RFC 7233</a>
	 */
	public static final String RANGE = "Range";
	/**
	 * The HTTP {@code Referer} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.2">Section 5.5.2 of RFC 7231</a>
	 */
	public static final String REFERER = "Referer";
	/**
	 * The HTTP {@code Retry-After} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">Section 7.1.3 of RFC 7231</a>
	 */
	public static final String RETRY_AFTER = "Retry-After";
	/**
	 * The HTTP {@code Server} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">Section 7.4.2 of RFC 7231</a>
	 */
	public static final String SERVER = "Server";
	/**
	 * The HTTP {@code Set-Cookie} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.2.2">Section 4.2.2 of RFC 2109</a>
	 */
	public static final String SET_COOKIE = "Set-Cookie";
	/**
	 * The HTTP {@code Set-Cookie2} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc2965">RFC 2965</a>
	 */
	public static final String SET_COOKIE2 = "Set-Cookie2";
	/**
	 * The HTTP {@code TE} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.3">Section 4.3 of RFC 7230</a>
	 */
	public static final String TE = "TE";
	/**
	 * The HTTP {@code Trailer} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.4">Section 4.4 of RFC 7230</a>
	 */
	public static final String TRAILER = "Trailer";
	/**
	 * The HTTP {@code Transfer-Encoding} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">Section 3.3.1 of RFC 7230</a>
	 */
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	/**
	 * The HTTP {@code Upgrade} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.7">Section 6.7 of RFC 7230</a>
	 */
	public static final String UPGRADE = "Upgrade";
	/**
	 * The HTTP {@code User-Agent} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.3">Section 5.5.3 of RFC 7231</a>
	 */
	public static final String USER_AGENT = "User-Agent";
	/**
	 * The HTTP {@code Vary} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.4">Section 7.1.4 of RFC 7231</a>
	 */
	public static final String VARY = "Vary";
	/**
	 * The HTTP {@code Via} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.7.1">Section 5.7.1 of RFC 7230</a>
	 */
	public static final String VIA = "Via";
	/**
	 * The HTTP {@code Warning} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.5">Section 5.5 of RFC 7234</a>
	 */
	public static final String WARNING = "Warning";
	/**
	 * The HTTP {@code WWW-Authenticate} header field name.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.1">Section 4.1 of RFC 7235</a>
	 */
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";


	/**
	 * An empty {@code HttpHeaders} instance (immutable).
	 * @since 5.0
	 */
	public static final HttpHeaders EMPTY = new ReadOnlyHttpHeaders(new LinkedMultiValueMap<>());

	private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ROOT);

	private static final ZoneId GMT = ZoneId.of("GMT");

	/**
	 * Date formats with time zone as specified in the HTTP RFC to use for formatting.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(GMT);

	/**
	 * Date formats with time zone as specified in the HTTP RFC to use for parsing.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final DateTimeFormatter[] DATE_PARSERS = new DateTimeFormatter[] {
			DateTimeFormatter.RFC_1123_DATE_TIME,
			DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
			DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.US).withZone(GMT)
	};


	@SuppressWarnings("serial")
	final MultiValueMap<String, String> headers;


	/**
	 * Construct a new, empty instance of the {@code HttpHeaders} object
	 * using an underlying case-insensitive map.
	 */
	public HttpHeaders() {
		this(CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ROOT)));
	}

	/**
	 * Construct a new {@code HttpHeaders} instance backed by an existing map.
	 * <p>This constructor is available as an optimization for adapting to existing
	 * headers map structures, primarily for internal use within the framework.
	 * @param headers the headers map (expected to operate with case-insensitive keys)
	 * @since 5.1
	 */
	public HttpHeaders(MultiValueMap<String, String> headers) {
		Assert.notNull(headers, "MultiValueMap must not be null");
		if (headers == EMPTY) {
			this.headers = CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH));
		}
		else if (headers instanceof HttpHeaders httpHeaders) {
			while (httpHeaders.headers instanceof HttpHeaders wrapped) {
				httpHeaders = wrapped;
			}
			this.headers = httpHeaders.headers;
		}
		else {
			this.headers = headers;
		}
	}

	/**
	 * Construct a new {@code HttpHeaders} instance by removing any read-only
	 * wrapper that may have been previously applied around the given
	 * {@code HttpHeaders} via {@link #readOnlyHttpHeaders(HttpHeaders)}.
	 * <p>Once the writable instance is mutated, the read-only instance is
	 * likely to be out of sync and should be discarded.
	 * @param httpHeaders the headers to expose
	 * @since 7.0
	 */
	public HttpHeaders(HttpHeaders httpHeaders) {
		Assert.notNull(httpHeaders, "HttpHeaders must not be null");
		if (httpHeaders == EMPTY) {
			this.headers = CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH));
		}
		else {
			while (httpHeaders.headers instanceof HttpHeaders wrapped) {
				httpHeaders = wrapped;
			}
			this.headers = httpHeaders.headers;
		}
	}


	/**
	 * Get the list of header values for the given header name, if any.
	 * @param headerName the header name
	 * @return the list of header values, or an empty list
	 * @since 7.0
	 */
	public List<String> getOrEmpty(String headerName) {
		return getOrDefault(headerName, Collections.emptyList());
	}

	/**
	 * Get the list of header values for the given header name, or the given
	 * default list of values if the header is not present.
	 * @param headerName the header name
	 * @param defaultValue the fallback list if header is not present
	 * @return the list of header values, or a default list of values
	 * @since 7.0
	 */
	public List<String> getOrDefault(String headerName, List<String> defaultValue) {
		List<String> values = get(headerName);
		return (values != null ? values : defaultValue);
	}

	/**
	 * Set the list of acceptable {@linkplain MediaType media types},
	 * as specified by the {@code Accept} header.
	 */
	public void setAccept(List<MediaType> acceptableMediaTypes) {
		set(ACCEPT, MediaType.toString(acceptableMediaTypes));
	}

	/**
	 * Return the list of acceptable {@linkplain MediaType media types},
	 * as specified by the {@code Accept} header.
	 * <p>Returns an empty list when the acceptable media types are unspecified.
	 */
	public List<MediaType> getAccept() {
		return MediaType.parseMediaTypes(get(ACCEPT));
	}

	/**
	 * Set the acceptable language ranges, as specified by the
	 * {@literal Accept-Language} header.
	 * @since 5.0
	 */
	public void setAcceptLanguage(List<Locale.LanguageRange> languages) {
		Assert.notNull(languages, "LanguageRange List must not be null");
		DecimalFormat decimal = new DecimalFormat("0.0", DECIMAL_FORMAT_SYMBOLS);
		List<String> values = languages.stream()
				.map(range ->
						range.getWeight() == Locale.LanguageRange.MAX_WEIGHT ?
								range.getRange() :
								range.getRange() + ";q=" + decimal.format(range.getWeight()))
				.toList();
		set(ACCEPT_LANGUAGE, toCommaDelimitedString(values));
	}

	/**
	 * Return the language ranges from the {@literal "Accept-Language"} header.
	 * <p>If you only need sorted, preferred locales only use
	 * {@link #getAcceptLanguageAsLocales()} or if you need to filter based on
	 * a list of supported locales you can pass the returned list to
	 * {@link Locale#filter(List, Collection)}.
	 * @throws IllegalArgumentException if the value cannot be converted to a language range
	 * @since 5.0
	 */
	public List<Locale.LanguageRange> getAcceptLanguage() {
		String value = getFirst(ACCEPT_LANGUAGE);
		if (StringUtils.hasText(value)) {
			try {
				return Locale.LanguageRange.parse(value);
			}
			catch (IllegalArgumentException ignored) {
				String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
				for (int i = 0; i < tokens.length; i++) {
					tokens[i] = StringUtils.trimTrailingCharacter(tokens[i], ';');
				}
				value = StringUtils.arrayToCommaDelimitedString(tokens);
				return Locale.LanguageRange.parse(value);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Variant of {@link #setAcceptLanguage(List)} using {@link Locale}'s.
	 * @since 5.0
	 */
	public void setAcceptLanguageAsLocales(List<Locale> locales) {
		setAcceptLanguage(locales.stream()
				.map(locale -> new Locale.LanguageRange(locale.toLanguageTag()))
				.toList());
	}

	/**
	 * A variant of {@link #getAcceptLanguage()} that converts each
	 * {@link java.util.Locale.LanguageRange} to a {@link Locale}.
	 * @return the locales or an empty list
	 * @throws IllegalArgumentException if the value cannot be converted to a locale
	 * @since 5.0
	 */
	public List<Locale> getAcceptLanguageAsLocales() {
		List<Locale.LanguageRange> ranges = getAcceptLanguage();
		if (ranges.isEmpty()) {
			return Collections.emptyList();
		}

		List<Locale> locales = new ArrayList<>(ranges.size());
		for (Locale.LanguageRange range : ranges) {
			if (!range.getRange().startsWith("*")) {
				locales.add(Locale.forLanguageTag(range.getRange()));
			}
		}
		return locales;
	}

	/**
	 * Set the list of acceptable {@linkplain MediaType media types} for
	 * {@code PATCH} methods, as specified by the {@code Accept-Patch} header.
	 * @since 5.3.6
	 */
	public void setAcceptPatch(List<MediaType> mediaTypes) {
		set(ACCEPT_PATCH, MediaType.toString(mediaTypes));
	}

	/**
	 * Return the list of acceptable {@linkplain MediaType media types} for
	 * {@code PATCH} methods, as specified by the {@code Accept-Patch} header.
	 * <p>Returns an empty list when the acceptable media types are unspecified.
	 * @since 5.3.6
	 */
	public List<MediaType> getAcceptPatch() {
		return MediaType.parseMediaTypes(get(ACCEPT_PATCH));
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Allow-Credentials} response header.
	 */
	public void setAccessControlAllowCredentials(boolean allowCredentials) {
		set(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
	}

	/**
	 * Return the value of the {@code Access-Control-Allow-Credentials} response header.
	 */
	public boolean getAccessControlAllowCredentials() {
		return Boolean.parseBoolean(getFirst(ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Allow-Headers} response header.
	 */
	public void setAccessControlAllowHeaders(List<String> allowedHeaders) {
		set(ACCESS_CONTROL_ALLOW_HEADERS, toCommaDelimitedString(allowedHeaders));
	}

	/**
	 * Return the value of the {@code Access-Control-Allow-Headers} response header.
	 */
	public List<String> getAccessControlAllowHeaders() {
		return getValuesAsList(ACCESS_CONTROL_ALLOW_HEADERS);
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Allow-Methods} response header.
	 */
	public void setAccessControlAllowMethods(List<HttpMethod> allowedMethods) {
		set(ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * Return the value of the {@code Access-Control-Allow-Methods} response header.
	 */
	public List<HttpMethod> getAccessControlAllowMethods() {
		String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
		if (value != null) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			List<HttpMethod> result = new ArrayList<>(tokens.length);
			for (String token : tokens) {
				HttpMethod method = HttpMethod.valueOf(token);
				result.add(method);
			}
			return result;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Allow-Origin} response header.
	 */
	public void setAccessControlAllowOrigin(@Nullable String allowedOrigin) {
		setOrRemove(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
	}

	/**
	 * Return the value of the {@code Access-Control-Allow-Origin} response header.
	 */
	public @Nullable String getAccessControlAllowOrigin() {
		return getFieldValues(ACCESS_CONTROL_ALLOW_ORIGIN);
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Expose-Headers} response header.
	 */
	public void setAccessControlExposeHeaders(List<String> exposedHeaders) {
		set(ACCESS_CONTROL_EXPOSE_HEADERS, toCommaDelimitedString(exposedHeaders));
	}

	/**
	 * Return the value of the {@code Access-Control-Expose-Headers} response header.
	 */
	public List<String> getAccessControlExposeHeaders() {
		return getValuesAsList(ACCESS_CONTROL_EXPOSE_HEADERS);
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Max-Age} response header.
	 * @since 5.2
	 */
	public void setAccessControlMaxAge(Duration maxAge) {
		set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge.getSeconds()));
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Max-Age} response header.
	 */
	public void setAccessControlMaxAge(long maxAge) {
		set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
	}

	/**
	 * Return the value of the {@code Access-Control-Max-Age} response header.
	 * <p>Returns -1 when the max age is unknown.
	 */
	public long getAccessControlMaxAge() {
		String value = getFirst(ACCESS_CONTROL_MAX_AGE);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Request-Headers} request header.
	 */
	public void setAccessControlRequestHeaders(List<String> requestHeaders) {
		set(ACCESS_CONTROL_REQUEST_HEADERS, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * Return the value of the {@code Access-Control-Request-Headers} request header.
	 */
	public List<String> getAccessControlRequestHeaders() {
		return getValuesAsList(ACCESS_CONTROL_REQUEST_HEADERS);
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Request-Method} request header.
	 */
	public void setAccessControlRequestMethod(@Nullable HttpMethod requestMethod) {
		setOrRemove(ACCESS_CONTROL_REQUEST_METHOD, (requestMethod != null ? requestMethod.name() : null));
	}

	/**
	 * Return the value of the {@code Access-Control-Request-Method} request header.
	 */
	public @Nullable HttpMethod getAccessControlRequestMethod() {
		String requestMethod = getFirst(ACCESS_CONTROL_REQUEST_METHOD);
		if (requestMethod != null) {
			return HttpMethod.valueOf(requestMethod);
		}
		else {
			return null;
		}
	}

	/**
	 * Set the list of acceptable {@linkplain Charset charsets},
	 * as specified by the {@code Accept-Charset} header.
	 */
	public void setAcceptCharset(List<Charset> acceptableCharsets) {
		StringJoiner joiner = new StringJoiner(", ");
		for (Charset charset : acceptableCharsets) {
			joiner.add(charset.name().toLowerCase(Locale.ROOT));
		}
		set(ACCEPT_CHARSET, joiner.toString());
	}

	/**
	 * Return the list of acceptable {@linkplain Charset charsets},
	 * as specified by the {@code Accept-Charset} header.
	 */
	public List<Charset> getAcceptCharset() {
		String value = getFirst(ACCEPT_CHARSET);
		if (value != null) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			List<Charset> result = new ArrayList<>(tokens.length);
			for (String token : tokens) {
				int paramIdx = token.indexOf(';');
				String charsetName;
				if (paramIdx == -1) {
					charsetName = token;
				}
				else {
					charsetName = token.substring(0, paramIdx);
				}
				if (!charsetName.equals("*")) {
					result.add(Charset.forName(charsetName));
				}
			}
			return result;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Set the set of allowed {@link HttpMethod HTTP methods},
	 * as specified by the {@code Allow} header.
	 */
	public void setAllow(Set<HttpMethod> allowedMethods) {
		set(ALLOW, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * Return the set of allowed {@link HttpMethod HTTP methods},
	 * as specified by the {@code Allow} header.
	 * <p>Returns an empty set when the allowed methods are unspecified.
	 */
	public Set<HttpMethod> getAllow() {
		String value = getFirst(ALLOW);
		if (StringUtils.hasLength(value)) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			Set<HttpMethod> result = CollectionUtils.newLinkedHashSet(tokens.length);
			for (String token : tokens) {
				HttpMethod method = HttpMethod.valueOf(token);
				result.add(method);
			}
			return result;
		}
		else {
			return Collections.emptySet();
		}
	}

	/**
	 * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
	 * Basic Authentication based on the given username and password.
	 * <p>Note that this method only supports characters in the
	 * {@link StandardCharsets#ISO_8859_1 ISO-8859-1} character set.
	 * @param username the username
	 * @param password the password
	 * @throws IllegalArgumentException if either {@code user} or
	 * {@code password} contain characters that cannot be encoded to ISO-8859-1
	 * @since 5.1
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public void setBasicAuth(String username, String password) {
		setBasicAuth(username, password, null);
	}

	/**
	 * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
	 * Basic Authentication based on the given username and password.
	 * @param username the username
	 * @param password the password
	 * @param charset the charset to use to convert the credentials into an octet
	 * sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
	 * @throws IllegalArgumentException if {@code username} or {@code password}
	 * contains characters that cannot be encoded to the given charset
	 * @since 5.1
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public void setBasicAuth(String username, String password, @Nullable Charset charset) {
		setBasicAuth(encodeBasicAuth(username, password, charset));
	}

	/**
	 * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
	 * Basic Authentication based on the given {@linkplain #encodeBasicAuth
	 * encoded credentials}.
	 * <p>Favor this method over {@link #setBasicAuth(String, String)} and
	 * {@link #setBasicAuth(String, String, Charset)} if you wish to cache the
	 * encoded credentials.
	 * @param encodedCredentials the encoded credentials
	 * @throws IllegalArgumentException if supplied credentials string is
	 * {@code null} or blank
	 * @since 5.2
	 * @see #setBasicAuth(String, String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public void setBasicAuth(String encodedCredentials) {
		Assert.hasText(encodedCredentials, "'encodedCredentials' must not be null or blank");
		set(AUTHORIZATION, "Basic " + encodedCredentials);
	}

	/**
	 * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
	 * the given Bearer token.
	 * @param token the Base64 encoded token
	 * @since 5.1
	 * @see <a href="https://tools.ietf.org/html/rfc6750">RFC 6750</a>
	 */
	public void setBearerAuth(String token) {
		set(AUTHORIZATION, "Bearer " + token);
	}

	/**
	 * Set a configured {@link CacheControl} instance as the
	 * new value of the {@code Cache-Control} header.
	 * @since 5.0.5
	 */
	public void setCacheControl(CacheControl cacheControl) {
		setOrRemove(CACHE_CONTROL, cacheControl.getHeaderValue());
	}

	/**
	 * Set the (new) value of the {@code Cache-Control} header.
	 */
	public void setCacheControl(@Nullable String cacheControl) {
		setOrRemove(CACHE_CONTROL, cacheControl);
	}

	/**
	 * Return the value of the {@code Cache-Control} header.
	 */
	public @Nullable String getCacheControl() {
		return getFieldValues(CACHE_CONTROL);
	}

	/**
	 * Set the (new) value of the {@code Connection} header.
	 */
	public void setConnection(String connection) {
		set(CONNECTION, connection);
	}

	/**
	 * Set the (new) value of the {@code Connection} header.
	 */
	public void setConnection(List<String> connection) {
		set(CONNECTION, toCommaDelimitedString(connection));
	}

	/**
	 * Return the value of the {@code Connection} header.
	 */
	public List<String> getConnection() {
		return getValuesAsList(CONNECTION);
	}

	/**
	 * Set the {@code Content-Disposition} header when creating a
	 * {@code "multipart/form-data"} request.
	 * <p>Applications typically would not set this header directly but
	 * rather prepare a {@code MultiValueMap<String, Object>}, containing an
	 * Object or a {@link org.springframework.core.io.Resource} for each part,
	 * and then pass that to the {@code RestTemplate} or {@code WebClient}.
	 * @param name the control name
	 * @param filename the filename (may be {@code null})
	 * @see #getContentDisposition()
	 */
	public void setContentDispositionFormData(String name, @Nullable String filename) {
		Assert.notNull(name, "Name must not be null");
		ContentDisposition.Builder disposition = ContentDisposition.formData().name(name);
		if (StringUtils.hasText(filename)) {
			disposition.filename(filename);
		}
		setContentDisposition(disposition.build());
	}

	/**
	 * Set the {@literal Content-Disposition} header.
	 * <p>This could be used on a response to indicate if the content is
	 * expected to be displayed inline in the browser or as an attachment to be
	 * saved locally.
	 * <p>It can also be used for a {@code "multipart/form-data"} request.
	 * For more details see notes on {@link #setContentDispositionFormData}.
	 * @since 5.0
	 * @see #getContentDisposition()
	 */
	public void setContentDisposition(ContentDisposition contentDisposition) {
		set(CONTENT_DISPOSITION, contentDisposition.toString());
	}

	/**
	 * Return a parsed representation of the {@literal Content-Disposition} header.
	 * @since 5.0
	 * @see #setContentDisposition(ContentDisposition)
	 */
	public ContentDisposition getContentDisposition() {
		String contentDisposition = getFirst(CONTENT_DISPOSITION);
		if (StringUtils.hasText(contentDisposition)) {
			return ContentDisposition.parse(contentDisposition);
		}
		return ContentDisposition.empty();
	}

	/**
	 * Set the {@link Locale} of the content language,
	 * as specified by the {@literal Content-Language} header.
	 * <p>Use {@code put(CONTENT_LANGUAGE, list)} if you need
	 * to set multiple content languages.</p>
	 * @since 5.0
	 */
	public void setContentLanguage(@Nullable Locale locale) {
		setOrRemove(CONTENT_LANGUAGE, (locale != null ? locale.toLanguageTag() : null));
	}

	/**
	 * Get the first {@link Locale} of the content languages, as specified by the
	 * {@code Content-Language} header.
	 * <p>Use {@link #getValuesAsList(String)} if you need to get multiple content
	 * languages.
	 * @return the first {@code Locale} of the content languages, or {@code null}
	 * if unknown
	 * @since 5.0
	 */
	public @Nullable Locale getContentLanguage() {
		return getValuesAsList(CONTENT_LANGUAGE)
				.stream()
				.findFirst()
				.map(Locale::forLanguageTag)
				.orElse(null);
	}

	/**
	 * Set the length of the body in bytes, as specified by the
	 * {@code Content-Length} header.
	 * @param contentLength content length (greater than or equal to zero)
	 * @throws IllegalArgumentException if the content length is negative
	 */
	public void setContentLength(long contentLength) {
		if (contentLength < 0) {
			throw new IllegalArgumentException("Content-Length must be a non-negative number");
		}
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * Return the length of the body in bytes, as specified by the
	 * {@code Content-Length} header.
	 * <p>Returns -1 when the content-length is unknown.
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * Set the {@linkplain MediaType media type} of the body,
	 * as specified by the {@code Content-Type} header.
	 */
	public void setContentType(@Nullable MediaType mediaType) {
		if (mediaType != null) {
			Assert.isTrue(!mediaType.isWildcardType(), "Content-Type cannot contain wildcard type '*'");
			Assert.isTrue(!mediaType.isWildcardSubtype(), "Content-Type cannot contain wildcard subtype '*'");
			set(CONTENT_TYPE, mediaType.toString());
		}
		else {
			remove(CONTENT_TYPE);
		}
	}

	/**
	 * Return the {@linkplain MediaType media type} of the body, as specified
	 * by the {@code Content-Type} header.
	 * <p>Returns {@code null} when the {@code Content-Type} header is not set.
	 * @throws InvalidMediaTypeException if the media type value cannot be parsed
	 */
	public @Nullable MediaType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
	}

	/**
	 * Set the date and time at which the message was created, as specified
	 * by the {@code Date} header.
	 * @since 5.2
	 */
	public void setDate(ZonedDateTime date) {
		setZonedDateTime(DATE, date);
	}

	/**
	 * Set the date and time at which the message was created, as specified
	 * by the {@code Date} header.
	 * @since 5.2
	 */
	public void setDate(Instant date) {
		setInstant(DATE, date);
	}

	/**
	 * Set the date and time at which the message was created, as specified
	 * by the {@code Date} header.
	 * <p>The date should be specified as the number of milliseconds since
	 * January 1, 1970 GMT.
	 */
	public void setDate(long date) {
		setDate(DATE, date);
	}

	/**
	 * Return the date and time at which the message was created, as specified
	 * by the {@code Date} header.
	 * <p>The date is returned as the number of milliseconds since
	 * January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @throws IllegalArgumentException if the value cannot be converted to a date
	 */
	public long getDate() {
		return getFirstDate(DATE);
	}

	/**
	 * Set the (new) entity tag of the body, as specified by the {@code ETag} header.
	 */
	public void setETag(@Nullable String tag) {
		if (tag != null) {
			set(ETAG, ETag.quoteETagIfNecessary(tag));
		}
		else {
			remove(ETAG);
		}
	}

	/**
	 * Return the entity tag of the body, as specified by the {@code ETag} header.
	 */
	public @Nullable String getETag() {
		return getFirst(ETAG);
	}

	/**
	 * Set the duration after which the message is no longer valid,
	 * as specified by the {@code Expires} header.
	 * @since 5.0.5
	 */
	public void setExpires(ZonedDateTime expires) {
		setZonedDateTime(EXPIRES, expires);
	}

	/**
	 * Set the date and time at which the message is no longer valid,
	 * as specified by the {@code Expires} header.
	 * @since 5.2
	 */
	public void setExpires(Instant expires) {
		setInstant(EXPIRES, expires);
	}

	/**
	 * Set the date and time at which the message is no longer valid,
	 * as specified by the {@code Expires} header.
	 * <p>The date should be specified as the number of milliseconds since
	 * January 1, 1970 GMT.
	 */
	public void setExpires(long expires) {
		setDate(EXPIRES, expires);
	}

	/**
	 * Return the date and time at which the message is no longer valid,
	 * as specified by the {@code Expires} header.
	 * <p>The date is returned as the number of milliseconds since
	 * January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getExpires() {
		return getFirstDate(EXPIRES, false);
	}

	/**
	 * Set the (new) value of the {@code Host} header.
	 * <p>If the given {@linkplain InetSocketAddress#getPort() port} is {@code 0},
	 * the host header will only contain the
	 * {@linkplain InetSocketAddress#getHostString() host name}.
	 * @since 5.0
	 */
	public void setHost(@Nullable InetSocketAddress host) {
		if (host != null) {
			String value = host.getHostString();
			int port = host.getPort();
			if (port != 0) {
				value = value + ":" + port;
			}
			set(HOST, value);
		}
		else {
			remove(HOST);
		}
	}

	/**
	 * Return the value of the {@code Host} header, if available.
	 * <p>If the header value does not contain a port, the
	 * {@linkplain InetSocketAddress#getPort() port} in the returned address will
	 * be {@code 0}.
	 * @since 5.0
	 */
	public @Nullable InetSocketAddress getHost() {
		String value = getFirst(HOST);
		if (value == null) {
			return null;
		}

		String host = null;
		int port = 0;
		int separator = (value.startsWith("[") ? value.indexOf(':', value.indexOf(']')) : value.lastIndexOf(':'));
		if (separator != -1) {
			host = value.substring(0, separator);
			String portString = value.substring(separator + 1);
			try {
				port = Integer.parseInt(portString);
			}
			catch (NumberFormatException ex) {
				// ignore
			}
		}

		if (host == null) {
			host = value;
		}
		return InetSocketAddress.createUnresolved(host, port);
	}

	/**
	 * Set the (new) value of the {@code If-Match} header.
	 * @since 4.3
	 */
	public void setIfMatch(String ifMatch) {
		set(IF_MATCH, ifMatch);
	}

	/**
	 * Set the (new) value of the {@code If-Match} header.
	 * @since 4.3
	 */
	public void setIfMatch(List<String> ifMatchList) {
		set(IF_MATCH, toCommaDelimitedString(ifMatchList));
	}

	/**
	 * Return the value of the {@code If-Match} header.
	 * @throws IllegalArgumentException if parsing fails
	 * @since 4.3
	 */
	public List<String> getIfMatch() {
		return getETagValuesAsList(IF_MATCH);
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * @since 5.1.4
	 */
	public void setIfModifiedSince(ZonedDateTime ifModifiedSince) {
		setZonedDateTime(IF_MODIFIED_SINCE, ifModifiedSince.withZoneSameInstant(GMT));
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * @since 5.1.4
	 */
	public void setIfModifiedSince(Instant ifModifiedSince) {
		setInstant(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * Set the (new) value of the {@code If-Modified-Since} header.
	 * <p>The date should be specified as the number of milliseconds since
	 * January 1, 1970 GMT.
	 */
	public void setIfModifiedSince(long ifModifiedSince) {
		setDate(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * Return the value of the {@code If-Modified-Since} header.
	 * <p>The date is returned as the number of milliseconds since
	 * January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getIfModifiedSince() {
		return getFirstDate(IF_MODIFIED_SINCE, false);
	}

	/**
	 * Set the (new) value of the {@code If-None-Match} header.
	 */
	public void setIfNoneMatch(String ifNoneMatch) {
		set(IF_NONE_MATCH, ifNoneMatch);
	}

	/**
	 * Set the (new) values of the {@code If-None-Match} header.
	 */
	public void setIfNoneMatch(List<String> ifNoneMatchList) {
		set(IF_NONE_MATCH, toCommaDelimitedString(ifNoneMatchList));
	}

	/**
	 * Return the value of the {@code If-None-Match} header.
	 * @throws IllegalArgumentException if parsing fails
	 */
	public List<String> getIfNoneMatch() {
		return getETagValuesAsList(IF_NONE_MATCH);
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * @since 5.1.4
	 */
	public void setIfUnmodifiedSince(ZonedDateTime ifUnmodifiedSince) {
		setZonedDateTime(IF_UNMODIFIED_SINCE, ifUnmodifiedSince.withZoneSameInstant(GMT));
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * @since 5.1.4
	 */
	public void setIfUnmodifiedSince(Instant ifUnmodifiedSince) {
		setInstant(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
	}

	/**
	 * Set the (new) value of the {@code If-Unmodified-Since} header.
	 * <p>The date should be specified as the number of milliseconds since
	 * January 1, 1970 GMT.
	 * @since 4.3
	 */
	public void setIfUnmodifiedSince(long ifUnmodifiedSince) {
		setDate(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
	}

	/**
	 * Return the value of the {@code If-Unmodified-Since} header.
	 * <p>The date is returned as the number of milliseconds since
	 * January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @since 4.3
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getIfUnmodifiedSince() {
		return getFirstDate(IF_UNMODIFIED_SINCE, false);
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * @since 5.1.4
	 */
	public void setLastModified(ZonedDateTime lastModified) {
		setZonedDateTime(LAST_MODIFIED, lastModified.withZoneSameInstant(GMT));
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * @since 5.1.4
	 */
	public void setLastModified(Instant lastModified) {
		setInstant(LAST_MODIFIED, lastModified);
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * <p>The date should be specified as the number of milliseconds since
	 * January 1, 1970 GMT.
	 */
	public void setLastModified(long lastModified) {
		setDate(LAST_MODIFIED, lastModified);
	}

	/**
	 * Return the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * <p>The date is returned as the number of milliseconds since
	 * January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getLastModified() {
		return getFirstDate(LAST_MODIFIED, false);
	}

	/**
	 * Set the (new) location of a resource,
	 * as specified by the {@code Location} header.
	 */
	public void setLocation(@Nullable URI location) {
		setOrRemove(LOCATION, (location != null ? location.toASCIIString() : null));
	}

	/**
	 * Return the (new) location of a resource
	 * as specified by the {@code Location} header.
	 * <p>Returns {@code null} when the location is unknown.
	 */
	public @Nullable URI getLocation() {
		String value = getFirst(LOCATION);
		return (value != null ? URI.create(value) : null);
	}

	/**
	 * Set the (new) value of the {@code Origin} header.
	 */
	public void setOrigin(@Nullable String origin) {
		setOrRemove(ORIGIN, origin);
	}

	/**
	 * Return the value of the {@code Origin} header.
	 */
	public @Nullable String getOrigin() {
		return getFirst(ORIGIN);
	}

	/**
	 * Set the (new) value of the {@code Pragma} header.
	 */
	public void setPragma(@Nullable String pragma) {
		setOrRemove(PRAGMA, pragma);
	}

	/**
	 * Return the value of the {@code Pragma} header.
	 */
	public @Nullable String getPragma() {
		return getFirst(PRAGMA);
	}

	/**
	 * Sets the (new) value of the {@code Range} header.
	 */
	public void setRange(List<HttpRange> ranges) {
		String value = HttpRange.toString(ranges);
		set(RANGE, value);
	}

	/**
	 * Return the value of the {@code Range} header.
	 * <p>Returns an empty list when the range is unknown.
	 */
	public List<HttpRange> getRange() {
		String value = getFirst(RANGE);
		return HttpRange.parseRanges(value);
	}

	/**
	 * Set the (new) value of the {@code Upgrade} header.
	 */
	public void setUpgrade(@Nullable String upgrade) {
		setOrRemove(UPGRADE, upgrade);
	}

	/**
	 * Return the value of the {@code Upgrade} header.
	 */
	public @Nullable String getUpgrade() {
		return getFirst(UPGRADE);
	}

	/**
	 * Set the request header names (for example, "Accept-Language") for which the
	 * response is subject to content negotiation and variances based on the
	 * value of those request headers.
	 * @param requestHeaders the request header names
	 * @since 4.3
	 */
	public void setVary(List<String> requestHeaders) {
		set(VARY, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * Return the request header names subject to content negotiation.
	 * @since 4.3
	 */
	public List<String> getVary() {
		return getValuesAsList(VARY);
	}

	/**
	 * Set the given date under the given header name after formatting it as a string
	 * using the RFC-1123 date-time formatter. The equivalent of
	 * {@link #set(String, String)} but for date headers.
	 * @since 5.0
	 */
	public void setZonedDateTime(String headerName, ZonedDateTime date) {
		set(headerName, DATE_FORMATTER.format(date));
	}

	/**
	 * Set the given date under the given header name after formatting it as a string
	 * using the RFC-1123 date-time formatter. The equivalent of
	 * {@link #set(String, String)} but for date headers.
	 * @since 5.1.4
	 */
	public void setInstant(String headerName, Instant date) {
		setZonedDateTime(headerName, ZonedDateTime.ofInstant(date, GMT));
	}

	/**
	 * Set the given date under the given header name after formatting it as a string
	 * using the RFC-1123 date-time formatter. The equivalent of
	 * {@link #set(String, String)} but for date headers.
	 * @since 3.2.4
	 * @see #setZonedDateTime(String, ZonedDateTime)
	 */
	public void setDate(String headerName, long date) {
		setInstant(headerName, Instant.ofEpochMilli(date));
	}

	/**
	 * Parse the first header value for the given header name as a date,
	 * return -1 if there is no value, or raise {@link IllegalArgumentException}
	 * if the value cannot be parsed as a date.
	 * @param headerName the header name
	 * @return the parsed date header, or -1 if none
	 * @since 3.2.4
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getFirstDate(String headerName) {
		return getFirstDate(headerName, true);
	}

	/**
	 * Parse the first header value for the given header name as a date,
	 * return -1 if there is no value or also in case of an invalid value
	 * (if {@code rejectInvalid=false}), or raise {@link IllegalArgumentException}
	 * if the value cannot be parsed as a date.
	 * @param headerName the header name
	 * @param rejectInvalid whether to reject invalid values with an
	 * {@link IllegalArgumentException} ({@code true}) or rather return -1
	 * in that case ({@code false})
	 * @return the parsed date header, or -1 if none (or invalid)
	 * @see #getFirstZonedDateTime(String, boolean)
	 */
	private long getFirstDate(String headerName, boolean rejectInvalid) {
		ZonedDateTime zonedDateTime = getFirstZonedDateTime(headerName, rejectInvalid);
		return (zonedDateTime != null ? zonedDateTime.toInstant().toEpochMilli() : -1);
	}

	/**
	 * Parse the first header value for the given header name as a date,
	 * return {@code null} if there is no value, or raise {@link IllegalArgumentException}
	 * if the value cannot be parsed as a date.
	 * @param headerName the header name
	 * @return the parsed date header, or {@code null} if none
	 * @since 5.0
	 */
	public @Nullable ZonedDateTime getFirstZonedDateTime(String headerName) {
		return getFirstZonedDateTime(headerName, true);
	}

	/**
	 * Parse the first header value for the given header name as a date,
	 * return {@code null} if there is no value or also in case of an invalid value
	 * (if {@code rejectInvalid=false}), or raise {@link IllegalArgumentException}
	 * if the value cannot be parsed as a date.
	 * @param headerName the header name
	 * @param rejectInvalid whether to reject invalid values with an
	 * {@link IllegalArgumentException} ({@code true}) or rather return {@code null}
	 * in that case ({@code false})
	 * @return the parsed date header, or {@code null} if none (or invalid)
	 */
	private @Nullable ZonedDateTime getFirstZonedDateTime(String headerName, boolean rejectInvalid) {
		String headerValue = getFirst(headerName);
		if (headerValue == null) {
			// No header value sent at all
			return null;
		}
		if (headerValue.length() >= 3) {
			// Short "0" or "-1" like values are never valid HTTP date headers...
			// Let's only bother with DateTimeFormatter parsing for long enough values.

			// See https://stackoverflow.com/questions/12626699/if-modified-since-http-header-passed-by-ie9-includes-length
			int parametersIndex = headerValue.indexOf(';');
			if (parametersIndex != -1) {
				headerValue = headerValue.substring(0, parametersIndex);
			}

			for (DateTimeFormatter dateFormatter : DATE_PARSERS) {
				try {
					return ZonedDateTime.parse(headerValue, dateFormatter);
				}
				catch (DateTimeParseException ex) {
					// ignore
				}
			}

		}
		if (rejectInvalid) {
			throw new IllegalArgumentException("Cannot parse date value \"" + headerValue +
					"\" for \"" + headerName + "\" header");
		}
		return null;
	}

	/**
	 * Return all values of a given header name, even if this header is set
	 * multiple times.
	 * <p>This method supports double-quoted values, as described in
	 * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#section-5.5-8">RFC
	 * 9110, section 5.5</a>.
	 * @param headerName the header name
	 * @return all associated values
	 * @since 4.3
	 */
	public List<String> getValuesAsList(String headerName) {
		List<String> values = get(headerName);
		if (values != null) {
			List<String> result = new ArrayList<>();
			for (String value : values) {
				if (value != null) {
					result.addAll(tokenizeQuoted(value));
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	private static List<String> tokenizeQuoted(String str) {
		List<String> tokens = new ArrayList<>();
		boolean quoted = false;
		boolean trim = true;
		StringBuilder builder = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);
			if (ch == '"') {
				if (builder.isEmpty()) {
					quoted = true;
				}
				else if (quoted) {
					quoted = false;
					trim = false;
				}
				else {
					builder.append(ch);
				}
			}
			else if (ch == '\\' && quoted && i < str.length() - 1) {
				builder.append(str.charAt(++i));
			}
			else if (ch == ',' && !quoted) {
				addToken(builder, tokens, trim);
				builder.setLength(0);
				trim = false;
			}
			else if (quoted || (!builder.isEmpty() && trim) || !Character.isWhitespace(ch)) {
				builder.append(ch);
			}
		}
		if (!builder.isEmpty()) {
			addToken(builder, tokens, trim);
		}
		return tokens;
	}

	private static void addToken(StringBuilder builder, List<String> tokens, boolean trim) {
		String token = builder.toString();
		if (trim) {
			token = token.trim();
		}
		if (!token.isEmpty()) {
			tokens.add(token);
		}
	}

	/**
	 * Remove the well-known {@code "Content-*"} HTTP headers.
	 * <p>Such headers should be cleared from the response if the intended
	 * body can't be written due to errors.
	 * @since 5.2.3
	 */
	public void clearContentHeaders() {
		this.headers.remove(HttpHeaders.CONTENT_DISPOSITION);
		this.headers.remove(HttpHeaders.CONTENT_ENCODING);
		this.headers.remove(HttpHeaders.CONTENT_LANGUAGE);
		this.headers.remove(HttpHeaders.CONTENT_LENGTH);
		this.headers.remove(HttpHeaders.CONTENT_LOCATION);
		this.headers.remove(HttpHeaders.CONTENT_RANGE);
		this.headers.remove(HttpHeaders.CONTENT_TYPE);
	}

	/**
	 * Retrieve a combined result from the field values of the ETag header.
	 * @param name the header name
	 * @return the combined result
	 * @throws IllegalArgumentException if parsing fails
	 * @since 4.3
	 */
	protected List<String> getETagValuesAsList(String name) {
		List<String> values = get(name);
		if (values == null) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		for (String value : values) {
			if (value != null) {
				List<ETag> tags = ETag.parse(value);
				Assert.notEmpty(tags, "Could not parse header '" + name + "' with value '" + value + "'");
				for (ETag tag : tags) {
					result.add(tag.formattedTag());
				}
			}
		}
		return result;
	}

	/**
	 * Retrieve a combined result from the field values of multivalued headers.
	 * @param headerName the header name
	 * @return the combined result
	 * @since 4.3
	 */
	protected @Nullable String getFieldValues(String headerName) {
		List<String> headerValues = get(headerName);
		return (headerValues != null ? toCommaDelimitedString(headerValues) : null);
	}

	/**
	 * Turn the given list of header values into a comma-delimited result.
	 * @param headerValues the list of header values
	 * @return a combined result with comma delimitation
	 */
	protected String toCommaDelimitedString(List<String> headerValues) {
		StringJoiner joiner = new StringJoiner(", ");
		for (String val : headerValues) {
			if (val != null) {
				joiner.add(val);
			}
		}
		return joiner.toString();
	}

	/**
	 * Set the given header value, or remove the header if {@code null}.
	 * @param headerName the header name
	 * @param headerValue the header value, or {@code null} for none
	 */
	private void setOrRemove(String headerName, @Nullable String headerValue) {
		if (headerValue != null) {
			set(headerName, headerValue);
		}
		else {
			remove(headerName);
		}
	}


	// MultiValueMap-like methods

	/**
	 * Return the first header value for the given header name, if any.
	 * @param headerName the header name
	 * @return the first header value, or {@code null} if none
	 */
	public @Nullable String getFirst(String headerName) {
		return this.headers.getFirst(headerName);
	}

	/**
	 * Add the given, single header value under the given name.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	public void add(String headerName, @Nullable String headerValue) {
		this.headers.add(headerName, headerValue);
	}

	/**
	 * Add all the given values under the given name.
	 * <p>As values are represented as a {@code List}, duplicate values can be
	 * introduced. See {@link #put(String, List)} to replace the list of values
	 * instead.
	 * @param headerName the header name
	 * @param headerValues the values to add
	 * @see #put(String, List)
	 */
	public void addAll(String headerName, List<? extends String> headerValues) {
		this.headers.addAll(headerName, headerValues);
	}

	/**
	 * Add all the values of the given {@code HttpHeaders} to the current header.
	 * <p>As values are represented as a {@code List}, duplicate values can be
	 * introduced. See {@link #putAll(HttpHeaders)} to replace the list of
	 * values of each individual header name instead.
	 * @param headers the headers to add
	 * @since 7.0
	 * @see #putAll(HttpHeaders)
	 */
	public void addAll(HttpHeaders headers) {
		this.headers.addAll(headers.headers);
	}

	/**
	 * Set the given, single header value under the given name.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	public void set(String headerName, @Nullable String headerValue) {
		this.headers.set(headerName, headerValue);
	}

	/**
	 * Set all single header value from the given Map under each of their
	 * corresponding name.
	 * @param values the name-single-value pairs
	 * @see #putAll(Map)
	 */
	public void setAll(Map<String, String> values) {
		this.headers.setAll(values);
	}

	/**
	 * Return this HttpHeaders as a {@code Map} with the first values for each
	 * header name.
	 * <p>The difference between this method and {@link #asSingleValueMap()} is
	 * that this method returns a copy of the headers, whereas the latter
	 * returns a view. This copy also ensures that collection-iterating methods
	 * like {@code entrySet()} are case-insensitive.
	 * @return a single value representation of these headers
	 */
	public Map<String, String> toSingleValueMap() {
		return this.headers.toSingleValueMap();
	}

	/**
	 * Return this HttpHeaders as a {@code Map} with the first values for each
	 * header name.
	 * <p>The difference between this method and {@link #toSingleValueMap()} is
	 * that this method returns a view of the headers, whereas the latter
	 * returns a copy. This method is also susceptible to include multiple
	 * casing variants of a given header name, see {@link #asMultiValueMap()}
	 * javadoc.
	 * @return a single value representation of these headers
	 * @deprecated Use {@link #toSingleValueMap()} which performs a copy but
	 * ensures that collection-iterating methods like {@code entrySet()} are
	 * case-insensitive
	 */
	@Deprecated
	public Map<String, String> asSingleValueMap() {
		return this.headers.asSingleValueMap();
	}

	/**
	 * Return this HttpHeaders as a {@code MultiValueMap} with the full list
	 * of values for each header name.
	 * <p>Note that some backing server headers implementations can store header
	 * names in a case-sensitive manner, which will lead to duplicates during
	 * iteration in methods like {@code entrySet()}, where multiple occurrences
	 * of a header name can surface depending on letter casing but each such
	 * entry has the full {@code List} of values.
	 * @return a MultiValueMap representation of these headers
	 * @since 7.0
	 * @deprecated This method is provided for backward compatibility with APIs
	 * that would only accept maps. Generally avoid using HttpHeaders as a Map
	 * or MultiValueMap.
	 */
	@Deprecated
	public MultiValueMap<String, String> asMultiValueMap() {
		return this.headers;
	}

	// Map-like implementation

	/**
	 * Returns {@code true} if this HttpHeaders contains no header entry.
	 */
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	/**
	 * Returns {@code true} if this HttpHeaders contains an entry for the
	 * given header name.
	 * @param headerName the header name
	 * @since 7.0
	 */
	public boolean containsHeader(String headerName) {
		return this.headers.containsKey(headerName);
	}

	/**
	 * Returns {@code true} if this HttpHeaders contains exactly the given list
	 * of values for the given header name.
	 * @param headerName the header name
	 * @param values the expected list of values
	 * @since 7.0
	 */
	public boolean hasHeaderValues(String headerName, List<String> values) {
		return ObjectUtils.nullSafeEquals(this.headers.get(headerName), values);
	}

	/**
	 * Returns {@code true} if this HttpHeaders contains the given header and
	 * its list of values contains the given value.
	 * @param headerName the header name
	 * @param value the value expected to be in the list of values
	 * @since 7.0
	 */
	public boolean containsHeaderValue(String headerName, String value) {
		final List<String> values = this.headers.get(headerName);
		if (values == null) {
			return false;
		}
		return values.contains(value);
	}

	/**
	 * Get the list of values associated with the given header name.
	 * @param headerName the header name
	 * @since 7.0
	 */
	@Nullable
	public List<String> get(String headerName) {
		return this.headers.get(headerName);
	}

	/**
	 * Set the list of values associated with the given header name. Returns the
	 * previous list of values, or {@code null} if the header was not present.
	 * @param headerName the header name
	 * @param headerValues the new values
	 * @return the old values for the given header name
	 */
	public @Nullable List<String> put(String headerName, List<String> headerValues) {
		return this.headers.put(headerName, headerValues);
	}

	/**
	 * Set header values for the given header name if that header name isn't
	 * already present in this HttpHeaders and return {@code null}. If the
	 * header is already present, returns the associated value list instead.
	 * @param headerName the header name
	 * @param headerValues the header values to set if header is not present
	 * @return the previous value or {@code null}
	 */
	public @Nullable List<String> putIfAbsent(String headerName, List<String> headerValues) {
		return this.headers.putIfAbsent(headerName, headerValues);
	}

	/**
	 * Put all the entries from the given HttpHeaders into this HttpHeaders.
	 * @param headers the given headers
	 * @since 7.0
	 * @see #put(String, List)
	 */
	public void putAll(HttpHeaders headers) {
		this.headers.putAll(headers.headers);
	}

	/**
	 * Put all the entries from the given {@code MultiValueMap} into this
	 * HttpHeaders.
	 * @param headers the given headers
	 * @see #put(String, List)
	 */
	public void putAll(Map<? extends String, ? extends List<String>> headers) {
		this.headers.putAll(headers);
	}

	/**
	 * Remove a header from this HttpHeaders instance, and return the associated
	 * value list or {@code null} if that header wasn't present.
	 * @param key the name of the header to remove
	 * @return the value list associated with the removed header name
	 * @since 7.0
	 */
	public @Nullable List<String> remove(String key) {
		return this.headers.remove(key);
	}

	/**
	 * Remove all headers from this HttpHeaders instance.
	 */
	public void clear() {
		this.headers.clear();
	}

	/**
	 * Return the number of headers in the collection. This can be inflated,
	 * see {@link HttpHeaders class level javadoc}.
	 */
	public int size() {
		return this.headers.size();
	}

	/**
	 * Perform an action over each header, as when iterated via
	 * {@link #headerSet()}.
	 * @param action the action to be performed for each entry
	 */
	public void forEach(BiConsumer<? super String, ? super List<String>> action) {
		this.headerSet().forEach(e -> action.accept(e.getKey(), e.getValue()));
	}

	/**
	 * Return a view of the headers as an entry {@code Set} of key-list pairs.
	 * Both {@link Iterator#remove()} and {@link Entry#setValue}
	 * are supported and mutate the headers.
	 * <p>This collection is guaranteed to contain one entry per header name
	 * even if the backing structure stores multiple casing variants of names,
	 * at the cost of first copying the names into a case-insensitive set for
	 * filtering the iteration.
	 * @return a {@code Set} view that iterates over all headers in a
	 * case-insensitive manner
	 * @since 6.1.15
	 */
	public Set<Entry<String, List<String>>> headerSet() {
		return new CaseInsensitiveEntrySet(this.headers);
	}

	/**
	 * Return the set of header names. Both {@link Set#remove(Object)} and
	 * {@link Set#clear()} operations are supported and mutate the headers.
	 * <p>This collection is guaranteed to contain only one casing variant
	 * of each header name even if the backing structure stores multiple casing
	 * variants of names. The first encountered variant is the one that is
	 * retained.
	 * @return a {@code Set} of all the headers names
	 * @since 7.0
	 */
	public Set<String> headerNames() {
		return new CaseInsensitiveHeaderNameSet(this.headers);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof HttpHeaders that && unwrap(this).equals(unwrap(that))));
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return formatHeaders(this.headers);
	}


	/**
	 * Apply a read-only {@code HttpHeaders} wrapper around the given headers, if necessary.
	 * <p>Also caches the parsed representations of the "Accept" and "Content-Type" headers.
	 * @param headers the headers to expose
	 * @return a read-only variant of the headers, or the original headers as-is
	 * (in case it happens to be a read-only {@code HttpHeaders} instance already)
	 * @since 5.3
	 */
	public static HttpHeaders readOnlyHttpHeaders(MultiValueMap<String, String> headers) {
		return (headers instanceof HttpHeaders httpHeaders ? readOnlyHttpHeaders(httpHeaders) :
				new ReadOnlyHttpHeaders(headers));
	}

	/**
	 * Apply a read-only {@code HttpHeaders} wrapper around the given headers, if necessary.
	 * <p>Also caches the parsed representations of the "Accept" and "Content-Type" headers.
	 * @param headers the headers to expose
	 * @return a read-only variant of the headers, or the original headers as-is if already read-only
	 */
	public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		return (headers instanceof ReadOnlyHttpHeaders ? headers : new ReadOnlyHttpHeaders(headers.headers));
	}

	/**
	 * Helps to format HTTP header values, as HTTP header values themselves can
	 * contain comma-separated values, can become confusing with regular
	 * {@link Map} formatting that also uses commas between entries.
	 * <p>Additionally, this method displays the native list of header names
	 * with the mention {@code with native header names} if the underlying
	 * implementation stores multiple casing variants of header names (see
	 * {@link HttpHeaders class level javadoc}).
	 * @param headers the headers to format
	 * @return the headers to a String
	 * @since 5.1.4
	 */
	public static String formatHeaders(MultiValueMap<String, String> headers) {
		Set<String> headerNames = new CaseInsensitiveHeaderNameSet(headers);
		String suffix = "]";
		if (headerNames.size() != headers.size()) {
			suffix = "] with native header names " + headers.keySet();
		}

		return headerNames.stream()
				.map(headerName -> {
					List<String> values = headers.get(headerName);
					Assert.notNull(values, "Expected at least one value for header " + headerName);
					return headerName + ":" + (values.size() == 1 ?
							"\"" + values.get(0) + "\"" :
							values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
				})
				.collect(Collectors.joining(", ", "[", suffix));
	}

	/**
	 * Encode the given username and password into Basic Authentication credentials.
	 * <p>The encoded credentials returned by this method can be supplied to
	 * {@link #setBasicAuth(String)} to set the Basic Authentication header.
	 * @param username the username
	 * @param password the password
	 * @param charset the charset to use to convert the credentials into an octet
	 * sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
	 * @throws IllegalArgumentException if {@code username} or {@code password}
	 * contains characters that cannot be encoded to the given charset
	 * @since 5.2
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public static String encodeBasicAuth(String username, String password, @Nullable Charset charset) {
		Assert.notNull(username, "Username must not be null");
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		Assert.notNull(password, "Password must not be null");
		if (charset == null) {
			charset = StandardCharsets.ISO_8859_1;
		}

		CharsetEncoder encoder = charset.newEncoder();
		if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to " + charset.displayName());
		}

		String credentialsString = username + ":" + password;
		byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
		return new String(encodedBytes, charset);
	}


	private static MultiValueMap<String, String> unwrap(HttpHeaders headers) {
		while (headers.headers instanceof HttpHeaders httpHeaders) {
			headers = httpHeaders;
		}
		return headers.headers;
	}

	// Package-private: used in ResponseCookie
	static String formatDate(long date) {
		Instant instant = Instant.ofEpochMilli(date);
		ZonedDateTime time = ZonedDateTime.ofInstant(instant, GMT);
		return DATE_FORMATTER.format(time);
	}


	private static final class CaseInsensitiveHeaderNameSet extends AbstractSet<String> {

		private static final Object VALUE = new Object();

		private final MultiValueMap<String, String> headers;
		private final Map<String, Object> deduplicatedNames;

		public CaseInsensitiveHeaderNameSet(MultiValueMap<String, String> headers) {
			this.headers = headers;
			this.deduplicatedNames = new LinkedCaseInsensitiveMap<>(headers.size(), Locale.ROOT);
			// add/addAll (put/putAll in LinkedCaseInsensitiveMap) retain the casing of the last occurrence.
			// Here we prefer the first.
			for (String header : headers.keySet()) {
				if (!this.deduplicatedNames.containsKey(header)) {
					this.deduplicatedNames.put(header, VALUE);
				}
			}
		}

		@Override
		public Iterator<String> iterator() {
			return new HeaderNamesIterator(this.headers, this.deduplicatedNames);
		}

		@Override
		public int size() {
			return this.deduplicatedNames.size();
		}

		@Override
		public boolean contains(Object o) {
			return this.headers.containsKey(o);
		}

		@Override
		public boolean remove(Object o) {
			return this.headers.remove(o) != null && this.deduplicatedNames.remove(o) != null;
		}

		@Override
		public void clear() {
			this.headers.clear();
			this.deduplicatedNames.clear();
		}
	}

	private static class HeaderNamesIterator implements Iterator<String> {


		private @Nullable String currentName;

		private final MultiValueMap<String, String> headers;
		private final Iterator<String> namesIterator;

		public HeaderNamesIterator(MultiValueMap<String, String> headers, Map<String, Object> caseInsensitiveNames) {
			this.headers = headers;
			this.namesIterator = caseInsensitiveNames.keySet().iterator();
			this.currentName = null;
		}

		@Override
		public boolean hasNext() {
			return this.namesIterator.hasNext();
		}

		@Override
		public String next() {
			this.currentName = this.namesIterator.next();
			return this.currentName;
		}

		@Override
		public void remove() {
			if (this.currentName == null) {
				throw new IllegalStateException("No current Header in iterator");
			}
			if (!this.headers.containsKey(this.currentName)) {
				throw new IllegalStateException("Header not present: " + this.currentName);
			}
			this.headers.remove(this.currentName);
		}
	}


	private static final class CaseInsensitiveEntrySet extends AbstractSet<Entry<String, List<String>>> {

		private final MultiValueMap<String, String> headers;
		private final CaseInsensitiveHeaderNameSet nameSet;

		public CaseInsensitiveEntrySet(MultiValueMap<String, String> headers) {
			this.headers = headers;
			this.nameSet = new CaseInsensitiveHeaderNameSet(headers);
		}

		@Override
		public Iterator<Entry<String, List<String>>> iterator() {
			return new CaseInsensitiveIterator(this.nameSet.iterator());
		}

		@Override
		public int size() {
			return this.nameSet.size();
		}

		private final class CaseInsensitiveIterator implements Iterator<Entry<String, List<String>>> {

			private final Iterator<String> namesIterator;

			public CaseInsensitiveIterator(Iterator<String> namesIterator) {
				this.namesIterator = namesIterator;
			}

			@Override
			public boolean hasNext() {
				return this.namesIterator.hasNext();
			}

			@Override
			public Entry<String, List<String>> next() {
				return new CaseInsensitiveEntrySet.CaseInsensitiveEntry(this.namesIterator.next());
			}

			@Override
			public void remove() {
				this.namesIterator.remove();
			}
		}

		private final class CaseInsensitiveEntry implements Entry<String, List<String>> {

			private final String key;

			CaseInsensitiveEntry(String key) {
				this.key = key;
			}

			@Override
			public String getKey() {
				return this.key;
			}

			@Override
			public List<String> getValue() {
				return Objects.requireNonNull(CaseInsensitiveEntrySet.this.headers.get(this.key));
			}

			@Override
			public List<String> setValue(List<String> value) {
				List<String> previousValues = Objects.requireNonNull(
						CaseInsensitiveEntrySet.this.headers.get(this.key));
				CaseInsensitiveEntrySet.this.headers.put(this.key, value);
				return previousValues;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (!(o instanceof Map.Entry<?,?> that)) {
					return false;
				}
				return ObjectUtils.nullSafeEquals(getKey(), that.getKey()) && ObjectUtils.nullSafeEquals(getValue(), that.getValue());
			}

			@Override
			public int hashCode() {
				return ObjectUtils.nullSafeHash(getKey(), getValue());
			}
		}
	}

}
