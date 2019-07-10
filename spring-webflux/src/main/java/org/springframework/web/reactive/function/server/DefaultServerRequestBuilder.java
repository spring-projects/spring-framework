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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriUtils;

/**
 * Default {@link ServerRequest.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.1
 */
class DefaultServerRequestBuilder implements ServerRequest.Builder {

	private final List<HttpMessageReader<?>> messageReaders;

	private ServerWebExchange exchange;

	private String methodName;

	private URI uri;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

	private final Map<String, Object> attributes = new LinkedHashMap<>();

	private Flux<DataBuffer> body = Flux.empty();


	public DefaultServerRequestBuilder(ServerRequest other) {
		Assert.notNull(other, "ServerRequest must not be null");
		this.messageReaders = other.messageReaders();
		this.exchange = other.exchange();
		this.methodName = other.methodName();
		this.uri = other.uri();
		headers(headers -> headers.addAll(other.headers().asHttpHeaders()));
		cookies(cookies -> cookies.addAll(other.cookies()));
		attributes(attributes -> attributes.putAll(other.attributes()));
	}


	@Override
	public ServerRequest.Builder method(HttpMethod method) {
		Assert.notNull(method, "HttpMethod must not be null");
		this.methodName = method.name();
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
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerRequest.Builder cookie(String name, String... values) {
		for (String value : values) {
			this.cookies.add(name, new HttpCookie(name, value));
		}
		return this;
	}

	@Override
	public ServerRequest.Builder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ServerRequest.Builder body(Flux<DataBuffer> body) {
		Assert.notNull(body, "Body must not be null");
		releaseBody();
		this.body = body;
		return this;
	}

	@Override
	public ServerRequest.Builder body(String body) {
		Assert.notNull(body, "Body must not be null");
		releaseBody();
		DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
		this.body = Flux.just(body).
				map(s -> {
					byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
					return dataBufferFactory.wrap(bytes);
				});
		return this;
	}

	private void releaseBody() {
		this.body.subscribe(DataBufferUtils.releaseConsumer());
	}

	@Override
	public ServerRequest.Builder attribute(String name, Object value) {
		this.attributes.put(name, value);
		return this;
	}

	@Override
	public ServerRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
		attributesConsumer.accept(this.attributes);
		return this;
	}

	@Override
	public ServerRequest build() {
		ServerHttpRequest serverHttpRequest = new BuiltServerHttpRequest(this.exchange.getRequest().getId(),
				this.methodName, this.uri, this.headers, this.cookies, this.body);
		ServerWebExchange exchange = new DelegatingServerWebExchange(
				serverHttpRequest, this.exchange, this.messageReaders);
		return new DefaultServerRequest(exchange, this.messageReaders);
	}


	private static class BuiltServerHttpRequest implements ServerHttpRequest {

		private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

		private final String id;

		private final String method;

		private final URI uri;

		private final RequestPath path;

		private final MultiValueMap<String, String> queryParams;

		private final HttpHeaders headers;

		private final MultiValueMap<String, HttpCookie> cookies;

		private final Flux<DataBuffer> body;

		public BuiltServerHttpRequest(String id, String method, URI uri, HttpHeaders headers,
				MultiValueMap<String, HttpCookie> cookies, Flux<DataBuffer> body) {

			this.id = id;
			this.method = method;
			this.uri = uri;
			this.path = RequestPath.parse(uri, null);
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies = unmodifiableCopy(cookies);
			this.queryParams = parseQueryParams(uri);
			this.body = body;
		}

		private static <K, V> MultiValueMap<K, V> unmodifiableCopy(MultiValueMap<K, V> original) {
			return CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(original));
		}

		private static MultiValueMap<String, String> parseQueryParams(URI uri) {
			MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
			String query = uri.getRawQuery();
			if (query != null) {
				Matcher matcher = QUERY_PATTERN.matcher(query);
				while (matcher.find()) {
					String name = UriUtils.decode(matcher.group(1), StandardCharsets.UTF_8);
					String eq = matcher.group(2);
					String value = matcher.group(3);
					if (value != null) {
						value = UriUtils.decode(value, StandardCharsets.UTF_8);
					}
					else {
						value = (StringUtils.hasLength(eq) ? "" : null);
					}
					queryParams.add(name, value);
				}
			}
			return queryParams;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public String getMethodValue() {
			return this.method;
		}

		@Override
		public URI getURI() {
			return this.uri;
		}

		@Override
		public RequestPath getPath() {
			return this.path;
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, HttpCookie> getCookies() {
			return this.cookies;
		}

		@Override
		public MultiValueMap<String, String> getQueryParams() {
			return this.queryParams;
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return this.body;
		}
	}


	private static class DelegatingServerWebExchange implements ServerWebExchange {

		private static final ResolvableType FORM_DATA_TYPE =
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

		private static final ResolvableType MULTIPART_DATA_TYPE = ResolvableType.forClassWithGenerics(
				MultiValueMap.class, String.class, Part.class);

		private static final Mono<MultiValueMap<String, String>> EMPTY_FORM_DATA =
				Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, String>(0))).cache();

		private static final Mono<MultiValueMap<String, Part>> EMPTY_MULTIPART_DATA =
				Mono.just(CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, Part>(0))).cache();

		private final ServerHttpRequest request;

		private final ServerWebExchange delegate;

		private final Mono<MultiValueMap<String, String>> formDataMono;

		private final Mono<MultiValueMap<String, Part>> multipartDataMono;

		public DelegatingServerWebExchange(
				ServerHttpRequest request, ServerWebExchange delegate, List<HttpMessageReader<?>> messageReaders) {

			this.request = request;
			this.delegate = delegate;
			this.formDataMono = initFormData(request, messageReaders);
			this.multipartDataMono = initMultipartData(request, messageReaders);
		}

		@SuppressWarnings("unchecked")
		private static Mono<MultiValueMap<String, String>> initFormData(ServerHttpRequest request,
				List<HttpMessageReader<?>> readers) {

			try {
				MediaType contentType = request.getHeaders().getContentType();
				if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
					return ((HttpMessageReader<MultiValueMap<String, String>>) readers.stream()
							.filter(reader -> reader.canRead(FORM_DATA_TYPE, MediaType.APPLICATION_FORM_URLENCODED))
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("No form data HttpMessageReader.")))
							.readMono(FORM_DATA_TYPE, request, Hints.none())
							.switchIfEmpty(EMPTY_FORM_DATA)
							.cache();
				}
			}
			catch (InvalidMediaTypeException ex) {
				// Ignore
			}
			return EMPTY_FORM_DATA;
		}

		@SuppressWarnings("unchecked")
		private static Mono<MultiValueMap<String, Part>> initMultipartData(ServerHttpRequest request,
				List<HttpMessageReader<?>> readers) {

			try {
				MediaType contentType = request.getHeaders().getContentType();
				if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
					return ((HttpMessageReader<MultiValueMap<String, Part>>) readers.stream()
							.filter(reader -> reader.canRead(MULTIPART_DATA_TYPE, MediaType.MULTIPART_FORM_DATA))
							.findFirst()
							.orElseThrow(() -> new IllegalStateException("No multipart HttpMessageReader.")))
							.readMono(MULTIPART_DATA_TYPE, request, Hints.none())
							.switchIfEmpty(EMPTY_MULTIPART_DATA)
							.cache();
				}
			}
			catch (InvalidMediaTypeException ex) {
				// Ignore
			}
			return EMPTY_MULTIPART_DATA;
		}
		@Override
		public ServerHttpRequest getRequest() {
			return this.request;
		}

		@Override
		public Mono<MultiValueMap<String, String>> getFormData() {
			return this.formDataMono;
		}

		@Override
		public Mono<MultiValueMap<String, Part>> getMultipartData() {
			return this.multipartDataMono;
		}

		// Delegating methods

		@Override
		public ServerHttpResponse getResponse() {
			return this.delegate.getResponse();
		}

		@Override
		public Map<String, Object> getAttributes() {
			return this.delegate.getAttributes();
		}

		@Override
		public Mono<WebSession> getSession() {
			return this.delegate.getSession();
		}

		@Override
		public <T extends Principal> Mono<T> getPrincipal() {
			return this.delegate.getPrincipal();
		}

		@Override
		public LocaleContext getLocaleContext() {
			return this.delegate.getLocaleContext();
		}

		@Nullable
		@Override
		public ApplicationContext getApplicationContext() {
			return this.delegate.getApplicationContext();
		}

		@Override
		public boolean isNotModified() {
			return this.delegate.isNotModified();
		}

		@Override
		public boolean checkNotModified(Instant lastModified) {
			return this.delegate.checkNotModified(lastModified);
		}

		@Override
		public boolean checkNotModified(String etag) {
			return this.delegate.checkNotModified(etag);
		}

		@Override
		public boolean checkNotModified(@Nullable String etag, Instant lastModified) {
			return this.delegate.checkNotModified(etag, lastModified);
		}

		@Override
		public String transformUrl(String url) {
			return this.delegate.transformUrl(url);
		}

		@Override
		public void addUrlTransformer(Function<String, String> transformer) {
			this.delegate.addUrlTransformer(transformer);
		}

		@Override
		public String getLogPrefix() {
			return this.delegate.getLogPrefix();
		}
	}
}
