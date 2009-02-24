/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.http;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.util.Assert;
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
public class HttpHeaders implements MultiValueMap<String, String> {

	private static String ACCEPT = "Accept";

	private static String ACCEPT_CHARSET = "Accept-Charset";

	private static String ALLOW = "Allow";

	private static String CONTENT_LENGTH = "Content-Length";

	private static String CONTENT_TYPE = "Content-Type";

	private static String LOCATION = "Location";


	private final Map<String, List<String>> headers = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(5);


	/**
	 * Set the list of acceptable {@linkplain MediaType media types}, as specified by the <code>Accept</code> header.
	 * @param acceptableMediaTypes the acceptable media types
	 */
	public void setAccept(List<MediaType> acceptableMediaTypes) {
		set(ACCEPT, MediaType.toString(acceptableMediaTypes));
	}

	/**
	 * Return the list of acceptable {@linkplain MediaType media types}, as specified by the <code>Accept</code> header.
	 * <p>Returns an empty list when the acceptable media types are unspecified.
	 * @return the acceptable media types
	 */
	public List<MediaType> getAccept() {
		String value = getFirst(ACCEPT);
		return (value != null ? MediaType.parseMediaTypes(value) : Collections.<MediaType>emptyList());
	}

	/**
	 * Set the list of acceptable {@linkplain Charset charsets}, as specified by the <code>Accept-Charset</code> header.
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
	 * Return the list of acceptable {@linkplain Charset charsets}, as specified by the <code>Accept-Charset</code>
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
				if (paramIdx == -1) {
					result.add(Charset.forName(token));
				}
				else {
					result.add(Charset.forName(token.substring(0, paramIdx)));
				}
			}
		}
		return result;
	}

	/**
	 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified by the <code>Allow</code> header.
	 * @param allowedMethods the allowed methods
	 */
	public void setAllow(Set<HttpMethod> allowedMethods) {
		set(ALLOW, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * Return the set of allowed {@link HttpMethod HTTP methods}, as specified by the <code>Allow</code> header.
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
	 * Set the length of the body in bytes, as specified by the <code>Content-Length</code> header.
	 * @param contentLength the content length
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * Return the length of the body in bytes, as specified by the <code>Content-Length</code> header.
	 * <p>Returns -1 when the content-length is unknown.
	 * @return the content length
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * Set the {@linkplain MediaType media type} of the body, as specified by the <code>Content-Type</code> header.
	 * @param mediaType the media type
	 */
	public void setContentType(MediaType mediaType) {
		Assert.isTrue(!mediaType.isWildcardType(), "'Content-Type' cannot contain wildcard type '*'");
		Assert.isTrue(!mediaType.isWildcardSubtype(), "'Content-Type' cannot contain wildcard subtype '*'");
		set(CONTENT_TYPE, mediaType.toString());
	}

	/**
	 * Return the {@linkplain MediaType media type} of the body, as specified by the <code>Content-Type</code> header.
	 * <p>Returns <code>null</code> when the content-type is unknown.
	 * @return the content type
	 */
	public MediaType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (value != null ? MediaType.parseMediaType(value) : null);
	}

	/**
	 * Set the (new) location of a resource, as specified by the <code>Location</code> header.
	 * @param location the location
	 */
	public void setLocation(URI location) {
		set(LOCATION, location.toASCIIString());
	}

	/**
	 * Return the (new) location of a resource, as specified by the <code>Location</code> header.
	 * <p>Returns <code>null</code> when the location is unknown.
	 * @return the location
	 */
	public URI getLocation() {
		String value = getFirst(LOCATION);
		return (value != null ? URI.create(value) : null);
	}


	// Single string methods

	/**
	 * Return the first header value for the given header name, if any.
	 * @param headerName the header name
	 * @return the first header value; or <code>null</code>
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
