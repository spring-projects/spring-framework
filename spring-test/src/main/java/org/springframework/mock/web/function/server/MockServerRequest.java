/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.mock.web.function.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MockServerRequest implements ServerRequest {

	private final HttpMethod method;

	private final URI uri;

	private final MockHeaders headers;

	private final MultiValueMap<String, Cookie> cookies;

	@Nullable
	private final Object body;

	private final Map<String, Object> attributes;

	private final MultiValueMap<String, String> queryParams;

	private final Map<String, String> pathVariables;

	@Nullable
	private final HttpSession session;

	@Nullable
	private final Principal principal;

	@Nullable
	private final InetSocketAddress remoteAddress;

	private final List<HttpMessageConverter<?>> messageConverters;

	private final ServletServerHttpRequest serverHttpRequest;

	public MockServerRequest(HttpMethod method, URI uri, MockHeaders headers,
							 MultiValueMap<String, Cookie> cookies,
							 @Nullable Object body, Map<String, Object> attributes,
							 MultiValueMap<String, String> queryParams, Map<String, String> pathVariables,
							 @Nullable HttpSession session, @Nullable Principal principal, @Nullable InetSocketAddress remoteAddress,
							 List<HttpMessageConverter<?>> messageConverters, ServletServerHttpRequest serverHttpRequest) {
		this.method = method;
		this.uri = uri;
		this.headers = headers;
		this.cookies = cookies;
		this.body = body;
		this.attributes = attributes;
		this.queryParams = queryParams;
		this.pathVariables = pathVariables;
		this.session = session;
		this.principal = principal;
		this.remoteAddress = remoteAddress;
		this.messageConverters = messageConverters;
		this.serverHttpRequest = serverHttpRequest;
	}

	@Override
	public HttpMethod method() {
		return this.method;
	}

	@Override
	@Deprecated
	public String methodName() {
		return this.method.name();
	}

	@Override
	public URI uri() {
		return uri;
	}

	@Override
	public UriBuilder uriBuilder() {
		return UriComponentsBuilder.fromUri(this.uri);
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return this.cookies;
	}

	@Override
	public Optional<InetSocketAddress> remoteAddress() {
		return Optional.ofNullable(this.remoteAddress);
	}

	@Override
	public List<HttpMessageConverter<?>> messageConverters() {
		return this.messageConverters;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T body(Class<T> bodyType) throws ServletException, IOException {
		Assert.state(this.body != null, "No body");
		return (T) this.body;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T body(ParameterizedTypeReference<T> bodyType) throws ServletException, IOException {
		Assert.state(this.body != null, "No body");
		return (T) this.body;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T bind(Class<T> bindType, Consumer<WebDataBinder> dataBinderCustomizer) throws BindException {
		Assert.state(this.body != null, "No body");
		return (T) this.body;
	}

	@Override
	public Map<String, Object> attributes() {
		return this.attributes;
	}

	@Override
	public MultiValueMap<String, String> params() {
		return CollectionUtils.unmodifiableMultiValueMap(this.queryParams);
	}

	@Override
	@SuppressWarnings("unchecked")
	public MultiValueMap<String, Part> multipartData() {
		Assert.state(this.body != null, "No body");
		return (MultiValueMap<String, Part>) this.body;
	}

	@Override
	public Map<String, String> pathVariables() {
		return this.pathVariables;
	}

	@Override
	public HttpSession session() {
		Assert.state(this.session != null, "No session");
		return this.session;
	}

	@Override
	public Optional<Principal> principal() {
		return Optional.ofNullable(this.principal);
	}

	@Override
	public HttpServletRequest servletRequest() {
		return this.serverHttpRequest.getServletRequest();
	}
	public static Builder builder() {
		return new BuilderImpl();
	}
	public interface Builder {
		Builder method(HttpMethod method);

		Builder uri(URI uri);

		Builder header(String key, String value);

		Builder headers(HttpHeaders headers);

		Builder cookie(Cookie... cookies);

		Builder cookies(MultiValueMap<String, Cookie> cookies);

		Builder attribute(String name, Object value);

		Builder attributes(Map<String, Object> attributes);

		Builder queryParam(String key, String value);

		Builder queryParams(MultiValueMap<String, String> queryParams);

		Builder pathVariable(String key, String value);

		Builder pathVariables(Map<String, String> pathVariables);

		Builder session(HttpSession session);

		Builder principal(Principal principal);

		Builder remoteAddress(InetSocketAddress remoteAddress);

		Builder messageConverters(List<HttpMessageConverter<?>> messageConverters);

		Builder serverHttpRequest(ServletServerHttpRequest serverHttpRequest);
		MockServerRequest body(Object body);

		MockServerRequest build();
	}

	private static class BuilderImpl implements Builder {

		private HttpMethod method = HttpMethod.GET;

		private URI uri = URI.create("http://localhost");
		;

		private MockHeaders headers = new MockHeaders(new HttpHeaders());

		private MultiValueMap<String, Cookie> cookies = new LinkedMultiValueMap<>();

		@Nullable
		private Object body;

		private Map<String, Object> attributes = new ConcurrentHashMap<>();

		private MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		;

		private Map<String, String> pathVariables = new LinkedHashMap<>();

		@Nullable
		private HttpSession session;

		@Nullable
		private Principal principal;

		@Nullable
		private InetSocketAddress remoteAddress;

		private List<HttpMessageConverter<?>> messageConverters;

		private ServletServerHttpRequest serverHttpRequest;

		@Override
		public Builder method(HttpMethod method) {
			Assert.notNull(method, "'method' must not be null");
			this.method = method;
			return this;
		}

		@Override
		public Builder uri(URI uri) {
			Assert.notNull(uri, "'uri' must not be null");
			this.uri = uri;
			return this;
		}

		@Override
		public Builder cookie(Cookie... cookies) {
			Arrays.stream(cookies).forEach(cookie -> this.cookies.add(cookie.getName(), cookie));
			return this;
		}

		@Override
		public Builder cookies(MultiValueMap<String, Cookie> cookies) {
			Assert.notNull(cookies, "'cookies' must not be null");
			this.cookies = cookies;
			return this;
		}

		@Override
		public Builder header(String key, String value) {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.headers.addHeader(key, value);
			return this;
		}

		@Override
		public Builder headers(HttpHeaders headers) {
			Assert.notNull(headers, "'headers' must not be null");
			this.headers = new MockHeaders(headers);
			return this;
		}

		@Override
		public Builder attribute(String name, Object value) {
			Assert.notNull(name, "'name' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.attributes.put(name, value);
			return this;
		}

		@Override
		public Builder attributes(Map<String, Object> attributes) {
			Assert.notNull(attributes, "'attributes' must not be null");
			this.attributes = attributes;
			return this;
		}

		@Override
		public Builder queryParam(String key, String value) {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.queryParams.add(key, value);
			return this;
		}

		@Override
		public Builder queryParams(MultiValueMap<String, String> queryParams) {
			Assert.notNull(queryParams, "'queryParams' must not be null");
			this.queryParams = queryParams;
			return this;
		}

		@Override
		public Builder pathVariable(String key, String value) {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.pathVariables.put(key, value);
			return this;
		}

		@Override
		public Builder pathVariables(Map<String, String> pathVariables) {
			Assert.notNull(pathVariables, "'pathVariables' must not be null");
			this.pathVariables = pathVariables;
			return this;
		}

		@Override
		public Builder session(HttpSession session) {
			Assert.notNull(session, "'session' must not be null");
			this.session = session;
			return this;
		}

		@Override
		public Builder principal(Principal principal) {
			Assert.notNull(principal, "'principal' must not be null");
			this.principal = principal;
			return this;
		}

		@Override
		public Builder remoteAddress(InetSocketAddress remoteAddress) {
			Assert.notNull(remoteAddress, "'remoteAddress' must not be null");
			this.remoteAddress = remoteAddress;
			return this;
		}

		@Override
		public Builder messageConverters(List<HttpMessageConverter<?>> messageConverters) {
			Assert.notNull(messageConverters, "'messageConverters' must not be null");
			this.messageConverters = messageConverters;
			return this;
		}

		@Override
		public Builder serverHttpRequest(ServletServerHttpRequest serverHttpRequest) {
			Assert.notNull(serverHttpRequest, "'messageConverters' must not be null");
			this.serverHttpRequest = serverHttpRequest;
			return this;
		}

		@Override
		public MockServerRequest body(Object body) {
			this.body = body;
			return new MockServerRequest(this.method, this.uri, this.headers,
					this.cookies, this.body, this.attributes,
					this.queryParams, this.pathVariables, this.session,
					this.principal, this.remoteAddress, this.messageConverters,
					this.serverHttpRequest);
		}

		@Override
		public MockServerRequest build() {
			return new MockServerRequest(this.method, this.uri, this.headers,
					this.cookies, this.body, this.attributes,
					this.queryParams, this.pathVariables, this.session,
					this.principal, this.remoteAddress, this.messageConverters,
					this.serverHttpRequest);
		}
	}

	private record MockHeaders(HttpHeaders headers) implements Headers {

		private HttpHeaders delegate() {
			return this.headers;
		}

		public void addHeader(String key, String value) {
			this.headers.add(key, value);
		}

		@Override
		public List<MediaType> accept() {
			return delegate().getAccept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return delegate().getAcceptCharset();
		}

		@Override
		public List<Locale.LanguageRange> acceptLanguage() {
			return delegate().getAcceptLanguage();
		}

		@Override
		public OptionalLong contentLength() {
			return toOptionalLong(delegate().getContentLength());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(delegate().getContentType());
		}

		@Override
		public InetSocketAddress host() {
			return delegate().getHost();
		}

		@Override
		public List<HttpRange> range() {
			return delegate().getRange();
		}

		@Override
		public List<String> header(String headerName) {
			List<String> headerValues = delegate().get(headerName);
			return headerValues != null ? headerValues : Collections.emptyList();
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return HttpHeaders.readOnlyHttpHeaders(delegate());
		}

		private OptionalLong toOptionalLong(long value) {
			return value != -1 ? OptionalLong.of(value) : OptionalLong.empty();
		}

	}
}
