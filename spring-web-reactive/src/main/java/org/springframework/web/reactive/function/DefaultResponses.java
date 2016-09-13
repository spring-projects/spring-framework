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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
abstract class DefaultResponses {

	private static final ResolvableType RESOURCE_TYPE = ResolvableType.forClass(Resource.class);

	private static final ResolvableType SERVER_SIDE_EVENT_TYPE =
			ResolvableType.forClass(ServerSentEvent.class);

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					DefaultResponses.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							DefaultResponses.class.getClassLoader());


	public static Response<Void> empty(int statusCode, HttpHeaders headers) {
		return new DefaultResponse<>(statusCode, headers, null,
				exchange -> exchange.getResponse().setComplete()
		);
	}

	public static <T extends Publisher<Void>> Response<T> empty(int statusCode, HttpHeaders headers,
			T voidPublisher) {
		return new DefaultResponse<T>(statusCode, headers, voidPublisher,
				exchange -> Flux.from(voidPublisher)
						.then(exchange.getResponse().setComplete()));
	}

	public static Response<Resource> resource(int statusCode, HttpHeaders headers,
			Resource resource) {
		return new DefaultResponse<>(statusCode, headers, resource,
				exchange -> {
					ResourceHttpMessageWriter messageWriter = new ResourceHttpMessageWriter();
					MediaType contentType = exchange.getResponse().getHeaders().getContentType();
					return messageWriter
							.write(Mono.just(resource), RESOURCE_TYPE, contentType,
									exchange.getResponse(), Collections.emptyMap());

				});
	}

	public static <T> Response<T> body(int statusCode, HttpHeaders headers, T body) {
		return new DefaultResponse<T>(statusCode, headers, body,
				exchange -> writeWithMessageWriters(exchange, Mono.just(body),
						ResolvableType.forInstance(body)));
	}

	public static <S extends Publisher<T>, T> Response<S> stream(int statusCode,
			HttpHeaders headers, S publisher,
			Class<T> elementClass) {
		return new DefaultResponse<S>(statusCode, headers, publisher,
				exchange -> writeWithMessageWriters(exchange, publisher,
						ResolvableType.forClass(elementClass)));
	}

	public static <T, S extends Publisher<ServerSentEvent<T>>> Response<S> sse(int statusCode,
			HttpHeaders headers, S eventsPublisher) {
		return new DefaultResponse<S>(statusCode, headers, eventsPublisher,
				exchange -> {
					ServerSentEventHttpMessageWriter messageWriter =
							serverSentEventHttpMessageWriter();
					MediaType contentType = exchange.getResponse().getHeaders().getContentType();
					return messageWriter
							.write(eventsPublisher, SERVER_SIDE_EVENT_TYPE, contentType, exchange.getResponse(), Collections
									.emptyMap());
				});
	}

	public static <S extends Publisher<T>, T> Response<S> sse(int statusCode, HttpHeaders headers,
			S eventsPublisher,
			Class<T> eventClass) {
		return new DefaultResponse<S>(statusCode, headers, eventsPublisher,
				exchange -> {
					ServerSentEventHttpMessageWriter messageWriter =
							serverSentEventHttpMessageWriter();
					MediaType contentType = exchange.getResponse().getHeaders().getContentType();
					return messageWriter
							.write(eventsPublisher, ResolvableType.forClass(eventClass), contentType,
									exchange.getResponse(), Collections.emptyMap());
				});
	}

	public static Response<Rendering> render(int statusCode, HttpHeaders headers, String name,
			Map<String, Object> modelMap) {
		Rendering defaultRendering = new DefaultRendering(name, modelMap);
		return new DefaultResponse<>(statusCode, headers, defaultRendering,
				exchange -> {
					MediaType contentType = exchange.getResponse().getHeaders().getContentType();
					Locale locale = Locale.ENGLISH; // TODO: resolve locale
					Stream<ViewResolver> viewResolverStream = configuration(exchange).viewResolvers().get();
					return Flux.fromStream(viewResolverStream)
							.concatMap(viewResolver -> viewResolver.resolveViewName(name, locale))
							.next()
							.otherwiseIfEmpty(Mono.error(new IllegalArgumentException("Could not resolve view with name '" + name +"'")))
							.then(view -> view.render(modelMap, contentType, exchange));


				});
	}

	private static ServerSentEventHttpMessageWriter serverSentEventHttpMessageWriter() {
		return jackson2Present ? new ServerSentEventHttpMessageWriter(
				Collections.singletonList(new Jackson2JsonEncoder())) :
				new ServerSentEventHttpMessageWriter();
	}

	private static <T> Mono<Void> writeWithMessageWriters(ServerWebExchange exchange,
			Publisher<T> body,
			ResolvableType bodyType) {

		// TODO: use ContentNegotiatingResultHandlerSupport
		MediaType contentType = exchange.getResponse().getHeaders().getContentType();
		ServerHttpResponse response = exchange.getResponse();
		Stream<HttpMessageWriter<?>> messageWriterStream = configuration(exchange).messageWriters().get();
		return messageWriterStream
				.filter(messageWriter -> messageWriter.canWrite(bodyType, contentType, Collections
						.emptyMap()))
				.findFirst()
				.map(CastingUtils::cast)
				.map(messageWriter -> messageWriter.write(body, bodyType, contentType, response, Collections
						.emptyMap()))
				.orElseGet(() -> {
					response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
					return response.setComplete();
				});
	}

	private static Configuration configuration(ServerWebExchange exchange) {
		return exchange.<Configuration>getAttribute(
				RoutingFunctions.CONFIGURATION_ATTRIBUTE)
				.orElseThrow(() -> new IllegalStateException(
						"Could not find Configuration in ServerWebExchange"));
	}


	private static final class DefaultResponse<T> implements Response<T> {

		private final int statusCode;

		private final HttpHeaders headers;

		private final T body;

		private final Function<ServerWebExchange, Mono<Void>> writingFunction;


		public DefaultResponse(
				int statusCode, HttpHeaders headers, T body,
				Function<ServerWebExchange, Mono<Void>> writingFunction) {
			this.statusCode = statusCode;
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
			this.body = body;
			this.writingFunction = writingFunction;
		}

		@Override
		public HttpStatus statusCode() {
			return HttpStatus.valueOf(this.statusCode);
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public T body() {
			return this.body;
		}

		@Override
		public Mono<Void> writeTo(ServerWebExchange exchange) {
			writeStatusAndHeaders(exchange);
			return this.writingFunction.apply(exchange);
		}

		private void writeStatusAndHeaders(ServerWebExchange exchange) {
			ServerHttpResponse response = exchange.getResponse();
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

	private static class DefaultRendering implements Rendering {

		private final String name;

		private final Map<String, Object> model;


		public DefaultRendering(String name, Map<String, Object> model) {
			this.name = name;
			this.model = model;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public Map<String, Object> model() {
			return this.model;
		}
	}
}
