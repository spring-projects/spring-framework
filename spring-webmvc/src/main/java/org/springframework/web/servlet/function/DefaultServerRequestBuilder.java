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

package org.springframework.web.servlet.function;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Default {@link ServerRequest.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class DefaultServerRequestBuilder implements ServerRequest.Builder {

	private final HttpServletRequest servletRequest;

	private final List<HttpMessageConverter<?>> messageConverters;

	private final @Nullable ApiVersionStrategy versionStrategy;

	private HttpMethod method;

	private URI uri;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, Cookie> cookies = new LinkedMultiValueMap<>();

	private final Map<String, Object> attributes = new LinkedHashMap<>();

	private final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

	private @Nullable InetSocketAddress remoteAddress;

	private byte[] body = new byte[0];


	public DefaultServerRequestBuilder(ServerRequest other) {
		Assert.notNull(other, "ServerRequest must not be null");
		this.servletRequest = other.servletRequest();
		this.messageConverters = new ArrayList<>(other.messageConverters());
		this.versionStrategy = other.apiVersionStrategy();
		this.method = other.method();
		this.uri = other.uri();
		headers(headers -> headers.addAll(other.headers().asHttpHeaders()));
		cookies(cookies -> cookies.addAll(other.cookies()));
		attributes(attributes -> attributes.putAll(other.attributes()));
		params(params -> params.addAll(other.params()));
		this.remoteAddress = other.remoteAddress().orElse(null);
	}

	@Override
	public ServerRequest.Builder method(HttpMethod method) {
		Assert.notNull(method, "HttpMethod must not be null");
		this.method = method;
		return this;
	}

	@Override
	public ServerRequest.Builder uri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		return this;
	}

	@Override
	public ServerRequest.Builder header(String headerName, String... headerValues) {
		Assert.notNull(headerName, "Header name must not be null");
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		Assert.notNull(headersConsumer, "Headers consumer must not be null");
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerRequest.Builder cookie(String name, String... values) {
		Assert.notNull(name, "Cookie name must not be null");
		for (String value : values) {
			this.cookies.add(name, new Cookie(name, value));
		}
		return this;
	}

	@Override
	public ServerRequest.Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer) {
		Assert.notNull(cookiesConsumer, "Cookies consumer must not be null");
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ServerRequest.Builder body(byte[] body) {
		Assert.notNull(body, "Body must not be null");
		this.body = body;
		return this;
	}

	@Override
	public ServerRequest.Builder body(String body) {
		Assert.notNull(body, "Body must not be null");
		return body(body.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public ServerRequest.Builder attribute(String name, Object value) {
		Assert.notNull(name, "Name must not be null");
		this.attributes.put(name, value);
		return this;
	}

	@Override
	public ServerRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
		Assert.notNull(attributesConsumer, "Attributes consumer must not be null");
		attributesConsumer.accept(this.attributes);
		return this;
	}

	@Override
	public ServerRequest.Builder param(String name, String... values) {
		Assert.notNull(name, "Name must not be null");
		for (String value : values) {
			this.params.add(name, value);
		}
		return this;
	}

	@Override
	public ServerRequest.Builder params(Consumer<MultiValueMap<String, String>> paramsConsumer) {
		Assert.notNull(paramsConsumer, "Parameters consumer must not be null");
		paramsConsumer.accept(this.params);
		return this;
	}

	@Override
	public ServerRequest.Builder remoteAddress(@Nullable InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}


	@Override
	public ServerRequest build() {
		return new BuiltServerRequest(this.servletRequest, this.method, this.uri, this.headers, this.cookies,
				this.attributes, this.params, this.remoteAddress, this.body,
				this.messageConverters, this.versionStrategy);
	}


	private static class BuiltServerRequest implements ServerRequest {

		private final HttpMethod method;

		private final URI uri;

		private final HttpHeaders headers;

		private final HttpServletRequest servletRequest;

		private final MultiValueMap<String, Cookie> cookies;

		private final Map<String, Object> attributes;

		private final byte[] body;

		private final List<HttpMessageConverter<?>> messageConverters;

		private final @Nullable ApiVersionStrategy versionStrategy;

		private final MultiValueMap<String, String> params;

		private final @Nullable InetSocketAddress remoteAddress;

		public BuiltServerRequest(HttpServletRequest servletRequest, HttpMethod method, URI uri,
				HttpHeaders headers, MultiValueMap<String, Cookie> cookies,
				Map<String, Object> attributes, MultiValueMap<String, String> params,
				@Nullable InetSocketAddress remoteAddress, byte[] body,
				List<HttpMessageConverter<?>> messageConverters,
				@Nullable ApiVersionStrategy versionStrategy) {

			this.servletRequest = servletRequest;
			this.method = method;
			this.uri = uri;
			this.headers = new HttpHeaders(headers);
			this.cookies = new LinkedMultiValueMap<>(cookies);
			this.attributes = new LinkedHashMap<>(attributes);
			this.params = new LinkedMultiValueMap<>(params);
			this.remoteAddress = remoteAddress;
			this.body = body;
			this.messageConverters = messageConverters;
			this.versionStrategy = versionStrategy;
		}

		@Override
		public HttpMethod method() {
			return this.method;
		}

		@Override
		public MultiValueMap<String, Part> multipartData() throws IOException, ServletException {
			return servletRequest().getParts().stream()
					.collect(Collectors.groupingBy(Part::getName,
							LinkedMultiValueMap::new,
							Collectors.toList()));
		}

		@Override
		public URI uri() {
			return this.uri;
		}

		@Override
		public UriBuilder uriBuilder() {
			return UriComponentsBuilder.fromUri(this.uri);
		}

		@Override
		public Headers headers() {
			return new DefaultServerRequest.DefaultRequestHeaders(this.headers);
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
		public @Nullable ApiVersionStrategy apiVersionStrategy() {
			return this.versionStrategy;
		}

		@Override
		public <T> T body(Class<T> bodyType) throws IOException, ServletException {
			return bodyInternal(bodyType, bodyType);
		}

		@Override
		public <T> T body(ParameterizedTypeReference<T> bodyType) throws IOException, ServletException {
			Type type = bodyType.getType();
			return bodyInternal(type, DefaultServerRequest.bodyClass(type));
		}

		@SuppressWarnings("unchecked")
		private <T> T bodyInternal(Type bodyType, Class<?> bodyClass) throws ServletException, IOException {
			HttpInputMessage inputMessage = new BuiltInputMessage();
			MediaType contentType = headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
				if (messageConverter instanceof GenericHttpMessageConverter<?> genericMessageConverter) {
					if (genericMessageConverter.canRead(bodyType, bodyClass, contentType)) {
						return (T) genericMessageConverter.read(bodyType, bodyClass, inputMessage);
					}
				}
				else if (messageConverter instanceof SmartHttpMessageConverter<?> smartMessageConverter) {
					ResolvableType resolvableType = ResolvableType.forType(bodyType);
					if (smartMessageConverter.canRead(resolvableType, contentType)) {
						return (T) smartMessageConverter.read(resolvableType, inputMessage, null);
					}
				}
				else if (messageConverter.canRead(bodyClass, contentType)) {
					HttpMessageConverter<T> theConverter =
							(HttpMessageConverter<T>) messageConverter;
					Class<? extends T> clazz = (Class<? extends T>) bodyClass;
					return theConverter.read(clazz, inputMessage);
				}
			}
			throw new HttpMediaTypeNotSupportedException(contentType, Collections.emptyList(), method());
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T bind(Class<T> bindType, Consumer<WebDataBinder> dataBinderCustomizer) throws BindException {
			Assert.notNull(bindType, "BindType must not be null");
			Assert.notNull(dataBinderCustomizer, "DataBinderCustomizer must not be null");

			ServletRequestDataBinder dataBinder = new ServletRequestDataBinder(null);
			dataBinder.setTargetType(ResolvableType.forClass(bindType));
			dataBinderCustomizer.accept(dataBinder);

			HttpServletRequest servletRequest = servletRequest();
			dataBinder.construct(servletRequest);
			dataBinder.bind(servletRequest);

			BindingResult bindingResult = dataBinder.getBindingResult();
			if (bindingResult.hasErrors()) {
				throw new BindException(bindingResult);
			}
			else {
				T result = (T) bindingResult.getTarget();
				if (result != null) {
					return result;
				}
				else {
					throw new IllegalStateException("Binding result has neither target nor errors");
				}
			}
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public MultiValueMap<String, String> params() {
			return this.params;
		}

		@Override
		public Map<String, String> pathVariables() {
			@SuppressWarnings("unchecked")
			Map<String, String> pathVariables = (Map<String, String>) attributes()
					.get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			if (pathVariables != null) {
				return pathVariables;
			}
			else {
				return Collections.emptyMap();
			}
		}

		@Override
		public HttpSession session() {
			return this.servletRequest.getSession();
		}

		@Override
		public Optional<Principal> principal() {
			return Optional.ofNullable(this.servletRequest.getUserPrincipal());
		}

		@Override
		public HttpServletRequest servletRequest() {
			return this.servletRequest;
		}


		private class BuiltInputMessage implements HttpInputMessage {

			@Override
			public InputStream getBody() throws IOException {
				return new BodyInputStream(body);
			}

			@Override
			public HttpHeaders getHeaders() {
				return headers;
			}
		}
	}


	private static class BodyInputStream extends ServletInputStream {

		private final InputStream delegate;

		public BodyInputStream(byte[] body) {
			this.delegate = new ByteArrayInputStream(body);
		}

		@Override
		public boolean isFinished() {
			return false;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int read() throws IOException {
			return this.delegate.read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return this.delegate.read(b, off, len);
		}

		@Override
		public int read(byte[] b) throws IOException {
			return this.delegate.read(b);
		}

		@Override
		public long skip(long n) throws IOException {
			return this.delegate.skip(n);
		}

		@Override
		public int available() throws IOException {
			return this.delegate.available();
		}

		@Override
		public void close() throws IOException {
			this.delegate.close();
		}

		@Override
		public synchronized void mark(int readlimit) {
			this.delegate.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			this.delegate.reset();
		}

		@Override
		public boolean markSupported() {
			return this.delegate.markSupported();
		}
	}

}
