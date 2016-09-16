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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Conventions;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default {@link Response.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 */
class DefaultResponseBuilder implements Response.BodyBuilder {

	private final int statusCode;

	private final HttpHeaders headers = new HttpHeaders();

	public DefaultResponseBuilder(int statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public Response.BodyBuilder header(String headerName, String... headerValues) {
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public Response.BodyBuilder headers(HttpHeaders headers) {
		if (headers != null) {
			this.headers.putAll(headers);
		}
		return this;
	}

	@Override
	public Response.BodyBuilder allow(HttpMethod... allowedMethods) {
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public Response.BodyBuilder contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public Response.BodyBuilder contentType(MediaType contentType) {
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public Response.BodyBuilder eTag(String eTag) {
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
	public Response.BodyBuilder lastModified(ZonedDateTime lastModified) {
		ZonedDateTime gmt = lastModified.withZoneSameInstant(ZoneId.of("GMT"));
		String headerValue = DateTimeFormatter.RFC_1123_DATE_TIME.format(gmt);
		this.headers.set(HttpHeaders.LAST_MODIFIED, headerValue);
		return this;
	}

	@Override
	public Response.BodyBuilder location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public Response.BodyBuilder cacheControl(CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			this.headers.setCacheControl(cacheControl.getHeaderValue());
		}
		return this;
	}

	@Override
	public Response.BodyBuilder varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public Response<Void> build() {
		return body(BodyPopulator.of(
				(response, configuration) -> response.setComplete(),
				() -> null));
	}

	@Override
	public <T extends Publisher<Void>> Response<T> build(T voidPublisher) {
		Assert.notNull(voidPublisher, "'voidPublisher' must not be null");
		return body(BodyPopulator.of(
				(response, configuration) -> Flux.from(voidPublisher).then(response.setComplete()),
				() -> null));
	}

	@Override
	public <T> Response<T> body(BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer,
			Supplier<T> supplier) {
		return body(BodyPopulator.of(writer, supplier));
	}

	@Override
	public <T> Response<T> body(BodyPopulator<T> populator) {
		Assert.notNull(populator, "'populator' must not be null");
		return new BodyPopulatorResponse<T>(this.statusCode, this.headers, populator);
	}

	@Override
	public Response<Rendering> render(String name, Object... modelAttributes) {
		Assert.hasLength(name, "'name' must not be empty");
		return render(name, toModelMap(modelAttributes));
	}

	private static Map<String, Object> toModelMap(Object[] modelAttributes) {
		if (!ObjectUtils.isEmpty(modelAttributes)) {
			return Arrays.stream(modelAttributes)
					.filter(o -> !isEmptyCollection(o))
					.collect(Collectors.toMap(Conventions::getVariableName, o -> o));
		}
		else {
			return null;
		}
	}

	private static boolean isEmptyCollection(Object o) {
		return o instanceof Collection && ((Collection<?>) o).isEmpty();
	}

	@Override
	public Response<Rendering> render(String name, Map<String, ?> model) {
		Assert.hasLength(name, "'name' must not be empty");
		Map<String, Object> modelMap = new LinkedHashMap<>();
		if (model != null) {
			modelMap.putAll(model);
		}
		return new RenderingResponse(this.statusCode, this.headers, name, modelMap);
	}


	private static abstract class AbstractResponse<T> implements Response<T> {

		private final int statusCode;

		private final HttpHeaders headers;


		protected AbstractResponse(int statusCode, HttpHeaders headers) {
			this.statusCode = statusCode;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
		}

		@Override
		public final HttpStatus statusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public final HttpHeaders headers() {
			return this.headers;
		}

		protected void writeStatusAndHeaders(ServerHttpResponse response) {
			response.setStatusCode(HttpStatus.valueOf(this.statusCode));
			HttpHeaders responseHeaders = response.getHeaders();

			if (!this.headers.isEmpty()) {
				this.headers.entrySet().stream()
						.filter(entry -> !responseHeaders.containsKey(entry.getKey()))
						.forEach(entry -> responseHeaders
								.put(entry.getKey(), entry.getValue()));
			}
		}
	}

	private static final class BodyPopulatorResponse<T> extends AbstractResponse<T> {

		private final BodyPopulator<T> populator;


		public BodyPopulatorResponse(
				int statusCode, HttpHeaders headers, BodyPopulator<T> populator) {
			super(statusCode, headers);
			this.populator = populator;
		}

		@Override
		public T body() {
			return this.populator.supplier().get();
		}

		@Override
		public Mono<Void> writeTo(ServerWebExchange exchange, Configuration configuration) {
			ServerHttpResponse response = exchange.getResponse();
			writeStatusAndHeaders(response);
			return this.populator.writer().apply(response, configuration);
		}

	}


	private static final class RenderingResponse extends AbstractResponse<Rendering> {

		private final String name;

		private final Map<String, Object> model;

		private final Rendering rendering;

		public RenderingResponse(int statusCode, HttpHeaders headers, String name,
				Map<String, Object> model) {
			super(statusCode, headers);
			this.name = name;
			this.model = model;
			this.rendering = new DefaultRendering();
		}

		@Override
		public Rendering body() {
			return this.rendering;
		}

		@Override
		public Mono<Void> writeTo(ServerWebExchange exchange, Configuration configuration) {
			ServerHttpResponse response = exchange.getResponse();
			writeStatusAndHeaders(response);
			MediaType contentType = exchange.getResponse().getHeaders().getContentType();
			Locale locale = Locale.ENGLISH; // TODO: resolve locale
			Stream<ViewResolver> viewResolverStream = configuration.viewResolvers().get();
			return Flux.fromStream(viewResolverStream)
					.concatMap(viewResolver -> viewResolver.resolveViewName(this.name, locale))
					.next()
					.otherwiseIfEmpty(Mono.error(new IllegalArgumentException("Could not resolve view with name '" +
							this.name +"'")))
					.then(view -> view.render(this.model, contentType, exchange));
		}

		private class DefaultRendering implements Rendering {

			@Override
			public String name() {
				return name;
			}

			@Override
			public Map<String, Object> model() {
				return model;
			}
		}


	}


}
