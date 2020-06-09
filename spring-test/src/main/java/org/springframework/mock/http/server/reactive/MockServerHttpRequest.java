/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.mock.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Mock extension of {@link AbstractServerHttpRequest} for use in tests without
 * an actual server. Use the static methods to obtain a builder.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class MockServerHttpRequest extends AbstractServerHttpRequest {

	/**
	 * String representation of one of {@link HttpMethod} or not empty custom method (e.g. <i>CONNECT</i>).
	 */
	private final String httpMethod;

	private final MultiValueMap<String, HttpCookie> cookies;

	@Nullable
	private final InetSocketAddress localAddress;

	@Nullable
	private final InetSocketAddress remoteAddress;

	@Nullable
	private final SslInfo sslInfo;

	private final Flux<DataBuffer> body;

	private MockServerHttpRequest(String httpMethod,
			URI uri, @Nullable String contextPath, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies,
			@Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress,
			@Nullable SslInfo sslInfo, Publisher<? extends DataBuffer> body) {

		super(uri, contextPath, headers);
		Assert.isTrue(StringUtils.hasText(httpMethod), "HTTP method is required.");
		this.httpMethod = httpMethod;
		this.cookies = cookies;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.sslInfo = sslInfo;
		this.body = Flux.from(body);
	}


	@Override
	@Nullable
	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.httpMethod);
	}

	@Override
	public String getMethodValue() {
		return this.httpMethod;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	@Nullable
	protected SslInfo initSslInfo() {
		return this.sslInfo;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		return this.cookies;
	}

	@Override
	public <T> T getNativeRequest() {
		throw new IllegalStateException("This is a mock. No running server, no native request.");
	}


	// Static builder methods

	/**
	 * Create an HTTP GET builder with the given URI template. The given URI may
	 * contain query parameters, or those may be added later via
	 * {@link BaseBuilder#queryParam queryParam} builder methods.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> get(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.GET, urlTemplate, uriVars);
	}

	/**
	 * HTTP HEAD variant. See {@link #get(String, Object...)} for general info.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> head(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.HEAD, urlTemplate, uriVars);
	}

	/**
	 * HTTP POST variant. See {@link #get(String, Object...)} for general info.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BodyBuilder post(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.POST, urlTemplate, uriVars);
	}

	/**
	 * HTTP PUT variant. See {@link #get(String, Object...)} for general info.
	 * {@link BaseBuilder#queryParam queryParam} builder methods.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BodyBuilder put(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PUT, urlTemplate, uriVars);
	}

	/**
	 * HTTP PATCH variant. See {@link #get(String, Object...)} for general info.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BodyBuilder patch(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PATCH, urlTemplate, uriVars);
	}

	/**
	 * HTTP DELETE variant. See {@link #get(String, Object...)} for general info.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> delete(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.DELETE, urlTemplate, uriVars);
	}

	/**
	 * HTTP OPTIONS variant. See {@link #get(String, Object...)} for general info.
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVars zero or more URI variables
	 * @return the created builder
	 */
	public static BaseBuilder<?> options(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.OPTIONS, urlTemplate, uriVars);
	}

	/**
	 * Create a builder with the given HTTP method and a {@link URI}.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the URL
	 * @return the created builder
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		Assert.notNull(method, "HTTP method is required. " +
				"For a custom HTTP method, please provide a String HTTP method value.");
		return new DefaultBodyBuilder(method.name(), url);
	}

	/**
	 * Alternative to {@link #method(HttpMethod, URI)} that accepts a URI template.
	 * The given URI may contain query parameters, or those may be added later via
	 * {@link BaseBuilder#queryParam queryParam} builder methods.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param uri the URI template for the target URL
	 * @param vars variables to expand into the template
	 * @return the created builder
	 */
	public static BodyBuilder method(HttpMethod method, String uri, Object... vars) {
		return method(method, toUri(uri, vars));
	}

	/**
	 * Create a builder with a raw HTTP method value value that is outside the
	 * range of {@link HttpMethod} enum values.
	 * @param httpMethod the HTTP methodValue value
	 * @param uri the URI template for target the URL
	 * @param vars variables to expand into the template
	 * @return the created builder
	 * @since 5.2.7
	 */
	public static BodyBuilder method(String httpMethod, String uri, Object... vars) {
		return new DefaultBodyBuilder(httpMethod, toUri(uri, vars));
	}

	private static URI toUri(String uri, Object[] vars) {
		return UriComponentsBuilder.fromUriString(uri).buildAndExpand(vars).encode().toUri();
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
		 * Append the given query parameter to the existing query parameters.
		 * If no values are given, the resulting URI will contain the query
		 * parameter name only (i.e. {@code ?foo} instead of {@code ?foo=bar}).
		 * <p>The provided query name and values will be encoded.
		 * @param name the query parameter name
		 * @param values the query parameter values
		 * @return this UriComponentsBuilder
		 */
		B queryParam(String name, Object... values);

		/**
		 * Add the given query parameters and values. The provided query name
		 * and corresponding values will be encoded.
		 * @param params the params
		 * @return this UriComponentsBuilder
		 */
		B queryParams(MultiValueMap<String, String> params);

		/**
		 * Set the remote address to return.
		 */
		B remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * Set the local address to return.
		 * @since 5.2.3
		 */
		B localAddress(InetSocketAddress localAddress);

		/**
		 * Set SSL session information and certificates.
		 */
		void sslInfo(SslInfo sslInfo);

		/**
		 * Add one or more cookies.
		 */
		B cookie(HttpCookie... cookie);

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
		 * Set the list of acceptable {@linkplain Locale locales}, as specified
		 * by the {@code Accept-Languages} header.
		 * @param acceptableLocales the acceptable locales
		 */
		B acceptLanguageAsLocales(Locale... acceptableLocales);

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

		private static final DataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();

		private final String methodValue;

		private final URI url;

		@Nullable
		private String contextPath;

		private final UriComponentsBuilder queryParamsBuilder = UriComponentsBuilder.newInstance();

		private final HttpHeaders headers = new HttpHeaders();

		private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

		@Nullable
		private InetSocketAddress remoteAddress;

		@Nullable
		private InetSocketAddress localAddress;

		@Nullable
		private SslInfo sslInfo;

		DefaultBodyBuilder(String method, URI url) {
			this.methodValue = method;
			this.url = url;
		}

		@Override
		public BodyBuilder contextPath(String contextPath) {
			this.contextPath = contextPath;
			return this;
		}

		@Override
		public BodyBuilder queryParam(String name, Object... values) {
			this.queryParamsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public BodyBuilder queryParams(MultiValueMap<String, String> params) {
			this.queryParamsBuilder.queryParams(params);
			return this;
		}

		@Override
		public BodyBuilder remoteAddress(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
			return this;
		}

		@Override
		public BodyBuilder localAddress(InetSocketAddress localAddress) {
			this.localAddress = localAddress;
			return this;
		}

		@Override
		public void sslInfo(SslInfo sslInfo) {
			this.sslInfo = sslInfo;
		}

		@Override
		public BodyBuilder cookie(HttpCookie... cookies) {
			Arrays.stream(cookies).forEach(cookie -> this.cookies.add(cookie.getName(), cookie));
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
		public BodyBuilder acceptLanguageAsLocales(Locale... acceptableLocales) {
			this.headers.setAcceptLanguageAsLocales(Arrays.asList(acceptableLocales));
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
		public MockServerHttpRequest build() {
			return body(Flux.empty());
		}

		@Override
		public MockServerHttpRequest body(String body) {
			return body(Flux.just(BUFFER_FACTORY.wrap(body.getBytes(getCharset()))));
		}

		private Charset getCharset() {
			return Optional.ofNullable(this.headers.getContentType())
					.map(MimeType::getCharset).orElse(StandardCharsets.UTF_8);
		}

		@Override
		public MockServerHttpRequest body(Publisher<? extends DataBuffer> body) {
			applyCookiesIfNecessary();
			return new MockServerHttpRequest(this.methodValue, getUrlToUse(), this.contextPath,
					this.headers, this.cookies, this.localAddress, this.remoteAddress, this.sslInfo, body);
		}

		private void applyCookiesIfNecessary() {
			if (this.headers.get(HttpHeaders.COOKIE) == null) {
				this.cookies.values().stream().flatMap(Collection::stream)
						.forEach(cookie -> this.headers.add(HttpHeaders.COOKIE, cookie.toString()));
			}
		}

		private URI getUrlToUse() {
			MultiValueMap<String, String> params =
					this.queryParamsBuilder.buildAndExpand().encode().getQueryParams();
			if (!params.isEmpty()) {
				return UriComponentsBuilder.fromUri(this.url).queryParams(params).build(true).toUri();
			}
			return this.url;
		}
	}

}
