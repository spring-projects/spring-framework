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
import java.util.function.Supplier;
import java.util.stream.Stream;

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
import org.springframework.web.server.ServerWebExchange;

/**
 * Default {@link EntityResponse.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultEntityResponseBuilder<T> implements EntityResponse.Builder<T> {

	private final T entity;

	private final BodyInserter<T, ? super ServerHttpResponse> inserter;

	private HttpStatus status = HttpStatus.OK;

	private final HttpHeaders headers = new HttpHeaders();

	private final Map<String, Object> hints = new HashMap<>();


	public DefaultEntityResponseBuilder(T entity, BodyInserter<T, ? super ServerHttpResponse> inserter) {
		this.entity = entity;
		this.inserter = inserter;
	}


	@Override
	public EntityResponse.Builder<T> status(HttpStatus status) {
		Assert.notNull(status, "'status' must not be null");
		this.status = status;
		return this;
	}

	@Override
	public EntityResponse.Builder<T> header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public EntityResponse.Builder<T> headers(HttpHeaders headers) {
		if (headers != null) {
			this.headers.putAll(headers);
		}
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public EntityResponse.Builder<T> allow(Set<HttpMethod> allowedMethods) {
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> eTag(String eTag) {
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
	public EntityResponse.Builder<T> hint(String key, Object value) {
		this.hints.put(key, value);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> lastModified(ZonedDateTime lastModified) {
		ZonedDateTime gmt = lastModified.withZoneSameInstant(ZoneId.of("GMT"));
		String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
		this.headers.set(HttpHeaders.LAST_MODIFIED, headerValue);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public EntityResponse.Builder<T> cacheControl(CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			this.headers.setCacheControl(cacheControl.getHeaderValue());
		}
		return this;
	}

	@Override
	public EntityResponse.Builder<T> varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public Mono<EntityResponse<T>> build() {
		return Mono.just(new DefaultEntityResponse<T>(this.status, this.headers, this.entity,
				this.inserter, this.hints));
	}


	private final static class DefaultEntityResponse<T>
			extends DefaultServerResponseBuilder.AbstractServerResponse
			implements EntityResponse<T> {

		private final T entity;

		private final BodyInserter<T, ? super ServerHttpResponse> inserter;

		private final Map<String, Object> hints;


		public DefaultEntityResponse(HttpStatus statusCode, HttpHeaders headers, T entity,
				BodyInserter<T, ? super ServerHttpResponse> inserter, Map<String, Object> hints) {

			super(statusCode, headers);
			this.entity = entity;
			this.inserter = inserter;
			this.hints = hints;
		}


		@Override
		public T entity() {
			return this.entity;
		}

		@Override
		public BodyInserter<T, ? super ServerHttpResponse> inserter() {
			return this.inserter;
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
