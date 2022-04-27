/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.service.invoker;


import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


/**
 * Container for HTTP request values extracted from an
 * {@link org.springframework.web.service.annotation.HttpExchange @HttpExchange}-annotated
 * method and argument values passed to it. This is then given to
 * {@link HttpClientAdapter} to adapt to the underlying HTTP client.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public final class HttpRequestValues {

	private static final MultiValueMap<String, String> EMPTY_COOKIES_MAP =
			CollectionUtils.toMultiValueMap(Collections.emptyMap());


	private final HttpMethod httpMethod;

	@Nullable
	private final URI uri;

	@Nullable
	private final String uriTemplate;

	private final Map<String, String> uriVariables;

	private final HttpHeaders headers;

	private final MultiValueMap<String, String> cookies;

	@Nullable
	private final Object bodyValue;

	@Nullable
	private final Publisher<?> body;

	@Nullable
	private final ParameterizedTypeReference<?> bodyElementType;


	private HttpRequestValues(HttpMethod httpMethod, @Nullable URI uri,
			@Nullable String uriTemplate, @Nullable Map<String, String> uriVariables,
			@Nullable HttpHeaders headers, @Nullable MultiValueMap<String, String> cookies,
			@Nullable Object bodyValue,
			@Nullable Publisher<?> body,
			@Nullable ParameterizedTypeReference<?> bodyElementType) {

		Assert.isTrue(uri == null || uriTemplate == null, "Expected either URI or URI template, not both");

		this.httpMethod = httpMethod;
		this.uri = uri;
		this.uriTemplate = (uri != null || uriTemplate != null ? uriTemplate : "");
		this.uriVariables = (uriVariables != null ? uriVariables : Collections.emptyMap());
		this.headers = (headers != null ? headers : HttpHeaders.EMPTY);
		this.cookies = (cookies != null ? cookies : EMPTY_COOKIES_MAP);
		this.bodyValue = bodyValue;
		this.body = body;
		this.bodyElementType = bodyElementType;
	}


	/**
	 * Return the HTTP method to use for the request.
	 */
	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	/**
	 * Return the full URL to use, if set.
	 * <p>This is mutually exclusive with {@link #getUriTemplate() uriTemplate}.
	 * One of the two has a value but not both.
	 */
	@Nullable
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return the URL template for the request, if set.
	 * <p>This is mutually exclusive with a {@link #getUri() full URL}.
	 * One of the two has a value but not both.
	 */
	@Nullable
	public String getUriTemplate() {
		return this.uriTemplate;
	}

	/**
	 * Return the URL template variables, or an empty map.
	 */
	public Map<String, String> getUriVariables() {
		return this.uriVariables;
	}

	/**
	 * Return the headers for the request, if any.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Return the cookies for the request, if any.
	 */
	public MultiValueMap<String, String> getCookies() {
		return this.cookies;
	}

	/**
	 * Return the request body as a value to be serialized, if set.
	 * <p>This is mutually exclusive with {@link #getBody()}.
	 * Only one of the two or neither is set.
	 */
	@Nullable
	public Object getBodyValue() {
		return this.bodyValue;
	}

	/**
	 * Return the request body as a Publisher.
	 * <p>This is mutually exclusive with {@link #getBodyValue()}.
	 * Only one of the two or neither is set.
	 */
	@Nullable
	public Publisher<?> getBody() {
		return this.body;
	}

	/**
	 * Return the element type for a {@link #getBody() Publisher body}.
	 */
	@Nullable
	public ParameterizedTypeReference<?> getBodyElementType() {
		return this.bodyElementType;
	}


	public static Builder builder(HttpMethod httpMethod) {
		return new Builder(httpMethod);
	}


	/**
	 * Builder for {@link HttpRequestValues}.
	 */
	public final static class Builder {

		private HttpMethod httpMethod;

		@Nullable
		private URI uri;

		@Nullable
		private String uriTemplate;

		@Nullable
		private Map<String, String> uriVariables;

		@Nullable
		private HttpHeaders headers;

		@Nullable
		private MultiValueMap<String, String> cookies;

		@Nullable
		private Object bodyValue;

		@Nullable
		private Publisher<?> body;

		@Nullable
		private ParameterizedTypeReference<?> bodyElementType;

		private Builder(HttpMethod httpMethod) {
			Assert.notNull(httpMethod, "HttpMethod is required");
			this.httpMethod = httpMethod;
		}

		/**
		 * Set the HTTP method for the request.
		 */
		public Builder setHttpMethod(HttpMethod httpMethod) {
			Assert.notNull(httpMethod, "HttpMethod is required");
			this.httpMethod = httpMethod;
			return this;
		}

		/**
		 * Set the request URL as a full URL.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@link #setUriTemplate(String)}.
		 */
		public Builder setUri(URI uri) {
			this.uri = uri;
			this.uriTemplate = null;
			return this;
		}

		/**
		 * Set the request URL as a String template.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@link #setUri(URI) full URI}.
		 */
		public Builder setUriTemplate(String uriTemplate) {
			this.uriTemplate = uriTemplate;
			this.uri = null;
			return this;
		}

		/**
		 * Add a URI variable name-value pair.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@link #setUri(URI) full URI}.
		 */
		public Builder setUriVariable(String name, String value) {
			this.uriVariables = (this.uriVariables != null ? this.uriVariables : new LinkedHashMap<>());
			this.uriVariables.put(name, value);
			this.uri = null;
			return this;
		}


		/**
		 * Set the media types for the request {@code Accept} header.
		 */
		public Builder setAccept(List<MediaType> acceptableMediaTypes) {
			initHeaders().setAccept(acceptableMediaTypes);
			return this;
		}

		/**
		 * Set the media type for the request {@code Content-Type} header.
		 */
		public Builder setContentType(MediaType contentType) {
			initHeaders().setContentType(contentType);
			return this;
		}

		/**
		 * Add the given header name and values.
		 */
		public Builder addHeader(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				initHeaders().add(headerName, headerValue);
			}
			return this;
		}

		private HttpHeaders initHeaders() {
			this.headers = (this.headers != null ? this.headers : new HttpHeaders());
			return this.headers;
		}

		/**
		 * Add the given cookie name and values.
		 */
		public Builder addCookie(String name, String... values) {
			this.cookies = (this.cookies != null ? this.cookies : new LinkedMultiValueMap<>());
			for (String value : values) {
				this.cookies.add(name, value);
			}
			return this;
		}

		/**
		 * Set the request body as a concrete value to be serialized.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@link #setBody(Publisher, ParameterizedTypeReference) body Publisher}.
		 */
		public void setBodyValue(Object bodyValue) {
			this.bodyValue = bodyValue;
			this.body = null;
			this.bodyElementType = null;
		}

		/**
		 * Set the request body as a concrete value to be serialized.
		 * <p>This is mutually exclusive with, and resets any previously set
		 * {@link #setBodyValue(Object) body value}.
		 */
		public <T, P extends Publisher<T>> void setBody(Publisher<P> body, ParameterizedTypeReference<?> elementTye) {
			this.body = body;
			this.bodyElementType = elementTye;
			this.bodyValue = null;
		}

		/**
		 * Builder the {@link HttpRequestValues} instance.
		 */
		public HttpRequestValues build() {
			return new HttpRequestValues(
					this.httpMethod, this.uri, this.uriTemplate, this.uriVariables,
					this.headers, this.cookies,
					this.bodyValue, this.body, this.bodyElementType);
		}

	}

}
