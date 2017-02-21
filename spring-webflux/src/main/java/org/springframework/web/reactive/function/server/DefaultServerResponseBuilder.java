/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default {@link ServerResponse.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

	private final HttpStatus statusCode;

	private final HttpHeaders headers = new HttpHeaders();

	private final Map<String, Object> hints = new HashMap<>();


	public DefaultServerResponseBuilder(HttpStatus statusCode) {
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
	public ServerResponse.BodyBuilder headers(HttpHeaders headers) {
		if (headers != null) {
			this.headers.putAll(headers);
		}
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
	public ServerResponse.BodyBuilder eTag(String eTag) {
		if (eTag != null) {
			if (!eTag.startsWith("\"") && !eTag.startsWith("W/\"")) {
				eTag = "\"" + eTag;
			}
			if (!eTag.endsWith("\"")) {
				eTag = eTag + "\"";
			}
		}
		this.headers.setETag(eTag);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder hint(String key, Object value) {
		this.hints.put(key, value);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(ZonedDateTime lastModified) {
		ZonedDateTime gmt = lastModified.withZoneSameInstant(ZoneId.of("GMT"));
		String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
		this.headers.set(HttpHeaders.LAST_MODIFIED, headerValue);
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
			this.headers.setCacheControl(cacheControl.getHeaderValue());
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
		Assert.notNull(voidPublisher, "'voidPublisher' must not be null");
		return build((exchange, handlerStrategies) ->
				Mono.from(voidPublisher).then(exchange.getResponse().setComplete()));
	}

	@Override
	public Mono<ServerResponse> build(
			BiFunction<ServerWebExchange, HandlerStrategies, Mono<Void>> writeFunction) {

		Assert.notNull(writeFunction, "'writeFunction' must not be null");
		return Mono.just(new WriterFunctionServerResponse(this.statusCode, this.headers, writeFunction));
	}

	@Override
	public <T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, Class<T> elementClass) {
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");

		return new DefaultEntityResponseBuilder<>(publisher,
				BodyInserters.fromPublisher(publisher, elementClass))
				.headers(this.headers)
				.status(this.statusCode)
				.build()
				.map(entityResponse -> entityResponse);
	}

	@Override
	public <T> Mono<ServerResponse> body(BodyInserter<T, ? super ServerHttpResponse> inserter) {
		Assert.notNull(inserter, "'inserter' must not be null");
		return Mono.just(new BodyInserterServerResponse<>(this.statusCode, this.headers, inserter, this.hints));
	}

	@Override
	public Mono<ServerResponse> render(String name, Object... modelAttributes) {
		Assert.hasLength(name, "'name' must not be empty");

		return new DefaultRenderingResponseBuilder(name)
				.headers(this.headers)
				.status(this.statusCode)
				.modelAttributes(modelAttributes)
				.build()
				.map(renderingResponse -> renderingResponse);
	}

	@Override
	public Mono<ServerResponse> render(String name, Map<String, ?> model) {
		Assert.hasLength(name, "'name' must not be empty");

		return new DefaultRenderingResponseBuilder(name)
				.headers(this.headers)
				.status(this.statusCode)
				.modelAttributes(model)
				.build()
				.map(renderingResponse -> renderingResponse);
	}


	static abstract class AbstractServerResponse implements ServerResponse {

		private final HttpStatus statusCode;

		private final HttpHeaders headers;

		protected AbstractServerResponse(HttpStatus statusCode, HttpHeaders headers) {
			this.statusCode = statusCode;
			this.headers = readOnlyCopy(headers);
		}

		private static HttpHeaders readOnlyCopy(HttpHeaders headers) {
			HttpHeaders copy = new HttpHeaders();
			copy.putAll(headers);
			return HttpHeaders.readOnlyHttpHeaders(copy);
		}

		@Override
		public final HttpStatus statusCode() {
			return this.statusCode;
		}

		@Override
		public final HttpHeaders headers() {
			return this.headers;
		}

		protected void writeStatusAndHeaders(ServerHttpResponse response) {
			response.setStatusCode(this.statusCode);
			HttpHeaders responseHeaders = response.getHeaders();

			if (!this.headers.isEmpty()) {
				this.headers.entrySet().stream()
						.filter(entry -> !responseHeaders.containsKey(entry.getKey()))
						.forEach(entry -> responseHeaders
								.put(entry.getKey(), entry.getValue()));
			}
		}
	}


	private static final class WriterFunctionServerResponse extends AbstractServerResponse {

		private final BiFunction<ServerWebExchange, HandlerStrategies, Mono<Void>> writeFunction;

		public WriterFunctionServerResponse(HttpStatus statusCode, HttpHeaders headers,
				BiFunction<ServerWebExchange, HandlerStrategies, Mono<Void>> writeFunction) {

			super(statusCode, headers);
			this.writeFunction = writeFunction;
		}

		@Override
		public Mono<Void> writeTo(ServerWebExchange exchange, HandlerStrategies strategies) {
			writeStatusAndHeaders(exchange.getResponse());
			return this.writeFunction.apply(exchange, strategies);
		}
	}


	private static final class BodyInserterServerResponse<T> extends AbstractServerResponse {

		private final BodyInserter<T, ? super ServerHttpResponse> inserter;

		private final Map<String, Object> hints;

		public BodyInserterServerResponse(HttpStatus statusCode, HttpHeaders headers,
				BodyInserter<T, ? super ServerHttpResponse> inserter, Map<String, Object> hints) {

			super(statusCode, headers);
			this.inserter = inserter;
			this.hints = hints;
		}

		@Override
		public Mono<Void> writeTo(ServerWebExchange exchange, HandlerStrategies strategies) {
			ServerHttpResponse response = exchange.getResponse();
			writeStatusAndHeaders(response);
			return this.inserter.insert(response, new BodyInserter.Context() {
				@Override
				public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
					return strategies.messageWriters();
				}
				@Override
				public Map<String, Object> hints() {
					return hints;
				}
			});
		}
	}

}
