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

package org.springframework.web.client;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

/**
 * Common base class for exceptions that contain actual HTTP response data.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class RestClientResponseException extends RestClientException {

	private static final long serialVersionUID = -8803556342728481792L;

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	private final HttpStatusCode statusCode;

	private final String statusText;

	private final byte[] responseBody;

	@Nullable
	private final HttpHeaders responseHeaders;

	@Nullable
	private final String responseCharset;

	@Nullable
	private transient Function<ResolvableType, ?> bodyConvertFunction;


	/**
	 * Construct a new instance of with the given response data.
	 * @param statusCode the raw status code value
	 * @param statusText the status text
	 * @param headers the response headers (may be {@code null})
	 * @param responseBody the response body content (may be {@code null})
	 * @param responseCharset the response body charset (may be {@code null})
	 */
	public RestClientResponseException(
			String message, int statusCode, String statusText, @Nullable HttpHeaders headers,
			@Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		this(message, HttpStatusCode.valueOf(statusCode), statusText, headers, responseBody, responseCharset);
	}

	/**
	 * Construct a new instance of with the given response data.
	 * @param statusCode the raw status code value
	 * @param statusText the status text
	 * @param headers the response headers (may be {@code null})
	 * @param responseBody the response body content (may be {@code null})
	 * @param responseCharset the response body charset (may be {@code null})
	 * @since 6.0
	 */
	public RestClientResponseException(
			String message, HttpStatusCode statusCode, String statusText, @Nullable HttpHeaders headers,
			@Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(message);
		this.statusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = copyHeaders(headers);
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = (responseCharset != null ? responseCharset.name() : null);
	}

	/**
	 * Copies the given headers, because the backing map might not be
	 * serializable.
	 */
	@Nullable
	private static HttpHeaders copyHeaders(@Nullable HttpHeaders headers) {
		if (headers != null) {
			MultiValueMap<String, String> result =
					CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(headers.size(), Locale.ENGLISH));
			headers.forEach((name, values) -> values.forEach(value -> result.add(name, value)));
			return HttpHeaders.readOnlyHttpHeaders(result);
		}
		else {
			return null;
		}
	}


	/**
	 * Return the HTTP status code.
	 * @since 6.0
	 */
	public HttpStatusCode getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Return the raw HTTP status code value.
	 * @deprecated as of 6.0, in favor of {@link #getStatusCode()}
	 */
	@Deprecated(since = "6.0")
	public int getRawStatusCode() {
		return this.statusCode.value();
	}

	/**
	 * Return the HTTP status text.
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * Return the HTTP response headers.
	 */
	@Nullable
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}

	/**
	 * Return the response body as a byte array.
	 */
	public byte[] getResponseBodyAsByteArray() {
		return this.responseBody;
	}

	/**
	 * Return the response body converted to String. The charset used is that
	 * of the response "Content-Type" or otherwise {@code "UTF-8"}.
	 */
	public String getResponseBodyAsString() {
		return getResponseBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * Return the response body converted to String. The charset used is that
	 * of the response "Content-Type" or otherwise the one given.
	 * @param fallbackCharset the charset to use on if the response doesn't specify.
	 * @since 5.1.11
	 */
	public String getResponseBodyAsString(Charset fallbackCharset) {
		if (this.responseCharset == null) {
			return new String(this.responseBody, fallbackCharset);
		}
		try {
			return new String(this.responseBody, this.responseCharset);
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Convert the error response content to the specified type.
	 * @param targetType the type to convert to
	 * @param <E> the expected target type
	 * @return the converted object, or {@code null} if there is no content
	 * @since 6.0
	 */
	@Nullable
	public <E> E getResponseBodyAs(Class<E> targetType) {
		return getResponseBodyAs(ResolvableType.forClass(targetType));
	}

	/**
	 * Variant of {@link #getResponseBodyAs(Class)} with
	 * {@link ParameterizedTypeReference}.
	 * @since 6.0
	 */
	@Nullable
	public <E> E getResponseBodyAs(ParameterizedTypeReference<E> targetType) {
		return getResponseBodyAs(ResolvableType.forType(targetType.getType()));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <E> E getResponseBodyAs(ResolvableType targetType) {
		Assert.state(this.bodyConvertFunction != null, "Function to convert body not set");
		return (E) this.bodyConvertFunction.apply(targetType);
	}

	/**
	 * Provide a function to use to decode the response error content
	 * via {@link #getResponseBodyAs(Class)}.
	 * @param bodyConvertFunction the function to use
	 * @since 6.0
	 */
	public void setBodyConvertFunction(Function<ResolvableType, ?> bodyConvertFunction) {
		this.bodyConvertFunction = bodyConvertFunction;
	}

}
