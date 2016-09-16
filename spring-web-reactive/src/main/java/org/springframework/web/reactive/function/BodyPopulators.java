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
 * Implementations of {@link BodyPopulator} that write various bodies, such a reactive streams,
 * server-sent events, resources, etc.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class BodyPopulators {

	private static final ResolvableType RESOURCE_TYPE = ResolvableType.forClass(Resource.class);

	private static final ResolvableType SERVER_SIDE_EVENT_TYPE =
			ResolvableType.forClass(ServerSentEvent.class);

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					BodyPopulators.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							BodyPopulators.class.getClassLoader());

	/**
	 * Return a {@code BodyPopulator} that writes the given single object.
	 * @param body the body of the response
	 * @return a {@code BodyPopulator} that writes a single object
	 */
	public static <T> BodyPopulator<T> ofObject(T body) {
		Assert.notNull(body, "'body' must not be null");
		return BodyPopulator.of(
				(response, configuration) -> writeWithMessageWriters(response, configuration,
						Mono.just(body), ResolvableType.forInstance(body)),
				() -> body);
	}

	/**
	 * Return a {@code BodyPopulator} that writes the given {@link Publisher}.
	 * @param publisher the publisher to stream to the response body
	 * @param elementClass the class of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <S> the type of the {@code Publisher}.
	 * @return a {@code BodyPopulator} that writes a {@code Publisher}
	 */
	public static <S extends Publisher<T>, T> BodyPopulator<S> ofPublisher(S publisher,
			Class<T> elementClass) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return ofPublisher(publisher, ResolvableType.forClass(elementClass));
	}

	/**
	 * Return a {@code BodyPopulator} that writes the given {@link Publisher}.
	 * @param publisher the publisher to stream to the response body
	 * @param elementType the type of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <S> the type of the {@code Publisher}.
	 * @return a {@code BodyPopulator} that writes a {@code Publisher}
	 */
	public static <S extends Publisher<T>, T> BodyPopulator<S> ofPublisher(S publisher,
			ResolvableType elementType) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");
		return BodyPopulator.of(
				(response, configuration) -> writeWithMessageWriters(response, configuration,
						publisher, elementType),
				() -> publisher
		);
	}

	/**
	 * Return a {@code BodyPopulator} that writes the given {@code Resource}.
	 * If the resource can be resolved to a {@linkplain Resource#getFile() file}, it will be copied
	 * using
	 * <a href="https://en.wikipedia.org/wiki/Zero-copy">zero-copy</a>
	 * @param resource the resource to write to the response
	 * @param <T> the type of the {@code Resource}
	 * @return a {@code BodyPopulator} that writes a {@code Publisher}
	 */
	public static <T extends Resource> BodyPopulator<T> ofResource(T resource) {
		Assert.notNull(resource, "'resource' must not be null");
		return BodyPopulator.of(
				(response, configuration) -> {
					ResourceHttpMessageWriter messageWriter = new ResourceHttpMessageWriter();
					MediaType contentType = response.getHeaders().getContentType();
					return messageWriter.write(Mono.just(resource), RESOURCE_TYPE, contentType,
							response, Collections.emptyMap());
				},
				() -> resource
		);
	}

	/**
	 * Return a {@code BodyPopulator} that writes the given {@code ServerSentEvent} publisher.
	 * @param eventsPublisher the {@code ServerSentEvent} publisher to write to the response body
	 * @param <T> the type of the elements contained in the {@link ServerSentEvent}
	 * @return a {@code BodyPopulator} that writes a {@code ServerSentEvent} publisher
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static <T, S extends Publisher<ServerSentEvent<T>>> BodyPopulator<S> ofServerSentEvents(
			S eventsPublisher) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		return BodyPopulator.of(
				(response, configuration) -> {
					ServerSentEventHttpMessageWriter messageWriter = sseMessageWriter();
					MediaType contentType = response.getHeaders().getContentType();
					return messageWriter.write(eventsPublisher, SERVER_SIDE_EVENT_TYPE,
							contentType, response, Collections.emptyMap());
				},
				() -> eventsPublisher
		);
	}

	/**
	 * Return a {@code BodyPopulator} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events.
	 * @param eventsPublisher the publisher to write to the response body as Server-Sent Events
	 * @param eventClass the class of event contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @return a {@code BodyPopulator} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static <T, S extends Publisher<T>> BodyPopulator<S> ofServerSentEvents(S eventsPublisher,
			Class<T> eventClass) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		Assert.notNull(eventClass, "'eventClass' must not be null");
		return ofServerSentEvents(eventsPublisher, ResolvableType.forClass(eventClass));
	}

	/**
	 * Return a {@code BodyPopulator} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events.
	 * @param eventsPublisher the publisher to write to the response body as Server-Sent Events
	 * @param eventType the type of event contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @return a {@code BodyPopulator} that writes the given {@code Publisher} publisher as
	 * Server-Sent Events
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static <T, S extends Publisher<T>> BodyPopulator<S> ofServerSentEvents(S eventsPublisher,
			ResolvableType eventType) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		Assert.notNull(eventType, "'eventType' must not be null");
		return BodyPopulator.of(
				(response, configuration) -> {
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
			Configuration configuration,
			Publisher<T> body,
			ResolvableType bodyType) {

		// TODO: use ContentNegotiatingResultHandlerSupport
		MediaType contentType = response.getHeaders().getContentType();
		return configuration.messageWriters().get()
				.filter(messageWriter -> messageWriter.canWrite(bodyType, contentType, Collections
						.emptyMap()))
				.findFirst()
				.map(BodyPopulators::cast)
				.map(messageWriter -> messageWriter
						.write(body, bodyType, contentType, response, Collections
								.emptyMap()))
				.orElseGet(() -> {
					response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
					return response.setComplete();
				});
	}

	@SuppressWarnings("unchecked")
	public static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}

	static class DefaultBodyPopulator<T> implements BodyPopulator<T> {

		private final BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer;

		private final Supplier<T> supplier;

		public DefaultBodyPopulator(
				BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer,
				Supplier<T> supplier) {
			this.writer = writer;
			this.supplier = supplier;
		}

		@Override
		public BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer() {
			return this.writer;
		}

		@Override
		public Supplier<T> supplier() {
			return this.supplier;
		}
	}


}
