/*
 * Copyright 2002-2014 the original author or authors.
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

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder for simple creation of a {@link HttpHeaders} instance.
 *
 * <p>Similar to {@link HttpHeaders}, this class offers the following convenience methods:
 * <ul>
 * <li>{@link #add(String, String)} adds a header value to the list of values for a header name</li>
 * <li>{@link #set(String, String)} sets the header value to a single string value</li>
 * </ul>
 *
 * @author Mattias Severson
 * @since 4.1
 */
public class HttpHeadersBuilder {

	private final HttpHeaders httpHeaders;


	/**
	 * Constructs a new, empty instance of the {@code HttpHeadersBuilder} object.
	 */
	public HttpHeadersBuilder() {
		this.httpHeaders = new HttpHeaders();
	}


	/**
	 * Set the list of acceptable {@linkplain MediaType media types}, as specified by the
	 * {@code Accept} header.
	 * @param acceptableMediaTypes the acceptable media types
	 * @return this builder
	 */
	public HttpHeadersBuilder setAccept(List<MediaType> acceptableMediaTypes) {
		this.httpHeaders.setAccept(acceptableMediaTypes);
		return this;
	}

	/**
	 * Set the list of acceptable {@linkplain java.nio.charset.Charset charsets}, as
	 * specified by the {@code Accept-Charset} header.
	 * @param acceptableCharsets the acceptable charsets
	 * @return this builder
	 */
	public HttpHeadersBuilder setAcceptCharset(List<Charset> acceptableCharsets) {
		this.httpHeaders.setAcceptCharset(acceptableCharsets);
		return this;
	}

	/**
	 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified by the
	 * {@code Allow} header.
	 * @param allowedMethods the allowed methods
	 * @return this builder
	 */
	public HttpHeadersBuilder setAllow(Set<HttpMethod> allowedMethods) {
		this.httpHeaders.setAllow(allowedMethods);
		return this;
	}

	/**
	 * Set the (new) value of the {@code Cache-Control} header.
	 * @param cacheControl the value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setCacheControl(String cacheControl) {
		this.httpHeaders.setCacheControl(cacheControl);
		return this;
	}

	/**
	 * Set the (new) value of the {@code Connection} header.
	 * @param connection the value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setConnection(String connection) {
		this.httpHeaders.setConnection(connection);
		return this;
	}

	/**
	 * Set the (new) value of the {@code Connection} header.
	 * @param connection the value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setConnection(List<String> connection) {
		this.httpHeaders.setConnection(connection);
		return this;
	}

	/**
	 * Set the (new) value of the {@code Content-Disposition} header for {@code form-data}.
	 * @param name the control name
	 * @param filename the filename, may be {@code null}
	 * @return this builder
	 */
	public HttpHeadersBuilder setContentDispositionFormData(String name, String filename) {
		this.httpHeaders.setContentDispositionFormData(name, filename);
		return this;
	}

	/**
	 * Set the length of the body in bytes, as specified by the {@code Content-Length} header.
	 * @param contentLength the content length
	 * @return this builder
	 */
	public HttpHeadersBuilder setContentLength(long contentLength) {
		this.httpHeaders.setContentLength(contentLength);
		return this;
	}

	/**
	 * Set the {@linkplain MediaType media type} of the body, as specified by the
	 * {@code Content-Type} header.
	 * @param mediaType the media type
	 * @return this builder
	 */
	public HttpHeadersBuilder setContentType(MediaType mediaType) {
		this.httpHeaders.setContentType(mediaType);
		return this;
	}

	/**
	 * Set the date and time at which the message was created, as specified by the
	 * {@code Date} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param date the date
	 * @return this builder
	 */
	public HttpHeadersBuilder setDate(long date) {
		this.httpHeaders.setDate(date);
		return this;
	}

	/**
	 * Set the (new) entity tag of the body, as specified by the {@code ETag} header.
	 * @param eTag the new entity tag
	 * @return this builder
	 */
	public HttpHeadersBuilder setETag(String eTag) {
		this.httpHeaders.setETag(eTag);
		return this;
	}

	/**
	 * Set the date and time at which the message is no longer valid, as specified by the
	 * {@code Expires} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param expires the new expires header value
	 * @return this builder
	 */
	public HttpHeadersBuilder setExpires(long expires) {
		this.httpHeaders.setExpires(expires);
		return this;
	}

	/**
	 * Set the (new) value of the {@code If-Modified-Since} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param ifModifiedSince the new value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setIfModifiedSince(long ifModifiedSince) {
		this.httpHeaders.setIfModifiedSince(ifModifiedSince);
		return this;
	}

	/**
	 * Set the (new) value of the {@code If-None-Match} header.
	 * @param ifNoneMatch the new value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setIfNoneMatch(String ifNoneMatch) {
		this.httpHeaders.setIfNoneMatch(ifNoneMatch);
		return this;
	}

	/**
	 * Set the (new) values of the {@code If-None-Match} header.
	 * @param ifNoneMatchList the new value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setIfNoneMatch(List<String> ifNoneMatchList) {
		this.httpHeaders.setIfNoneMatch(ifNoneMatchList);
		return this;
	}

	/**
	 * Set the time the resource was last changed, as specified by the
	 * {@code Last-Modified} header.
	 * <p>The date should be specified as the number of milliseconds since January 1, 1970 GMT.
	 * @param lastModified the last modified date
	 * @return this builder
	 */
	public HttpHeadersBuilder setLastModified(long lastModified) {
		this.httpHeaders.setLastModified(lastModified);
		return this;
	}

	/**
	 * Set the (new) location of a resource, as specified by the {@code Location} header.
	 * @param location the location
	 * @return this builder
	 */
	public HttpHeadersBuilder setLocation(URI location) {
		this.httpHeaders.setLocation(location);
		return this;
	}

	/**
	 * Sets the (new) value of the {@code Origin} header.
	 * @param origin the value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setOrigin(String origin) {
		this.httpHeaders.setOrigin(origin);
		return this;
	}

	/**
	 * Set the (new) value of the {@code Pragma} header.
	 * @param pragma the value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setPragma(String pragma) {
		this.httpHeaders.setPragma(pragma);
		return this;
	}

	/**
	 * Set the (new) value of the {@code Upgrade} header.
	 * @param upgrade the value of the header
	 * @return this builder
	 */
	public HttpHeadersBuilder setUpgrade(String upgrade) {
		this.httpHeaders.setUpgrade(upgrade);
		return this;
	}

	/**
	 * Set the given date under the given header name after formatting it as a string
	 * using the pattern {@code "EEE, dd MMM yyyy HH:mm:ss zzz"}.
	 * @return this builder
	 */
	public HttpHeadersBuilder setDate(String headerName, long date) {
		this.httpHeaders.setDate(headerName, date);
		return this;
	}

	/**
	 * Add the given, single header value under the given name.
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @see #put(String, java.util.List)
	 * @see #set(String, String)
	 * @return this builder
	 */
	public HttpHeadersBuilder add(String headerName, String headerValue) {
		this.httpHeaders.add(headerName, headerValue);
		return this;
	}

	/**
	 * Set the given, single header value under the given name.
	 * @param headerName  the header name
	 * @param headerValue the header value
	 * @see #put(String, java.util.List)
	 * @see #add(String, String)
	 * @return this builder
	 */
	public HttpHeadersBuilder set(String headerName, String headerValue) {
		this.httpHeaders.set(headerName, headerValue);
		return this;
	}

	/**
	 * Set the given map keys as header values under the given map keys.
	 * @param values the header values
	 * @return this builder
	 */
	public HttpHeadersBuilder setAll(Map<String, String> values) {
		this.httpHeaders.setAll(values);
		return this;
	}

	/**
	 * Set the given list as header value under the given name.
	 * @param headerName the header name
	 * @param headerValues the header values
	 * @return this builder
	 */
	public HttpHeadersBuilder put(String headerName, List<String> headerValues) {
		this.httpHeaders.put(headerName, headerValues);
		return this;
	}

	/**
	 * Return the {@code HttpHeaders} from this builder.
	 * @return the {@code HttpHeaders} instance
	 */
	public HttpHeaders build() {
		return httpHeaders;
	}

}
