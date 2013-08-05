/*
	 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http;

import java.io.Serializable;

import java.net.URI;

import java.nio.charset.Charset;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Represents HTTP request and response headers, mapping string header names to list of string values.
 *
 * <p>In addition to the normal methods defined by {@link Map}, this class offers the following convenience methods:
 * <ul>
 * <li>{@link #getFirst(String)} returns the first value associated with a given header name</li>
 * <li>{@link #add(String, String)} adds a header value to the list of values for a header name</li>
 * <li>{@link #set(String, String)} sets the header value to a single string value</li>
 * </ul>
 *
 * <p>Inspired by {@link com.sun.net.httpserver.Headers}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class HttpHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = -8578554704772377436L;

	private static final String ACCEPT = "Accept";

	private static final String ACCEPT_CHARSET = "Accept-Charset";

	private static final String ALLOW = "Allow";

	private static final String CACHE_CONTROL = "Cache-Control";

	private static final String CONTENT_DISPOSITION = "Content-Disposition";

	private static final String CONTENT_LENGTH = "Content-Length";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String DATE = "Date";

	private static final String ETAG = "ETag";

	private static final String EXPIRES = "Expires";

	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String IF_NONE_MATCH = "If-None-Match";

	private static final String LAST_MODIFIED = "Last-Modified";

	private static final String LOCATION = "Location";

	private static final String PRAGMA = "Pragma";


	private static final String[] DATE_FORMATS = new String[] {
		"EEE, dd MMM yyyy HH:mm:ss zzz",
		"EEE, dd-MMM-yy HH:mm:ss zzz",
		"EEE MMM dd HH:mm:ss yyyy"
	};

	private static TimeZone GMT = TimeZone.getTimeZone("GMT");

	private final Map<String, List<String>> headers;


	/**
	 * Private constructor that can create read-only {@code HttpHeader} instances.
	 */
	private HttpHeaders(Map<String, List<String>> headers, boolean readOnly) {
		Assert.notNull(headers, "'headers' must not be null");
		if (readOnly) {
			Map<String, List<String>> map =
					new LinkedCaseInsensitiveMap<List<String>>(headers.size(), Locale.ENGLISH);
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				List<String> values = Collections.unmodifiableList(entry.getValue());
				map.put(entry.getKey(), values);
			}
			this.headers = Collections.unmodifiableMap(map);
		}
		else {
			this.headers = headers;
		}
	}

	/**
	 * Constructs a new, empty instance of the {@code HttpHeaders} object.
	 */
	public HttpHeaders() {
		this(new LinkedCaseInsensitiveMap<List<String>>(8, Locale.ENGLISH), false);
	}

	/**
	 * Returns {@code HttpHeaders} object that can only be read, not written to.
	 */
	public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
		return new HttpHeaders(headers, true);
	}

	/**
	 * Set the list of acceptable {@linkplain MediaType media types}, as specified by the {@code Accept} header.
	 * @param acceptableMediaTypes the acceptable media types
	 */
	public void setAccept(List<MediaType> acceptableMediaTypes) {
		set(ACCEPT, MediaType.toString(acceptableMediaTypes));
	}

	/**
	 * Return the list of acceptable {@linkplain MediaType media types}, as specified by the {@code Accept} header.
	 * <p>Returns an empty list when the acceptable media types are unspecified.
	 * @return the acceptable media types
	 */
	public List<MediaType> getAccept() {
		String value = getFirst(ACCEPT);
		List<MediaType> result = (value != null) ? MediaType.parseMediaTypes(value) : Collections.<MediaType>emptyList();

		// Some containers parse 'Accept' into multiple values
		if ((result.size() == 1) && (headers.get(ACCEPT).size() > 1)) {
			value = StringUtils.collectionToCommaDelimitedString(headers.get(ACCEPT));
			result = MediaType.parseMediaTypes(value);
		}

		return result;
	}

	/**
	 * Set the list of acceptable {@linkplain Charset charsets}, as specified by the {@code Accept-Charset} header.
	 * @param acceptableCharsets the acceptable charsets
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
	 * Return the list of acceptable {@linkplain Charset charsets}, as specified by the {@code Accept-Charset}
	 * header.
	 * @return the acceptable charsets
	 */
	public List<Charset> getAcceptCharset() {
		List<Charset> result = new ArrayList<Charset>();
		String value = getFirst(ACCEPT_CHARSET);
		if (value != null) {
			String[] tokens = value.split(",\\s*");
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
		}
		return result;
	}

	/**
	 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified by the {@code Allow} header.
	 * @param allowedMethods the allowed methods
	 */
	public void setAllow(Set<HttpMethod> allowedMethods) {
		set(ALLOW, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * Return the set of allowed {@link HttpMethod HTTP methods}, as specified by the {@code Allow} header.
	 * <p>Returns an empty set when the allowed methods are unspecified.
	 * @return the allowed methods
	 */
	public Set<HttpMethod> getAllow() {
		String value = getFirst(ALLOW);
		if (value != null) {
			List<HttpMethod> allowedMethod = new ArrayList<HttpMethod>(5);
			String[] tokens = value.split(",\\s*");
			for (String token : tokens) {
				allowedMethod.add(HttpMethod.valueOf(token));
			}
			return EnumSet.copyOf(allowedMethod);
		}
		else {
			return EnumSet.noneOf(HttpMethod.class);
		}
	}

	/**
	 * Sets the (new) value of the {@code Cache-Control} header.
	 * @param cacheControl the value of the header
	 */
	public void setCacheControl(String cacheControl) {
		set(CACHE_CONTROL, cacheControl);
	}

	/**
	 * Returns the value of the {@code Cache-Control} header.
	 * @return the value of the header
	 */
	public String getCacheControl() {
		return getFirst(CACHE_CONTROL);
	}

	/**
	 * Sets the (new) value of the {@code Content-Disposition} header for {@code form-data}.
	 * @param name the control name
	 * @param filename the filename, may be {@code null}
	 */
	public void setContentDispositionFormData(String name, String filename) {
		Assert.notNull(name, "'name' must not be null");
		StringBuilder builder = new StringBuilder("form-data; name=\"");
		builder.append(name).append('\"');
		if (filename != null) {
			builder.append("; filename=\"");
			builder.append(filename).append('\"');
		}
		set(CONTENT_DISPOSITION, builder.toString());
	}

	/**
	 * Set the length of the body in bytes, as specified by the {@code Content-Length} header.
	 * @param contentLength the content length
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * Return the length of the body in bytes, as specified by the {@code Content-Length} header.
	 * <p>Returns -1 when the content-length is unknown.
	 * @return the content length
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * Set the {@linkplain MediaType media type} of the body, as specified by the {@code Content-Type} header.
	 * @param mediaType the media type
	 */
	public void setContentType(MediaType mediaType) {
		Assert.isTrue(!mediaType.isWildcardType(), "'Content-Type' cannot contain wildcard type '*'");
		Assert.isTrue(!mediaType.isWildcardSubtype(), "'Content-Type' cannot contain wildcard subtype '*'");
		set(CONTENT_TYPE, mediaType.toString());
	}

	/**
	 * Return the {@linkplain MediaType media type} of the body, as specified by the {@code Content-Type} header.
	 * <p>Returns {@code null} when the content-type is unknown.
	 * @return the content type
	 */
	public MediaType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (value != null ? MediaType.parseMediaType(value) : null);
	}

	/**
	 * Sets the date and time at which the message was created, as specified by the {@code Date} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param date the date
	 */
	public void setDate(long date) {
		setDate(DATE, date);
	}

	/**
	 * Returns the date and time at which the message was created, as specified by the {@code Date} header.
	 * <p>The date is returned as the number of milliseconds since January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @return the creation date/time
	 * @throws IllegalArgumentException if the value can't be converted to a date
	 */
	public long getDate() {
		return getFirstDate(DATE);
	}

	/**
	 * Sets the (new) entity tag of the body, as specified by the {@code ETag} header.
	 * @param eTag the new entity tag
	 */
	public void setETag(String eTag) {
		if (eTag != null) {
			Assert.isTrue(eTag.startsWith("\"") || eTag.startsWith("W/"), "Invalid eTag, does not start with W/ or \"");
			Assert.isTrue(eTag.endsWith("\""), "Invalid eTag, does not end with \"");
		}
		set(ETAG, eTag);
	}

	/**
	 * Returns the entity tag of the body, as specified by the {@code ETag} header.
	 * @return the entity tag
	 */
	public String getETag() {
		return getFirst(ETAG);
	}

	/**
	 * Sets the date and time at which the message is no longer valid, as specified by the {@code Expires} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param expires the new expires header value
	 */
	public void setExpires(long expires) {
		setDate(EXPIRES, expires);
	}

	/**
	 * Returns the date and time at which the message is no longer valid, as specified by
	 * the {@code Expires} header.
	 * <p>The date is returned as the number of milliseconds since January 1, 1970 GMT.
	 * Returns -1 when the date is unknown.
	 *
	 * @return the expires value
	 */
	public long getExpires() {
		try {
			return getFirstDate(EXPIRES);
		}
		catch (IllegalArgumentException ex) {
			return -1;
		}
	}

	/**
	 * Sets the (new) value of the {@code If-Modified-Since} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param ifModifiedSince the new value of the header
	 */
	public void setIfModifiedSince(long ifModifiedSince) {
		setDate(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * Returns the value of the {@code IfModifiedSince} header.
	 * <p>The date is returned as the number of milliseconds since January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @return the header value
	 * @deprecated use {@link #getIfModifiedSince()}
	 */
	@Deprecated
	public long getIfNotModifiedSince() {
		return getIfModifiedSince();
	}

	/**
	 * Returns the value of the {@code If-Modified-Since} header.
	 * <p>The date is returned as the number of milliseconds since January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @return the header value
	 */
	public long getIfModifiedSince() {
		return getFirstDate(IF_MODIFIED_SINCE);
	}

	/**
	 * Sets the (new) value of the {@code If-None-Match} header.
	 * @param ifNoneMatch the new value of the header
	 */
	public void setIfNoneMatch(String ifNoneMatch) {
		set(IF_NONE_MATCH, ifNoneMatch);
	}

	/**
	 * Sets the (new) values of the {@code If-None-Match} header.
	 * @param ifNoneMatchList the new value of the header
	 */
	public void setIfNoneMatch(List<String> ifNoneMatchList) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> iterator = ifNoneMatchList.iterator(); iterator.hasNext();) {
			String ifNoneMatch = iterator.next();
			builder.append(ifNoneMatch);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		set(IF_NONE_MATCH, builder.toString());
	}

	/**
	 * Returns the value of the {@code If-None-Match} header.
	 * @return the header value
	 */
	public List<String> getIfNoneMatch() {
		List<String> result = new ArrayList<String>();

		String value = getFirst(IF_NONE_MATCH);
		if (value != null) {
			String[] tokens = value.split(",\\s*");
			for (String token : tokens) {
				result.add(token);
			}
		}
		return result;
	}

	/**
	 * Sets the time the resource was last changed, as specified by the {@code Last-Modified} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param lastModified the last modified date
	 */
	public void setLastModified(long lastModified) {
		setDate(LAST_MODIFIED, lastModified);
	}

	/**
	 * Returns the time the resource was last changed, as specified by the {@code Last-Modified} header.
	 * <p>The date is returned as the number of milliseconds since January 1, 1970 GMT. Returns -1 when the date is unknown.
	 * @return the last modified date
	 */
	public long getLastModified() {
		return getFirstDate(LAST_MODIFIED);
	}

	/**
	 * Set the (new) location of a resource, as specified by the {@code Location} header.
	 * @param location the location
	 */
	public void setLocation(URI location) {
		set(LOCATION, location.toASCIIString());
	}

	/**
	 * Return the (new) location of a resource, as specified by the {@code Location} header.
	 * <p>Returns {@code null} when the location is unknown.
	 * @return the location
	 */
	public URI getLocation() {
		String value = getFirst(LOCATION);
		return (value != null ? URI.create(value) : null);
	}

	/**
	 * Sets the (new) value of the {@code Pragma} header.
	 * @param pragma the value of the header
	 */
	public void setPragma(String pragma) {
		set(PRAGMA, pragma);
	}

	/**
	 * Returns the value of the {@code Pragma} header.
	 * @return the value of the header
	 */
	public String getPragma() {
		return getFirst(PRAGMA);
	}

	// Date methods

	/**
	 * Parse the first header value for the given header name as a date, return -1 if
	 * there is no value, or raise {@link IllegalArgumentException} if the value cannot be
	 * parsed as a date.
	 */
	public long getFirstDate(String headerName) {
		String headerValue = getFirst(headerName);
		if (headerValue == null) {
			return -1;
		}
		for (String dateFormat : DATE_FORMATS) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
			simpleDateFormat.setTimeZone(GMT);
			try {
				return simpleDateFormat.parse(headerValue).getTime();
			}
			catch (ParseException e) {
				// ignore
			}
		}
		throw new IllegalArgumentException("Cannot parse date value \"" + headerValue +
				"\" for \"" + headerName + "\" header");
	}

	/**
	 * Set the given date under the given header name after formatting it as a string
	 * using the pattern {@code "EEE, dd MMM yyyy HH:mm:ss zzz"}. The equivalent of
	 * {@link #set(String, String)} but for date headers.
	 */
	public void setDate(String headerName, long date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMATS[0], Locale.US);
		dateFormat.setTimeZone(GMT);
		set(headerName, dateFormat.format(new Date(date)));
	}

	// Single string methods

	/**
	 * Return the first header value for the given header name, if any.
	 * @param headerName the header name
	 * @return the first header value; or {@code null}
	 */
	public String getFirst(String headerName) {
		List<String> headerValues = headers.get(headerName);
		return headerValues != null ? headerValues.get(0) : null;
	}

	/**
	 * Add the given, single header value under the given name.
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	public void add(String headerName, String headerValue) {
		List<String> headerValues = headers.get(headerName);
		if (headerValues == null) {
			headerValues = new LinkedList<String>();
			this.headers.put(headerName, headerValues);
		}
		headerValues.add(headerValue);
	}

	/**
	 * Set the given, single header value under the given name.
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @throws UnsupportedOperationException if adding headers is not supported
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	public void set(String headerName, String headerValue) {
		List<String> headerValues = new LinkedList<String>();
		headerValues.add(headerValue);
		headers.put(headerName, headerValues);
	}

	public void setAll(Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			set(entry.getKey(), entry.getValue());
		}
	}

	public Map<String, String> toSingleValueMap() {
		LinkedHashMap<String, String> singleValueMap = new LinkedHashMap<String,String>(this.headers.size());
		for (Entry<String, List<String>> entry : headers.entrySet()) {
			singleValueMap.put(entry.getKey(), entry.getValue().get(0));
		}
		return singleValueMap;
	}

	// Map implementation

	public int size() {
		return this.headers.size();
	}

	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	public void putAll(Map<? extends String, ? extends List<String>> m) {
		this.headers.putAll(m);
	}

	public void clear() {
		this.headers.clear();
	}

	public Set<String> keySet() {
		return this.headers.keySet();
	}

	public Collection<List<String>> values() {
		return this.headers.values();
	}

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

}
