/*
 * Copyright 2002-2018 the original author or authors.
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default {@link ServerResponse.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

	private final int statusCode;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private final Map<String, Object> hints = new HashMap<>();


	public DefaultServerResponseBuilder(ServerResponse other) {
		Assert.notNull(other, "ServerResponse must not be null");
		this.statusCode = (other instanceof AbstractServerResponse ?
				((AbstractServerResponse) other).statusCode : other.statusCode().value());
		this.headers.addAll(other.headers());
	}

	public DefaultServerResponseBuilder(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.statusCode = status.value();
	}

	public DefaultServerResponseBuilder(int statusCode) {
		this.statusCode = statusCode;
	}


	@Override
	public ServerResponse.BodyBuilder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(Set<HttpMethod> allowedMethods) {
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder eTag(String etag) {
		if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
			etag = "\"" + etag;
		}
		if (!etag.endsWith("\"")) {
			etag = etag + "\"";
		}
		this.headers.setETag(etag);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder hint(String key, Object value) {
		this.hints.put(key, value);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(ZonedDateTime lastModified) {
		this.headers.setZonedDateTime(HttpHeaders.LAST_MODIFIED, lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cacheControl(CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			this.headers.setCacheControl(ccValue);
		}
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public Mono<ServerResponse> build() {
		return build((exchange, handlerStrategies) -> exchange.getResponse().setComplete());
	}

	@Override
	public Mono<ServerResponse> build(Publisher<Void> voidPublisher) {
		Assert.notNull(voidPublisher, "Publisher must not be null");
		return build((exchange, handlerStrategies) ->
				Mono.from(voidPublisher).then(exchange.getResponse().setComplete()));
	}

	@Override
	public Mono<ServerResponse> build(
			BiFunction<ServerWebExchange, ServerResponse.Context, Mono<Void>> writeFunction) {

		return Mono.just(
				new WriterFunctionResponse(this.statusCode, this.headers, this.cookies, writeFunction));
	}

	@Override
	public <T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, Class<T> elementClass) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.notNull(elementClass, "Element Class must not be null");

		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, elementClass))
				.headers(this.headers)
				.status(this.statusCode)
				.build()
				.map(entityResponse -> entityResponse);
	}

	@Override
	public <T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher,
			ParameterizedTypeReference<T> typeReference) {

		Assert.notNull(publisher, "Publisher must not be null");
		Assert.notNull(typeReference, "ParameterizedTypeReference must not be null");

		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, typeReference))
				.headers(this.headers)
				.status(this.statusCode)
				.build()
				.map(entityResponse -> entityResponse);
	}

	@Override
	public Mono<ServerResponse> syncBody(Object body) {
		Assert.notNull(body, "Body must not be null");
		Assert.isTrue(!(body instanceof Publisher),
				"Please specify the element class by using body(Publisher, Class)");

		return new DefaultEntityResponseBuilder<>(body,
				BodyInserters.fromObject(body))
				.headers(this.headers)
				.status(this.statusCode)
				.build()
				.map(entityResponse -> entityResponse);
	}

	@Override
	public Mono<ServerResponse> body(BodyInserter<?, ? super ServerHttpResponse> inserter) {
		return Mono.just(
				new BodyInserterResponse<>(this.statusCode, this.headers, this.cookies, inserter, this.hints));
	}

	@Override
	public Mono<ServerResponse> render(String name, Object... modelAttributes) {
		return new DefaultRenderingResponseBuilder(name)
				.headers(this.headers)
				.status(this.statusCode)
				.modelAttributes(modelAttributes)
				.build()
				.map(renderingResponse -> renderingResponse);
	}

	@Override
	public Mono<ServerResponse> render(String name, Map<String, ?> model) {
		return new DefaultRenderingResponseBuilder(name)
				.headers(this.headers)
				.status(this.statusCode)
				.modelAttributes(model)
				.build()
				.map(renderingResponse -> renderingResponse);
	}


	/**
	 * Abstract base class for {@link ServerResponse} implementations.
	 */
	abstract static class AbstractServerResponse implements ServerResponse {

		private static final Set<HttpMethod> SAFE_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

		final int statusCode;

		private final HttpHeaders headers;

		private final MultiValueMap<String, ResponseCookie> cookies;

		protected AbstractServerResponse(
				int statusCode, HttpHeaders headers, MultiValueMap<String, ResponseCookie> cookies) {

			this.statusCode = statusCode;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(cookies));
		}

		@Override
		public final HttpStatus statusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public final HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public MultiValueMap<String, ResponseCookie> cookies() {
			return this.cookies;
		}

		@Override
		public final Mono<Void> writeTo(ServerWebExchange exchange, Context context) {
			writeStatusAndHeaders(exchange.getResponse());

			Instant lastModified = Instant.ofEpochMilli(headers().getLastModified());
			HttpMethod httpMethod = exchange.getRequest().getMethod();
			if (SAFE_METHODS.contains(httpMethod) && exchange.checkNotModified(headers().getETag(), lastModified)) {
				return exchange.getResponse().setComplete();
			}
			else {
				return writeToInternal(exchange, context);
			}
		}

		private void writeStatusAndHeaders(ServerHttpResponse response) {
			if (response instanceof AbstractServerHttpResponse) {
				((AbstractServerHttpResponse) response).setStatusCodeValue(this.statusCode);
			}
			else {
				HttpStatus status = HttpStatus.resolve(this.statusCode);
				if (status == null) {
					throw new IllegalStateException(
							"Unresolvable HttpStatus for general ServerHttpResponse: " + this.statusCode);
				}
				response.setStatusCode(status);
			}
			copy(this.headers, response.getHeaders());
			copy(this.cookies, response.getCookies());
		}

		protected abstract Mono<Void> writeToInternal(ServerWebExchange exchange, Context context);

		private static <K,V> void copy(MultiValueMap<K,V> src, MultiValueMap<K,V> dst) {
			if (!src.isEmpty()) {
				src.entrySet().stream()
						.filter(entry -> !dst.containsKey(entry.getKey()))
						.forEach(entry -> dst.put(entry.getKey(), entry.getValue()));
			}
		}
	}


	private static final class WriterFunctionResponse extends AbstractServerResponse {

		private final BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction;

		public WriterFunctionResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, ResponseCookie> cookies,
				BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction) {

			super(statusCode, headers, cookies);
			Assert.notNull(writeFunction, "BiFunction must not be null");
			this.writeFunction = writeFunction;
		}

		@Override
		protected Mono<Void> writeToInternal(ServerWebExchange exchange, Context context) {
			return this.writeFunction.apply(exchange, context);
		}
	}


	private static final class BodyInserterResponse<T> extends AbstractServerResponse {

		private final BodyInserter<T, ? super ServerHttpResponse> inserter;

		private final Map<String, Object> hints;

		public BodyInserterResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, ResponseCookie> cookies,
				BodyInserter<T, ? super ServerHttpResponse> body, Map<String, Object> hints) {

			super(statusCode, headers, cookies);
			Assert.notNull(body, "BodyInserter must not be null");
			this.inserter = body;
			this.hints = hints;
		}

		@Override
		protected Mono<Void> writeToInternal(ServerWebExchange exchange, Context context) {
			return this.inserter.insert(exchange.getResponse(), new BodyInserter.Context() {
				@Override
				public List<HttpMessageWriter<?>> messageWriters() {
					return context.messageWriters();
				}
				@Override
				public Optional<ServerHttpRequest> serverRequest() {
					return Optional.of(exchange.getRequest());
				}
				@Override
				public Map<String, Object> hints() {
					return hints;
				}
			});
		}
	}

}
