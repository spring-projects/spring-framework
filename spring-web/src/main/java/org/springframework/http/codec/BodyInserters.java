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

package org.springframework.http.codec;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;

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

	/**
	 * Return a {@code BodyInserter} that writes the given single object.
	 * @param body the body of the response
	 * @return a {@code BodyInserter} that writes a single object
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromObject(T body) {
		Assert.notNull(body, "'body' must not be null");
		return BodyInserter.of(
				writeFunctionFor(Mono.just(body), ResolvableType.forInstance(body)),
				() -> body);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@link Publisher}.
	 * @param publisher the publisher to stream to the response body
	 * @param elementClass the class of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <S> the type of the {@code Publisher}
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <S extends Publisher<T>, T> BodyInserter<S, ReactiveHttpOutputMessage> fromPublisher(S publisher,
			Class<T> elementClass) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return BodyInserter.of(
				writeFunctionFor(publisher, ResolvableType.forClass(elementClass)),
				() -> publisher
		);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@link Publisher}.
	 * @param publisher the publisher to stream to the response body
	 * @param elementType the type of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <S> the type of the {@code Publisher}
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <S extends Publisher<T>, T> BodyInserter<S, ReactiveHttpOutputMessage> fromPublisher(S publisher,
			ResolvableType elementType) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");
		return BodyInserter.of(
				writeFunctionFor(publisher, elementType),
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
	public static <T extends Resource> BodyInserter<T, ReactiveHttpOutputMessage> fromResource(T resource) {
		Assert.notNull(resource, "'resource' must not be null");
		return BodyInserter.of(
				(response, context) -> {
					HttpMessageWriter<Resource> messageWriter = resourceHttpMessageWriter(context);
					return messageWriter.write(Mono.just(resource), RESOURCE_TYPE, null,
							response, Collections.emptyMap());
				},
				() -> resource
		);
	}

	private static HttpMessageWriter<Resource> resourceHttpMessageWriter(BodyInserter.Context context) {
		return context.messageWriters().get()
				.filter(messageWriter -> messageWriter.canWrite(RESOURCE_TYPE, null))
				.findFirst()
				.map(BodyInserters::<Resource>cast)
				.orElseThrow(() -> new IllegalStateException(
						"Could not find HttpMessageWriter that supports Resources."));
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code ServerSentEvent} publisher.
	 * @param eventsPublisher the {@code ServerSentEvent} publisher to write to the response body
	 * @param <T> the type of the elements contained in the {@link ServerSentEvent}
	 * @return a {@code BodyInserter} that writes a {@code ServerSentEvent} publisher
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static <T, S extends Publisher<ServerSentEvent<T>>> BodyInserter<S, ServerHttpResponse> fromServerSentEvents(
			S eventsPublisher) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		return BodyInserter.of(
				(response, context) -> {
					HttpMessageWriter<ServerSentEvent<T>> messageWriter = sseMessageWriter(context);
					return messageWriter.write(eventsPublisher, SERVER_SIDE_EVENT_TYPE,
							MediaType.TEXT_EVENT_STREAM, response, Collections.emptyMap());
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
	public static <T, S extends Publisher<T>> BodyInserter<S, ServerHttpResponse> fromServerSentEvents(S eventsPublisher,
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
	public static <T, S extends Publisher<T>> BodyInserter<S, ServerHttpResponse> fromServerSentEvents(S eventsPublisher,
			ResolvableType eventType) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		Assert.notNull(eventType, "'eventType' must not be null");
		return BodyInserter.of(
				(outputMessage, context) -> {
					HttpMessageWriter<T> messageWriter = sseMessageWriter(context);
					return messageWriter.write(eventsPublisher, eventType,
							MediaType.TEXT_EVENT_STREAM, outputMessage, Collections.emptyMap());

				},
				() -> eventsPublisher
		);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code Publisher<DataBuffer>} to the
	 * body.
	 * @param publisher the data buffer publisher to write
	 * @param <T> the type of the publisher
	 * @return a {@code BodyInserter} that writes directly to the body
	 * @see ReactiveHttpOutputMessage#writeWith(Publisher)
	 */
	public static <T extends Publisher<DataBuffer>> BodyInserter<T, ReactiveHttpOutputMessage> fromDataBuffers(T publisher) {
		Assert.notNull(publisher, "'publisher' must not be null");

		return BodyInserter.of(
				(outputMessage, context) -> outputMessage.writeWith(publisher),
				() -> publisher
		);
	}

	private static <T> HttpMessageWriter<T> sseMessageWriter(BodyInserter.Context context) {
		return context.messageWriters().get()
				.filter(messageWriter -> messageWriter
						.canWrite(SERVER_SIDE_EVENT_TYPE, MediaType.TEXT_EVENT_STREAM))
				.findFirst()
				.map(BodyInserters::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"Could not find HttpMessageWriter that supports " +
								MediaType.TEXT_EVENT_STREAM_VALUE));
	}

	private static <T, M extends ReactiveHttpOutputMessage> BiFunction<M, BodyInserter.Context, Mono<Void>>
		writeFunctionFor(Publisher<T> body, ResolvableType bodyType) {

		return (m, context) -> {

			MediaType contentType = m.getHeaders().getContentType();
			Supplier<Stream<HttpMessageWriter<?>>> messageWriters = context.messageWriters();
			return messageWriters.get()
					.filter(messageWriter -> messageWriter.canWrite(bodyType, contentType))
					.findFirst()
					.map(BodyInserters::cast)
					.map(messageWriter -> messageWriter
							.write(body, bodyType, contentType, m, Collections
									.emptyMap()))
					.orElseGet(() -> {
						List<MediaType> supportedMediaTypes = messageWriters.get()
								.flatMap(reader -> reader.getWritableMediaTypes().stream())
								.collect(Collectors.toList());
						UnsupportedMediaTypeException error =
								new UnsupportedMediaTypeException(contentType, supportedMediaTypes);
						return Mono.error(error);
					});
		};
	}

	@SuppressWarnings("unchecked")
	private static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}

	static class DefaultBodyInserter<T, M extends ReactiveHttpOutputMessage>
			implements BodyInserter<T, M> {

		private final BiFunction<M, Context, Mono<Void>> writer;

		private final Supplier<T> supplier;

		public DefaultBodyInserter(
				BiFunction<M, Context, Mono<Void>> writer,
				Supplier<T> supplier) {
			this.writer = writer;
			this.supplier = supplier;
		}

		@Override
		public Mono<Void> insert(M outputMessage, Context context) {
			return this.writer.apply(outputMessage, context);
		}

		@Override
		public T t() {
			return this.supplier.get();
		}

	}


}
