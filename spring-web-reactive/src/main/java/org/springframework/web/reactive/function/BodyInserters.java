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
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Implementations of {@link BodyInserter} that write various bodies, such a reactive streams,
 * server-sent events, resources, etc.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class BodyInserters {

	private static final ResolvableType RESOURCE_TYPE = ResolvableType.forClass(Resource.class);

	private static final ResolvableType SERVER_SIDE_EVENT_TYPE =
			ResolvableType.forClass(ServerSentEvent.class);

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					BodyInserters.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							BodyInserters.class.getClassLoader());

	/**
	 * Return a {@code BodyInserter} that writes the given single object.
	 * @param body the body of the response
	 * @return a {@code BodyInserter} that writes a single object
	 */
	public static <T> BodyInserter<T> fromObject(T body) {
		Assert.notNull(body, "'body' must not be null");
		return BodyInserter.of(
				(response, strategies) -> writeWithMessageWriters(response, strategies,
						Mono.just(body), ResolvableType.forInstance(body)),
				() -> body);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@link Publisher}.
	 * @param publisher the publisher to stream to the response body
	 * @param elementClass the class of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <S> the type of the {@code Publisher}.
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <S extends Publisher<T>, T> BodyInserter<S> fromPublisher(S publisher,
			Class<T> elementClass) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return fromPublisher(publisher, ResolvableType.forClass(elementClass));
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@link Publisher}.
	 * @param publisher the publisher to stream to the response body
	 * @param elementType the type of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <S> the type of the {@code Publisher}.
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <S extends Publisher<T>, T> BodyInserter<S> fromPublisher(S publisher,
			ResolvableType elementType) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");
		return BodyInserter.of(
				(response, strategies) -> writeWithMessageWriters(response, strategies,
						publisher, elementType),
				() -> publisher
		);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code Resource}.
	 * If the resource can be resolved to a {@linkplain Resource#getFile() file}, it will be copied
	 * using
	 * <a href="https://en.wikipedia.org/wiki/Zero-copy">zero-copy</a>
	 * @param resource the resource to write to the response
	 * @param <T> the type of the {@code Resource}
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <T extends Resource> BodyInserter<T> fromResource(T resource) {
		Assert.notNull(resource, "'resource' must not be null");
		return BodyInserter.of(
				(response, strategies) -> {
					ResourceHttpMessageWriter messageWriter = new ResourceHttpMessageWriter();
					MediaType contentType = response.getHeaders().getContentType();
					return messageWriter.write(Mono.just(resource), RESOURCE_TYPE, contentType,
							response, Collections.emptyMap());
				},
				() -> resource
		);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code ServerSentEvent} publisher.
	 * @param eventsPublisher the {@code ServerSentEvent} publisher to write to the response body
	 * @param <T> the type of the elements contained in the {@link ServerSentEvent}
	 * @return a {@code BodyInserter} that writes a {@code ServerSentEvent} publisher
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static <T, S extends Publisher<ServerSentEvent<T>>> BodyInserter<S> fromServerSentEvents(
			S eventsPublisher) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		return BodyInserter.of(
				(response, strategies) -> {
					ServerSentEventHttpMessageWriter messageWriter = sseMessageWriter();
					MediaType contentType = response.getHeaders().getContentType();
					return messageWriter.write(eventsPublisher, SERVER_SIDE_EVENT_TYPE,
							contentType, response, Collections.emptyMap());
				},
				() -> eventsPublisher
		);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events.
	 * @param eventsPublisher the publisher to write to the response body as Server-Sent Events
	 * @param eventClass the class of event contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @return a {@code BodyInserter} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static <T, S extends Publisher<T>> BodyInserter<S> fromServerSentEvents(S eventsPublisher,
			Class<T> eventClass) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		Assert.notNull(eventClass, "'eventClass' must not be null");
		return fromServerSentEvents(eventsPublisher, ResolvableType.forClass(eventClass));
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events.
	 * @param eventsPublisher the publisher to write to the response body as Server-Sent Events
	 * @param eventType the type of event contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @return a {@code BodyInserter} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static <T, S extends Publisher<T>> BodyInserter<S> fromServerSentEvents(S eventsPublisher,
			ResolvableType eventType) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		Assert.notNull(eventType, "'eventType' must not be null");
		return BodyInserter.of(
				(response, strategies) -> {
					ServerSentEventHttpMessageWriter messageWriter = sseMessageWriter();
					MediaType contentType = response.getHeaders().getContentType();
					return messageWriter.write(eventsPublisher, eventType, contentType, response,
							Collections.emptyMap());

				},
				() -> eventsPublisher
		);
	}

	private static ServerSentEventHttpMessageWriter sseMessageWriter() {
		return jackson2Present ? new ServerSentEventHttpMessageWriter(
				Collections.singletonList(new Jackson2JsonEncoder())) :
				new ServerSentEventHttpMessageWriter();
	}

	private static <T> Mono<Void> writeWithMessageWriters(ServerHttpResponse response,
			StrategiesSupplier strategies,
			Publisher<T> body,
			ResolvableType bodyType) {

		// TODO: use ContentNegotiatingResultHandlerSupport
		MediaType contentType = response.getHeaders().getContentType();
		return strategies.messageWriters().get()
				.filter(messageWriter -> messageWriter.canWrite(bodyType, contentType))
				.findFirst()
				.map(BodyInserters::cast)
				.map(messageWriter -> messageWriter
						.write(body, bodyType, contentType, response, Collections
								.emptyMap()))
				.orElseGet(() -> {
					response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
					return response.setComplete();
				});
	}

	@SuppressWarnings("unchecked")
	private static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}

	static class DefaultBodyInserter<T> implements BodyInserter<T> {

		private final BiFunction<ServerHttpResponse, StrategiesSupplier, Mono<Void>> writer;

		private final Supplier<T> supplier;

		public DefaultBodyInserter(
				BiFunction<ServerHttpResponse, StrategiesSupplier, Mono<Void>> writer,
				Supplier<T> supplier) {
			this.writer = writer;
			this.supplier = supplier;
		}

		@Override
		public Mono<Void> insert(ServerHttpResponse response, StrategiesSupplier strategies) {
			return this.writer.apply(response, strategies);
		}

		@Override
		public T t() {
			return this.supplier.get();
		}

	}


}
