/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.mock.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Mock implementation of {@link ServerHttpRequest}.
 *
 * <p><strong>Note:</strong> this class extends the same
 * {@link AbstractServerHttpRequest} base class as actual server-specific
 * implementation and is therefore read-only once created. Use static builder
 * methods in this class to build up request instances.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpMethod httpMethod;

	private final String contextPath;

	private final MultiValueMap<String, HttpCookie> cookies;

	private final InetSocketAddress remoteAddress;

	private final Flux<DataBuffer> body;


	private MockServerHttpRequest(HttpMethod httpMethod, URI uri, String contextPath,
			HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies,
			InetSocketAddress remoteAddress,
			Publisher<? extends DataBuffer> body) {

		super(uri, headers);
		this.httpMethod = httpMethod;
		this.contextPath = (contextPath != null ? contextPath : "");
		this.cookies = cookies;
		this.remoteAddress = remoteAddress;
		this.body = Flux.from(body);
	}


	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	@Override
	public Optional<InetSocketAddress> getRemoteAddress() {
		return Optional.ofNullable(this.remoteAddress);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		return this.cookies;
	}


	/**
	 * Shortcut to wrap the request with a {@code MockServerWebExchange}.
	 */
	public MockServerWebExchange toExchange() {
		return new MockServerWebExchange(this);
	}


	// Static builder methods

	/**
	 * Create a builder with the given HTTP method and a {@link URI}.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the URL
	 * @return the created builder
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		return new DefaultBodyBuilder(method, url);
	}

	/**
	 * Alternative to {@link #method(HttpMethod, URI)} that accepts a URI template.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param urlTemplate the URL template
	 * @param vars variables to expand into the template
	 * @return the created builder
	 */
	public static BodyBuilder method(HttpMethod method, String urlTemplate, Object... vars) {
		URI url = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(vars).encode().toUri();
		return new DefaultBodyBuilder(method, url);
	}

	/**
	 * Create an HTTP GET builder with the given url.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> get(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.GET, urlTemplate, uriVars);
	}

	/**
	 * Create an HTTP HEAD builder with the given url.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> head(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.HEAD, urlTemplate, uriVars);
	}

	/**
	 * Create an HTTP POST builder with the given url.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BodyBuilder post(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.POST, urlTemplate, uriVars);
	}

	/**
	 * Create an HTTP PUT builder with the given url.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BodyBuilder put(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PUT, urlTemplate, uriVars);
	}

	/**
	 * Create an HTTP PATCH builder with the given url.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BodyBuilder patch(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PATCH, urlTemplate, uriVars);
	}

	/**
	 * Create an HTTP DELETE builder with the given url.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> delete(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.DELETE, urlTemplate, uriVars);
	}

	/**
	 * Creates an HTTP OPTIONS builder with the given url.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> options(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.OPTIONS, urlTemplate, uriVars);
	}


	/**
	 * Request builder exposing properties not related to the body.
	 * @param <B> the builder sub-class
	 */
	public interface BaseBuilder<B extends BaseBuilder<B>> {

		/**
		 * Set the contextPath to return.
		 */
		B contextPath(String contextPath);

		/**
		 * Set the remote address to return.
		 */
		B remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * Add one or more cookies.
		 */
		B cookie(String path, HttpCookie... cookie);

		/**
		 * Add the given cookies.
		 * @param cookies the cookies.
		 */
		B cookies(MultiValueMap<String, HttpCookie> cookies);

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * Add the given header values.
		 * @param headers the header values
		 */
		B headers(MultiValueMap<String, String> headers);

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
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifModifiedSince the new value of the header
		 */
		B ifModifiedSince(long ifModifiedSince);

		/**
		 * Set the (new) value of the {@code If-Unmodified-Since} header.
		 * <p>The date should be specified as the number of milliseconds since
		 * January 1, 1970 GMT.
		 * @param ifUnmodifiedSince the new value of the header
		 * @see HttpHeaders#setIfUnmodifiedSince(long)
		 */
		B ifUnmodifiedSince(long ifUnmodifiedSince);

		/**
		 * Set the values of the {@code If-None-Match} header.
		 * @param ifNoneMatches the new value of the header
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * Set the (new) value of the Range header.
		 * @param ranges the HTTP ranges
		 * @see HttpHeaders#setRange(List)
		 */
		B range(HttpRange... ranges);

		/**
		 * Builds the request with no body.
		 * @return the request
		 * @see BodyBuilder#body(Publisher)
		 * @see BodyBuilder#body(String)
		 */
		MockServerHttpRequest build();

		/**
		 * Shortcut for:<br>
		 * {@code build().toExchange()}
		 */
		MockServerWebExchange toExchange();
	}

	/**
	 * A builder that adds a body to the request.
	 */
	public interface BodyBuilder extends BaseBuilder<BodyBuilder> {

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
		 * Set the body of the request and build it.
		 * @param body the body
		 * @return the built request entity
		 */
		MockServerHttpRequest body(Publisher<? extends DataBuffer> body);

		/**
		 * Set the body of the request and build it.
		 * <p>The String is assumed to be UTF-8 encoded unless the request has a
		 * "content-type" header with a charset attribute.
		 * @param body the body as text
		 * @return the built request entity
		 */
		MockServerHttpRequest body(String body);

	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		private final HttpMethod method;

		private final URI url;

		private String contextPath;

		private final HttpHeaders headers = new HttpHeaders();

		private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

		private InetSocketAddress remoteAddress;


		public DefaultBodyBuilder(HttpMethod method, URI url) {
			this.method = method;
			this.url = url;
		}

		@Override
		public BodyBuilder contextPath(String contextPath) {
			this.contextPath = contextPath;
			return this;
		}

		@Override
		public BodyBuilder remoteAddress(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
			return this;
		}

		@Override
		public BodyBuilder cookie(String path, HttpCookie... cookies) {
			this.cookies.put(path, Arrays.asList(cookies));
			return this;
		}

		@Override
		public BodyBuilder cookies(MultiValueMap<String, HttpCookie> cookies) {
			this.cookies.putAll(cookies);
			return this;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(MultiValueMap<String, String> headers) {
			this.headers.putAll(headers);
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
		public BodyBuilder ifModifiedSince(long ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifUnmodifiedSince(long ifUnmodifiedSince) {
			this.headers.setIfUnmodifiedSince(ifUnmodifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public BodyBuilder range(HttpRange... ranges) {
			this.headers.setRange(Arrays.asList(ranges));
			return this;
		}

		@Override
		public MockServerHttpRequest body(Publisher<? extends DataBuffer> body) {
			return new MockServerHttpRequest(this.method, this.url, this.contextPath,
					this.headers, this.cookies, this.remoteAddress, body);
		}

		@Override
		public MockServerHttpRequest body(String body) {
			Charset charset = getCharset();
			byte[] bytes = body.getBytes(charset);
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
			DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);
			return body(Flux.just(buffer));
		}

		private Charset getCharset() {
			MediaType contentType = this.headers.getContentType();
			Charset charset = (contentType != null ? contentType.getCharset() : null);
			charset = charset != null ? charset : StandardCharsets.UTF_8;
			return charset;
		}

		@Override
		public MockServerHttpRequest build() {
			return body(Flux.empty());
		}

		@Override
		public MockServerWebExchange toExchange() {
			return build().toExchange();
		}
	}

}