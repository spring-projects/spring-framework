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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.Hints;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.HttpMessageWriter;
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
 * @author Sebastien Deleuze
 * @since 5.0
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

	private final int statusCode;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private final Map<String, Object> hints = new HashMap<>();


	public DefaultServerResponseBuilder(ServerResponse other) {
		Assert.notNull(other, "ServerResponse must not be null");
		this.headers.addAll(other.headers());
		this.cookies.addAll(other.cookies());
		if (other instanceof AbstractServerResponse) {
			AbstractServerResponse abstractOther = (AbstractServerResponse) other;
			this.statusCode = abstractOther.statusCode;
			this.hints.putAll(abstractOther.hints);
		}
		else {
			this.statusCode = other.statusCode().value();
		}
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
	public ServerResponse.BodyBuilder hints(Consumer<Map<String, Object>> hintsConsumer) {
		hintsConsumer.accept(this.hints);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(ZonedDateTime lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(Instant lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cacheControl(CacheControl cacheControl) {
		this.headers.setCacheControl(cacheControl);
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

		return Mono.just(new WriterFunctionResponse(
				this.statusCode, this.headers, this.cookies, writeFunction));
	}

	@Override
	public Mono<ServerResponse> bodyValue(Object body) {
		return initBuilder(body, BodyInserters.fromValue(body));
	}

	@Override
	public <T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, Class<T> elementClass) {
		return initBuilder(publisher, BodyInserters.fromPublisher(publisher, elementClass));
	}

	@Override
	public <T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, ParameterizedTypeReference<T> typeRef) {
		return initBuilder(publisher, BodyInserters.fromPublisher(publisher, typeRef));
	}

	@Override
	public Mono<ServerResponse> body(Object producer, Class<?> elementClass) {
		return initBuilder(producer, BodyInserters.fromProducer(producer, elementClass));
	}

	@Override
	public Mono<ServerResponse> body(Object producer, ParameterizedTypeReference<?> elementTypeRef) {
		return initBuilder(producer, BodyInserters.fromProducer(producer, elementTypeRef));
	}

	private  <T> Mono<ServerResponse> initBuilder(T entity, BodyInserter<T, ReactiveHttpOutputMessage> inserter) {
		return new DefaultEntityResponseBuilder<>(entity, inserter)
				.status(this.statusCode)
				.headers(this.headers)
				.cookies(cookies -> cookies.addAll(this.cookies))
				.hints(hints -> hints.putAll(this.hints))
				.build()
				.map(Function.identity());
	}

	@Override
	public Mono<ServerResponse> body(BodyInserter<?, ? super ServerHttpResponse> inserter) {
		return Mono.just(new BodyInserterResponse<>(
				this.statusCode, this.headers, this.cookies, inserter, this.hints));
	}

	@Override
	@Deprecated
	public Mono<ServerResponse> syncBody(Object body) {
		return bodyValue(body);
	}

	@Override
	public Mono<ServerResponse> render(String name, Object... modelAttributes) {
		return new DefaultRenderingResponseBuilder(name)
				.status(this.statusCode)
				.headers(this.headers)
				.cookies(cookies -> cookies.addAll(this.cookies))
				.modelAttributes(modelAttributes)
				.build()
				.map(Function.identity());
	}

	@Override
	public Mono<ServerResponse> render(String name, Map<String, ?> model) {
		return new DefaultRenderingResponseBuilder(name)
				.status(this.statusCode)
				.headers(this.headers)
				.cookies(cookies -> cookies.addAll(this.cookies))
				.modelAttributes(model)
				.build()
				.map(Function.identity());
	}


	/**
	 * Abstract base class for {@link ServerResponse} implementations.
	 */
	abstract static class AbstractServerResponse implements ServerResponse {

		private static final Set<HttpMethod> SAFE_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

		final int statusCode;

		private final HttpHeaders headers;

		private final MultiValueMap<String, ResponseCookie> cookies;

		final Map<String, Object> hints;


		protected AbstractServerResponse(
				int statusCode, HttpHeaders headers, MultiValueMap<String, ResponseCookie> cookies,
				Map<String, Object> hints) {

			this.statusCode = statusCode;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(cookies));
			this.hints = hints;
		}

		@Override
		public final HttpStatus statusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public int rawStatusCode() {
			return this.statusCode;
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
			response.setRawStatusCode(this.statusCode);
			copy(this.headers, response.getHeaders());
			copy(this.cookies, response.getCookies());
		}

		protected abstract Mono<Void> writeToInternal(ServerWebExchange exchange, Context context);

		private static <K,V> void copy(MultiValueMap<K,V> src, MultiValueMap<K,V> dst) {
			if (!src.isEmpty()) {
				dst.putAll(src);
			}
		}
	}


	private static final class WriterFunctionResponse extends AbstractServerResponse {

		private final BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction;

		public WriterFunctionResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, ResponseCookie> cookies,
				BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction) {

			super(statusCode, headers, cookies, Collections.emptyMap());
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


		public BodyInserterResponse(int statusCode, HttpHeaders headers,
				MultiValueMap<String, ResponseCookie> cookies,
				BodyInserter<T, ? super ServerHttpResponse> body, Map<String, Object> hints) {

			super(statusCode, headers, cookies, hints);
			Assert.notNull(body, "BodyInserter must not be null");
			this.inserter = body;
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
					hints.put(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix());
					return hints;
				}
			});
		}
	}

}
