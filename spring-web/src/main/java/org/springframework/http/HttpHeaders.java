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

package org.springframework.http;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
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
 * <p>Note that {@code HttpHeaders} generally treats header names in a case-insensitive manner.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Josh Long
 * @since 3.0
 */
public class HttpHeaders implements MultiValueMap<String, String>, Serializable {

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
	public static final HttpHeaders EMPTY = new HttpHeaders(new LinkedHashMap<>(), true);

	/**
	 * Pattern matching ETag multiple field values in headers such as "If-Match", "If-None-Match".
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
	 */
	private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

	private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);

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


	private final Map<String, List<String>> headers;

	private final boolean readOnly;


	/**
	 * Construct a new, empty instance of the {@code HttpHeaders} object.
	 */
	public HttpHeaders() {
		this(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH), false);
	}

	/**
	 * Private constructor that can create read-only {@code HttpHeader} instances.
	 */
	private HttpHeaders(Map<String, List<String>> headers, boolean readOnly) {
		if (readOnly) {
			Map<String, List<String>> map = new LinkedCaseInsensitiveMap<>(headers.size(), Locale.ENGLISH);
			headers.forEach((key, valueList) -> map.put(key, Collections.unmodifiableList(valueList)));
			this.headers = Collections.unmodifiableMap(map);
		}
		else {
			this.headers = headers;
		}
		this.readOnly = readOnly;
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
				.collect(Collectors.toList());
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
		return (StringUtils.hasText(value) ? Locale.LanguageRange.parse(value) : Collections.emptyList());
	}

	/**
	 * Variant of {@link #setAcceptLanguage(List)} using {@link Locale}'s.
	 * @since 5.0
	 */
	public void setAcceptLanguageAsLocales(List<Locale> locales) {
		setAcceptLanguage(locales.stream()
				.map(locale -> new Locale.LanguageRange(locale.toLanguageTag()))
				.collect(Collectors.toList()));
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
		return ranges.stream()
				.map(range -> Locale.forLanguageTag(range.getRange()))
				.filter(locale -> StringUtils.hasText(locale.getDisplayName()))
				.collect(Collectors.toList());
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
		List<HttpMethod> result = new ArrayList<>();
		String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
		if (value != null) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			for (String token : tokens) {
				HttpMethod resolved = HttpMethod.resolve(token);
				if (resolved != null) {
					result.add(resolved);
				}
			}
		}
		return result;
	}

	/**
	 * Set the (new) value of the {@code Access-Control-Allow-Origin} response header.
	 */
	public void setAccessControlAllowOrigin(@Nullable String allowedOrigin) {
		set(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
	}

	/**
	 * Return the value of the {@code Access-Control-Allow-Origin} response header.
	 */
	@Nullable
	public String getAccessControlAllowOrigin() {
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
		set(ACCESS_CONTROL_REQUEST_METHOD, (requestMethod != null ? requestMethod.name() : null));
	}

	/**
	 * Return the value of the {@code Access-Control-Request-Method} request header.
	 */
	@Nullable
	public HttpMethod getAccessControlRequestMethod() {
		return HttpMethod.resolve(getFirst(ACCESS_CONTROL_REQUEST_METHOD));
	}

	/**
	 * Set the list of acceptable {@linkplain Charset charsets},
	 * as specified by the {@code Accept-Charset} header.
	 */
	public void setAcceptCharset(List<Charset> acceptableCharsets) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<Charset> iterator = acceptableCharsets.iterator(); iterator.hasNext();) {
			Charset charset = iterator.next();
			builder.append(charset.name().toLowerCase(Locale.ENGLISH));
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		set(ACCEPT_CHARSET, builder.toString());
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
			List<HttpMethod> result = new ArrayList<>(tokens.length);
			for (String token : tokens) {
				HttpMethod resolved = HttpMethod.resolve(token);
				if (resolved != null) {
					result.add(resolved);
				}
			}
			return EnumSet.copyOf(result);
		}
		else {
			return EnumSet.noneOf(HttpMethod.class);
		}
	}

	/**
	 * Set a configured {@link CacheControl} instance as the
	 * new value of the {@code Cache-Control} header.
	 * @since 5.0.5
	 */
	public void setCacheControl(CacheControl cacheControl) {
		set(CACHE_CONTROL, cacheControl.getHeaderValue());
	}

	/**
	 * Set the (new) value of the {@code Cache-Control} header.
	 */
	public void setCacheControl(@Nullable String cacheControl) {
		set(CACHE_CONTROL, cacheControl);
	}

	/**
	 * Return the value of the {@code Cache-Control} header.
	 */
	@Nullable
	public String getCacheControl() {
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
		ContentDisposition.Builder disposition = ContentDisposition.builder("form-data").name(name);
		if (filename != null) {
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
		if (contentDisposition != null) {
			return ContentDisposition.parse(contentDisposition);
		}
		return ContentDisposition.empty();
	}

	/**
	 * Set the {@link Locale} of the content language,
	 * as specified by the {@literal Content-Language} header.
	 * <p>Use {@code set(CONTENT_LANGUAGE, ...)} if you need
	 * to set multiple content languages.</p>
	 * @since 5.0
	 */
	public void setContentLanguage(@Nullable Locale locale) {
		set(CONTENT_LANGUAGE, (locale != null ? locale.toLanguageTag() : null));
	}

	/**
	 * Return the first {@link Locale} of the content languages,
	 * as specified by the {@literal Content-Language} header.
	 * <p>Returns {@code null} when the content language is unknown.
	 * <p>Use {@code getValuesAsList(CONTENT_LANGUAGE)} if you need
	 * to get multiple content languages.</p>
	 * @since 5.0
	 */
	@Nullable
	public Locale getContentLanguage() {
		return getValuesAsList(CONTENT_LANGUAGE)
				.stream()
				.findFirst()
				.map(Locale::forLanguageTag)
				.orElse(null);
	}

	/**
	 * Set the length of the body in bytes, as specified by the
	 * {@code Content-Length} header.
	 */
	public void setContentLength(long contentLength) {
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
			set(CONTENT_TYPE, null);
		}
	}

	/**
	 * Return the {@linkplain MediaType media type} of the body, as specified
	 * by the {@code Content-Type} header.
	 * <p>Returns {@code null} when the content-type is unknown.
	 */
	@Nullable
	public MediaType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
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
	public void setETag(@Nullable String etag) {
		if (etag != null) {
			Assert.isTrue(etag.startsWith("\"") || etag.startsWith("W/"),
					"Invalid ETag: does not start with W/ or \"");
			Assert.isTrue(etag.endsWith("\""), "Invalid ETag: does not end with \"");
		}
		set(ETAG, etag);
	}

	/**
	 * Return the entity tag of the body, as specified by the {@code ETag} header.
	 */
	@Nullable
	public String getETag() {
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
			set(HOST, null);
		}
	}

	/**
	 * Return the value of the {@code Host} header, if available.
	 * <p>If the header value does not contain a port, the
	 * {@linkplain InetSocketAddress#getPort() port} in the returned address will
	 * be {@code 0}.
	 * @since 5.0
	 */
	@Nullable
	public InetSocketAddress getHost() {
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
	 * @since 4.3
	 */
	public List<String> getIfMatch() {
		return getETagValuesAsList(IF_MATCH);
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
	 */
	public List<String> getIfNoneMatch() {
		return getETagValuesAsList(IF_NONE_MATCH);
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
		set(LOCATION, (location != null ? location.toASCIIString() : null));
	}

	/**
	 * Return the (new) location of a resource
	 * as specified by the {@code Location} header.
	 * <p>Returns {@code null} when the location is unknown.
	 */
	@Nullable
	public URI getLocation() {
		String value = getFirst(LOCATION);
		return (value != null ? URI.create(value) : null);
	}

	/**
	 * Set the (new) value of the {@code Origin} header.
	 */
	public void setOrigin(@Nullable String origin) {
		set(ORIGIN, origin);
	}

	/**
	 * Return the value of the {@code Origin} header.
	 */
	@Nullable
	public String getOrigin() {
		return getFirst(ORIGIN);
	}

	/**
	 * Set the (new) value of the {@code Pragma} header.
	 */
	public void setPragma(@Nullable String pragma) {
		set(PRAGMA, pragma);
	}

	/**
	 * Return the value of the {@code Pragma} header.
	 */
	@Nullable
	public String getPragma() {
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
		set(UPGRADE, upgrade);
	}

	/**
	 * Return the value of the {@code Upgrade} header.
	 */
	@Nullable
	public String getUpgrade() {
		return getFirst(UPGRADE);
	}

	/**
	 * Set the request header names (e.g. "Accept-Language") for which the
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
	 * @since 3.2.4
	 * @see #setZonedDateTime(String, ZonedDateTime)
	 */
	public void setDate(String headerName, long date) {
		set(headerName, formatDate(date));
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
	@Nullable
	public ZonedDateTime getFirstZonedDateTime(String headerName) {
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
	@Nullable
	private ZonedDateTime getFirstZonedDateTime(String headerName, boolean rejectInvalid) {
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
	 * Return all values of a given header name,
	 * even if this header is set multiple times.
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
					Collections.addAll(result, StringUtils.tokenizeToStringArray(value, ","));
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * Retrieve a combined result from the field values of the ETag header.
	 * @param headerName the header name
	 * @return the combined result
	 * @since 4.3
	 */
	protected List<String> getETagValuesAsList(String headerName) {
		List<String> values = get(headerName);
		if (values != null) {
			List<String> result = new ArrayList<>();
			for (String value : values) {
				if (value != null) {
					Matcher matcher = ETAG_HEADER_VALUE_PATTERN.matcher(value);
					while (matcher.find()) {
						if ("*".equals(matcher.group())) {
							result.add(matcher.group());
						}
						else {
							result.add(matcher.group(1));
						}
					}
					if (result.isEmpty()) {
						throw new IllegalArgumentException(
								"Could not parse header '" + headerName + "' with value '" + value + "'");
					}
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * Retrieve a combined result from the field values of multi-valued headers.
	 * @param headerName the header name
	 * @return the combined result
	 * @since 4.3
	 */
	@Nullable
	protected String getFieldValues(String headerName) {
		List<String> headerValues = get(headerName);
		return (headerValues != null ? toCommaDelimitedString(headerValues) : null);
	}

	/**
	 * Turn the given list of header values into a comma-delimited result.
	 * @param headerValues the list of header values
	 * @return a combined result with comma delimitation
	 */
	protected String toCommaDelimitedString(List<String> headerValues) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> it = headerValues.iterator(); it.hasNext();) {
			String val = it.next();
			builder.append(val);
			if (it.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}


	// MultiValueMap implementation

	/**
	 * Return the first header value for the given header name, if any.
	 * @param headerName the header name
	 * @return the first header value, or {@code null} if none
	 */
	@Override
	@Nullable
	public String getFirst(String headerName) {
		List<String> headerValues = this.headers.get(headerName);
		return (headerValues != null ? headerValues.get(0) : null);
	}

	/**
	 * Add the given, single header value under the given name.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	@Override
	public void add(String headerName, @Nullable String headerValue) {
		List<String> headerValues = this.headers.computeIfAbsent(headerName, k -> new LinkedList<>());
		headerValues.add(headerValue);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		List<String> currentValues = this.headers.computeIfAbsent(key, k -> new LinkedList<>());
		currentValues.addAll(values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this::addAll);
	}

	/**
	 * Set the given, single header value under the given name.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	@Override
	public void set(String headerName, @Nullable String headerValue) {
		List<String> headerValues = new LinkedList<>();
		headerValues.add(headerValue);
		this.headers.put(headerName, headerValues);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		LinkedHashMap<String, String> singleValueMap = new LinkedHashMap<>(this.headers.size());
		this.headers.forEach((key, valueList) -> singleValueMap.put(key, valueList.get(0)));
		return singleValueMap;
	}


	// Map implementation

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HttpHeaders)) {
			return false;
		}
		HttpHeaders otherHeaders = (HttpHeaders) other;
		return this.headers.equals(otherHeaders.headers);
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return this.headers.toString();
	}


	/**
	 * Return an {@code HttpHeaders} object that can only be read, not written to.
	 */
	public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		return (headers.readOnly ? headers : new HttpHeaders(headers, true));
	}

	// Package-private: used in ResponseCookie
	static String formatDate(long date) {
		Instant instant = Instant.ofEpochMilli(date);
		ZonedDateTime time = ZonedDateTime.ofInstant(instant, GMT);
		return DATE_FORMATTER.format(time);
	}

}
