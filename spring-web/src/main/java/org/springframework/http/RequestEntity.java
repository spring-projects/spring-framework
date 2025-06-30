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

package org.springframework.http;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Extension of {@link HttpEntity} that also exposes the HTTP method and the
 * target URL. For use in the {@code RestTemplate} to prepare requests with
 * and in {@code @Controller} methods to represent request input.
 *
 * <p>Example use with the {@code RestTemplate}:
 * <pre class="code">
 * MyRequest body = ...
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity
 *     .post(&quot;https://example.com/{foo}&quot;, &quot;bar&quot;)
 *     .accept(MediaType.APPLICATION_JSON)
 *     .body(body);
 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
 * </pre>
 *
 * <p>Example use in an {@code @Controller}:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public void handle(RequestEntity&lt;String&gt; request) {
 *   HttpMethod method = request.getMethod();
 *   URI url = request.getUrl();
 *   String body = request.getBody();
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Parviz Rozikov
 * @since 4.1
 * @param <T> the body type
 * @see #getMethod()
 * @see #getUrl()
 * @see org.springframework.web.client.RestOperations#exchange(RequestEntity, Class)
 * @see ResponseEntity
 */
public class RequestEntity<T> extends HttpEntity<T> {

	private final @Nullable HttpMethod method;

	private final @Nullable URI url;

	private final @Nullable Type type;

	/**
	 * Constructor with method and URL but without body nor headers.
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(HttpMethod method, URI url) {
		this(null, (HttpHeaders) null, method, url, null);
	}

	/**
	 * Constructor with method, URL and body but without headers.
	 * @param body the body
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(@Nullable T body, HttpMethod method, URI url) {
		this(body, (HttpHeaders) null, method, url, null);
	}

	/**
	 * Constructor with method, URL, body and type but without headers.
	 * @param body the body
	 * @param method the method
	 * @param url the URL
	 * @param type the type used for generic type resolution
	 * @since 4.3
	 */
	public RequestEntity(@Nullable T body, HttpMethod method, URI url, Type type) {
		this(body, (HttpHeaders) null, method, url, type);
	}

	/**
	 * Constructor with method, URL and headers but without body.
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @since 7.0
	 */
	public RequestEntity(HttpHeaders headers, HttpMethod method, URI url) {
		this(null, headers, method, url, null);
	}

	/**
	 * Constructor with method, URL, headers and body.
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @since 7.0
	 */
	public RequestEntity(@Nullable T body, @Nullable HttpHeaders headers,
			@Nullable HttpMethod method, URI url) {

		this(body, headers, method, url, null);
	}

	/**
	 * Constructor with method, URL, headers, body and type.
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @param type the type used for generic type resolution
	 * @since 7.0
	 */
	public RequestEntity(@Nullable T body, @Nullable HttpHeaders headers,
			@Nullable HttpMethod method, @Nullable URI url, @Nullable Type type) {

		super(body, headers);
		this.method = method;
		this.url = url;
		this.type = type;
	}

	/**
	 * Constructor with method, URL and headers but without body.
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @deprecated in favor of {@link #RequestEntity(HttpHeaders, HttpMethod, URI)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public RequestEntity(MultiValueMap<String, String> headers, HttpMethod method, URI url) {
		this(null, headers, method, url, null);
	}

	/**
	 * Constructor with method, URL, headers and body.
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @deprecated in favor of {@link #RequestEntity(Object, HttpHeaders, HttpMethod, URI)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public RequestEntity(
			@Nullable T body, @Nullable MultiValueMap<String, String> headers,
			@Nullable HttpMethod method, URI url) {

		this(body, headers, method, url, null);
	}

	/**
	 * Constructor with method, URL, headers, body and type.
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @param type the type used for generic type resolution
	 * @since 4.3
	 * @deprecated in favor of {@link #RequestEntity(Object, HttpHeaders, HttpMethod, URI, Type)}
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
			@Nullable HttpMethod method, @Nullable URI url, @Nullable Type type) {

		super(body, headers);
		this.method = method;
		this.url = url;
		this.type = type;
	}


	/**
	 * Return the HTTP method of the request.
	 * @return the HTTP method as an {@code HttpMethod} enum value
	 */
	public @Nullable HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * Return the {@link URI} for the target HTTP endpoint.
	 * <p><strong>Note:</strong> This method raises
	 * {@link UnsupportedOperationException} if the {@code RequestEntity} was
	 * created with a URI template and variables rather than with a {@link URI}
	 * instance. This is because a URI cannot be created without further input
	 * on how to expand template and encode the URI. In such cases, the
	 * {@code URI} is prepared by the
	 * {@link org.springframework.web.client.RestTemplate} with the help of the
	 * {@link org.springframework.web.util.UriTemplateHandler} it is configured with.
	 */
	public URI getUrl() {
		if (this.url == null) {
			throw new UnsupportedOperationException(
					"The RequestEntity was created with a URI template and variables, " +
							"and there is not enough information on how to correctly expand and " +
							"encode the URI template. This will be done by the RestTemplate instead " +
							"with help from the UriTemplateHandler it is configured with.");
		}
		return this.url;
	}


	/**
	 * Return the type of the request's body.
	 * @return the request's body type, or {@code null} if not known
	 * @since 4.3
	 */
	public @Nullable Type getType() {
		if (this.type == null) {
			T body = getBody();
			if (body != null) {
				return body.getClass();
			}
		}
		return this.type;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		return (other instanceof RequestEntity<?> otherEntity &&
				ObjectUtils.nullSafeEquals(this.method, otherEntity.method) &&
				ObjectUtils.nullSafeEquals(this.url, otherEntity.url));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.method);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.url);
		return hashCode;
	}

	@Override
	public String toString() {
		return format(getMethod(), getUrl().toString(), getBody(), getHeaders());
	}

	static <T> String format(@Nullable HttpMethod httpMethod, String url, @Nullable T body, HttpHeaders headers) {
		StringBuilder builder = new StringBuilder("<");
		builder.append(httpMethod);
		builder.append(' ');
		builder.append(url);
		builder.append(',');
		if (body != null) {
			builder.append(body);
			builder.append(',');
		}
		builder.append(headers);
		builder.append('>');
		return builder.toString();
	}


	// Static builder methods

	/**
	 * Create a builder with the given method and url.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the URL
	 * @return the created builder
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		return new DefaultBodyBuilder(method, url);
	}

	/**
	 * Create a builder with the given HTTP method, URI template, and variables.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder method(HttpMethod method, String uriTemplate, @Nullable Object... uriVariables) {
		return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
	}

	/**
	 * Create a builder with the given HTTP method, URI template, and variables.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param uriTemplate the uri template to use
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder method(HttpMethod method, String uriTemplate, Map<String, ?> uriVariables) {
		return new DefaultBodyBuilder(method, uriTemplate, uriVariables);
	}


	/**
	 * Create an HTTP GET builder with the given url.
	 * @param url the URL
	 * @return the created builder
	 */
	public static HeadersBuilder<?> get(URI url) {
		return method(HttpMethod.GET, url);
	}

	/**
	 * Create an HTTP GET builder with the given string base uri template.
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> get(String uriTemplate, @Nullable Object... uriVariables) {
		return method(HttpMethod.GET, uriTemplate, uriVariables);
	}

	/**
	 * Create an HTTP HEAD builder with the given url.
	 * @param url the URL
	 * @return the created builder
	 */
	public static HeadersBuilder<?> head(URI url) {
		return method(HttpMethod.HEAD, url);
	}

	/**
	 * Create an HTTP HEAD builder with the given string base uri template.
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> head(String uriTemplate, @Nullable Object... uriVariables) {
		return method(HttpMethod.HEAD, uriTemplate, uriVariables);
	}

	/**
	 * Create an HTTP POST builder with the given url.
	 * @param url the URL
	 * @return the created builder
	 */
	public static BodyBuilder post(URI url) {
		return method(HttpMethod.POST, url);
	}

	/**
	 * Create an HTTP POST builder with the given string base uri template.
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder post(String uriTemplate, @Nullable Object... uriVariables) {
		return method(HttpMethod.POST, uriTemplate, uriVariables);
	}

	/**
	 * Create an HTTP PUT builder with the given url.
	 * @param url the URL
	 * @return the created builder
	 */
	public static BodyBuilder put(URI url) {
		return method(HttpMethod.PUT, url);
	}

	/**
	 * Create an HTTP PUT builder with the given string base uri template.
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder put(String uriTemplate, @Nullable Object... uriVariables) {
		return method(HttpMethod.PUT, uriTemplate, uriVariables);
	}

	/**
	 * Create an HTTP PATCH builder with the given url.
	 * @param url the URL
	 * @return the created builder
	 */
	public static BodyBuilder patch(URI url) {
		return method(HttpMethod.PATCH, url);
	}

	/**
	 * Create an HTTP PATCH builder with the given string base uri template.
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder patch(String uriTemplate, @Nullable Object... uriVariables) {
		return method(HttpMethod.PATCH, uriTemplate, uriVariables);
	}

	/**
	 * Create an HTTP DELETE builder with the given url.
	 * @param url the URL
	 * @return the created builder
	 */
	public static HeadersBuilder<?> delete(URI url) {
		return method(HttpMethod.DELETE, url);
	}

	/**
	 * Create an HTTP DELETE builder with the given string base uri template.
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> delete(String uriTemplate, @Nullable Object... uriVariables) {
		return method(HttpMethod.DELETE, uriTemplate, uriVariables);
	}

	/**
	 * Creates an HTTP OPTIONS builder with the given url.
	 * @param url the URL
	 * @return the created builder
	 */
	public static HeadersBuilder<?> options(URI url) {
		return method(HttpMethod.OPTIONS, url);
	}

	/**
	 * Creates an HTTP OPTIONS builder with the given string base uri template.
	 * @param uriTemplate the uri template to use
	 * @param uriVariables variables to expand the URI template with
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> options(String uriTemplate, @Nullable Object... uriVariables) {
		return method(HttpMethod.OPTIONS, uriTemplate, uriVariables);
	}


	/**
	 * Defines a builder that adds headers to the request entity.
	 * @param <B> the builder subclass
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 * @param headers the existing HttpHeaders to copy from
		 * @return this builder
		 * @since 5.2
		 * @see HttpHeaders#add(String, String)
		 */
		B headers(@Nullable HttpHeaders headers);

		/**
		 * Manipulate this entity's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(String) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 * @since 5.2
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Set the list of acceptable {@linkplain MediaType media types}, as
		 * specified by the {@code Accept} header.
		 * @param acceptableMediaTypes the acceptable media types
		 */
		B accept(MediaType... acceptableMediaTypes);

		/**
		 * Set the list of acceptable {@linkplain Charset charsets}, as specified
		 * by the {@code Accept-Charset} header.
		 * @param acceptableCharsets the acceptable charsets
		 */
		B acceptCharset(Charset... acceptableCharsets);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * @param ifModifiedSince the new value of the header
		 * @since 5.1.4
		 */
		B ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * @param ifModifiedSince the new value of the header
		 * @since 5.1.4
		 */
		B ifModifiedSince(Instant ifModifiedSince);

		/**
		 * Set the value of the {@code If-Modified-Since} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifModifiedSince the new value of the header
		 */
		B ifModifiedSince(long ifModifiedSince);

		/**
		 * Set the values of the {@code If-None-Match} header.
		 * @param ifNoneMatches the new value of the header
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * Builds the request entity with no body.
		 * @return the request entity
		 * @see BodyBuilder#body(Object)
		 */
		RequestEntity<Void> build();
	}


	/**
	 * Defines a builder that adds a body to the response entity.
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * Set the body of the request entity and build the RequestEntity.
		 * @param <T> the type of the body
		 * @param body the body of the request entity
		 * @return the built request entity
		 */
		<T> RequestEntity<T> body(T body);

		/**
		 * Set the body and type of the request entity and build the RequestEntity.
		 * @param <T> the type of the body
		 * @param body the body of the request entity
		 * @param type the type of the body, useful for generic type resolution
		 * @return the built request entity
		 * @since 4.3
		 */
		<T> RequestEntity<T> body(T body, Type type);
	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		private final HttpMethod method;

		private final HttpHeaders headers = new HttpHeaders();

		private final @Nullable URI uri;

		private final @Nullable String uriTemplate;

		private final @Nullable Object @Nullable [] uriVarsArray;

		private final @Nullable Map<String, ? extends @Nullable Object> uriVarsMap;

		DefaultBodyBuilder(HttpMethod method, URI url) {
			this.method = method;
			this.uri = url;
			this.uriTemplate = null;
			this.uriVarsArray = null;
			this.uriVarsMap = null;
		}

		DefaultBodyBuilder(HttpMethod method, String uriTemplate, @Nullable Object... uriVars) {
			this.method = method;
			this.uri = null;
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = uriVars;
			this.uriVarsMap = null;
		}

		DefaultBodyBuilder(HttpMethod method, String uriTemplate, Map<String, ? extends @Nullable Object> uriVars) {
			this.method = method;
			this.uri = null;
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = null;
			this.uriVarsMap = uriVars;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(@Nullable HttpHeaders headers) {
			if (headers != null) {
				this.headers.putAll(headers);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(this.headers);
			return this;
		}

		@Override
		public BodyBuilder accept(MediaType... acceptableMediaTypes) {
			this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
			this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(ZonedDateTime ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(Instant ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(long ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public RequestEntity<Void> build() {
			return buildInternal(null, null);
		}

		@Override
		public <T> RequestEntity<T> body(T body) {
			return buildInternal(body, null);
		}

		@Override
		public <T> RequestEntity<T> body(T body, Type type) {
			return buildInternal(body, type);
		}

		private <T> RequestEntity<T> buildInternal(@Nullable T body, @Nullable Type type) {
			if (this.uri != null) {
				return new RequestEntity<>(body, this.headers, this.method, this.uri, type);
			}
			else if (this.uriTemplate != null){
				return new UriTemplateRequestEntity<>(body, this.headers, this.method, type,
						this.uriTemplate, this.uriVarsArray, this.uriVarsMap);
			}
			else {
				throw new IllegalStateException("Neither URI nor URI template");
			}
		}
	}


	/**
	 * RequestEntity initialized with a URI template and variables instead of a {@link URI}.
	 * @since 5.3
	 * @param <T> the body type
	 */
	public static class UriTemplateRequestEntity<T> extends RequestEntity<T> {

		private final String uriTemplate;

		private final @Nullable Object @Nullable [] uriVarsArray;

		private final @Nullable Map<String, ? extends @Nullable Object> uriVarsMap;

		UriTemplateRequestEntity(
				@Nullable T body, @Nullable HttpHeaders headers,
				@Nullable HttpMethod method, @Nullable Type type, String uriTemplate,
				@Nullable Object @Nullable [] uriVarsArray, @Nullable Map<String, ?> uriVarsMap) {

			super(body, headers, method, null, type);
			this.uriTemplate = uriTemplate;
			this.uriVarsArray = uriVarsArray;
			this.uriVarsMap = uriVarsMap;
		}

		public String getUriTemplate() {
			return this.uriTemplate;
		}

		public @Nullable Object @Nullable [] getVars() {
			return this.uriVarsArray;
		}

		public @Nullable Map<String, ? extends @Nullable Object> getVarsMap() {
			return this.uriVarsMap;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return (other instanceof UriTemplateRequestEntity<?> otherEntity &&
					ObjectUtils.nullSafeEquals(this.uriTemplate, otherEntity.uriTemplate) &&
					ObjectUtils.nullSafeEquals(this.uriVarsArray, otherEntity.uriVarsArray) &&
					ObjectUtils.nullSafeEquals(this.uriVarsMap, otherEntity.uriVarsMap));
		}

		@Override
		public int hashCode() {
			return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.uriTemplate));
		}

		@Override
		public String toString() {
			return format(getMethod(), getUriTemplate(), getBody(), getHeaders());
		}
	}

}
