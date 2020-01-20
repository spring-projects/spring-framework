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

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Extension of {@link HttpEntity} that adds a {@linkplain HttpMethod method} and
 * {@linkplain URI uri} or String based uri template with placeholder. Uri variables can be provided
 * through the builder {@link HeadersBuilder#uriVariables(Object...)}.
 *
 * Used in {@code RestTemplate} and {@code @Controller} methods.
 *
 * <p>In {@code RestTemplate}, this class is used as parameter in
 * {@link org.springframework.web.client.RestTemplate#exchange(RequestEntity, Class) exchange()}:
 * <pre class="code">
 * MyRequest body = ...
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity
 *     .post(new URI(&quot;https://example.com/bar&quot;))
 *     .accept(MediaType.APPLICATION_JSON)
 *     .body(body);
 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
 * </pre>
 *
 * <p>If you would like to provide a URI template with variables, consider using
 * {@link org.springframework.web.util.DefaultUriBuilderFactory DefaultUriBuilderFactory}:
 * <pre class="code">
 * // Create shared factory
 * UriBuilderFactory factory = new DefaultUriBuilderFactory();
 *
 * // Use factory to create URL from template
 * URI uri = factory.uriString(&quot;https://example.com/{foo}&quot;).build(&quot;bar&quot;);
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity.post(uri).accept(MediaType.APPLICATION_JSON).body(body);
 * </pre>
 *
 * <p> if you would like to provide a string base URI template with variable, consider using
 * {@link RequestEntity#method(HttpMethod, String)}}, {@link RequestEntity#get(String)},
 * {@link RequestEntity#post(String)}, {@link RequestEntity#put(String)}, {@link RequestEntity#delete(String)},
 * {@link RequestEntity#patch(String)}, {@link RequestEntity#options(String)}
 *
 * <pre class="code>
 *    RequestEntity  request = RequestEntity
 *    		.post(&quot;https://example.com/{foo}&quot;)
 *    	    .uriVariables(&quot;bar&quot;)
 *          .accept(MediaType.APPLICATION_JSON)
 *          .build();
 * </pre>
 *
 * <p>Can also be used in Spring MVC, as a parameter in a @Controller method:
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
 */
public class RequestEntity<T> extends HttpEntity<T> {

	private final static UriTemplateHandler DEFAULT_URI_BUILDER_FACTORY = new DefaultUriBuilderFactory();

	@Nullable
	private final HttpMethod method;

	@Nullable
	private final URI url;

	@Nullable
	private String uri;

	@Nullable
	private Object[] uriVariables;

	@Nullable
	private final Type type;


	/**
	 * Constructor with method and URL but without body nor headers.
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(HttpMethod method, URI url) {
		this(null, null, method, url, null);
	}

	/**
	 * Constructor with method, URL and body but without headers.
	 * @param body the body
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(@Nullable T body, HttpMethod method, URI url) {
		this(body, null, method, url, null);
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
		this(body, null, method, url, type);
	}

	/**
	 * Constructor with method, URL and headers but without body.
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(MultiValueMap<String, String> headers, HttpMethod method, URI url) {
		this(null, headers, method, url, null);
	}

	/**
	 * Constructor with method, URL, headers and body.
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
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
	 */
	public RequestEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers,
						 @Nullable HttpMethod method, URI url, @Nullable Type type) {

		super(body, headers);
		this.method = method;
		this.url = url;
		this.type = type;
	}

	/**
	 * Private Constructor with method, URL, UriTemplate and varargs urivariables but without body nor headers.
	 * @param method the method
	 * @param url the URL
	 * @param uri the UriTemplate
	 * @param uriVariables the uriVariables
	 */
	private RequestEntity(MultiValueMap<String, String> headers, HttpMethod method, @Nullable URI url,
						  @Nullable  String uri, @Nullable Object... uriVariables) {
		super(null, headers);
		Assert.isTrue(uri == null || url == null, "Either url or url must be not null");
		this.method = method;
		this.url = url;
		this.type = null;
		this.uri = uri;
		this.uriVariables = uriVariables;
	}


	/**
	 * Return the HTTP method of the request.
	 * @return the HTTP method as an {@code HttpMethod} enum value
	 */
	@Nullable
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * Return the URL of the request.
	 * Used  {@link org.springframework.web.util.DefaultUriBuilderFactory} to expand and
	 * encode {@link DefaultUriBuilderFactory#setEncodingMode} when provided {@link RequestEntity#uri}
	 * @return the URL as a {@code URI}
	 */
	public URI getUrl() {
		if (uri == null) {
			return this.url;
		}
		return DEFAULT_URI_BUILDER_FACTORY.expand(uri, uriVariables);
	}

	/**
	 * Return the URL of the request.
	 * @return the URL as a {@code URI}
	 * @since 5.3
	 */
	public URI getUrl(UriTemplateHandler uriTemplateHandler) {
		if (uri == null) {
			return this.url;
		}
		return uriTemplateHandler.expand(uri, uriVariables);
	}


	/**
	 * Return the type of the request's body.
	 * @return the request's body type, or {@code null} if not known
	 * @since 4.3
	 */
	@Nullable
	public Type getType() {
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
		RequestEntity<?> otherEntity = (RequestEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(getMethod(), otherEntity.getMethod()) &&
				ObjectUtils.nullSafeEquals(getUrl(), otherEntity.getUrl()));
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
		StringBuilder builder = new StringBuilder("<");
		builder.append(getMethod());
		builder.append(' ');
		builder.append(getUrl());
		builder.append(',');
		T body = getBody();
		HttpHeaders headers = getHeaders();
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
	 * Create a builder with the given method and given string base uri template.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param uri the uri
	 * @return the created builder
	 * @see RequestEntity
	 * @since 5.3
	 */
	public static BodyBuilder method(HttpMethod method, String uri) {
		return new DefaultBodyBuilder(method, uri);
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
	 * @param uri the uri template
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> get(String uri) {
		return method(HttpMethod.GET, uri);
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
	 * @param uri the uri template
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> head(String uri) {
		return method(HttpMethod.HEAD, uri);
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
	 * @param uri the uri template
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder post(String uri) {
		return method(HttpMethod.POST, uri);
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
	 * @param uri the uri template
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder put(String uri) {
		return method(HttpMethod.PUT, uri);
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
	 * @param uri the uri template
	 * @return the created builder
	 * @since 5.3
	 */
	public static BodyBuilder patch(String uri) {
		return method(HttpMethod.PATCH, uri);
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
	 * @param uri the uri template
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> delete(String uri) {
		return method(HttpMethod.DELETE, uri);
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
	 * @param uri the uri template
	 * @return the created builder
	 * @since 5.3
	 */
	public static HeadersBuilder<?> options(String uri) {
		return method(HttpMethod.OPTIONS, uri);
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
		 * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
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
		 * Set the values of the {@code If-None-Match} header.
		 * @param uriVariables the variables to expand the template
		 * @since 5.3
		 */
		B uriVariables(Object... uriVariables);

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

		@Nullable
		private final URI url;

		@Nullable
		private final String uri;

		@Nullable
		private  Object[] uriVariables;


		private final HttpHeaders headers = new HttpHeaders();

		public DefaultBodyBuilder(HttpMethod method, URI url) {
			this.method = method;
			this.url = url;
			this.uri = null;
		}

		public DefaultBodyBuilder(HttpMethod method, String uri) {
			this.method = method;
			this.uri = uri;
			this.url = null;
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
		public BodyBuilder uriVariables(Object... uriVariables) {
			this.uriVariables = uriVariables;
			return this;
		}

		@Override
		public RequestEntity<Void> build() {
			if (this.url != null){
				new RequestEntity<>(this.headers, this.method, this.url);
			}
			return new RequestEntity<>(this.headers, this.method, this.url, uri, uriVariables);
		}

		@Override
		public <T> RequestEntity<T> body(T body) {
			return new RequestEntity<>(body, this.headers, this.method, this.url);
		}

		@Override
		public <T> RequestEntity<T> body(T body, Type type) {
			return new RequestEntity<>(body, this.headers, this.method, this.url, type);
		}
	}

}
