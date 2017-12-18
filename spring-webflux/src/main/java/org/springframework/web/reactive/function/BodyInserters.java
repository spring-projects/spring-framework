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

package org.springframework.web.reactive.function;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Implementations of {@link BodyInserter} that write various bodies, such a reactive streams,
 * server-sent events, resources, etc.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class BodyInserters {

	private static final ResolvableType RESOURCE_TYPE =
			ResolvableType.forClass(Resource.class);

	private static final ResolvableType SERVER_SIDE_EVENT_TYPE =
			ResolvableType.forClass(ServerSentEvent.class);

	private static final ResolvableType FORM_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MULTIPART_VALUE_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Object.class);

	private static final BodyInserter<Void, ReactiveHttpOutputMessage> EMPTY =
					(response, context) -> response.setComplete();


	/**
	 * Return an empty {@code BodyInserter} that writes nothing.
	 * @return an empty {@code BodyInserter}
	 */
	@SuppressWarnings("unchecked")
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> empty() {
		return (BodyInserter<T, ReactiveHttpOutputMessage>)EMPTY;
	}

	/**
	 * Return a {@code BodyInserter} that writes the given single object.
	 * <p>Note also that
	 * {@link org.springframework.web.reactive.function.client.WebClient WebClient} and
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse}
	 * each offer a {@code syncBody(Object)} shortcut for providing an Object
	 * as the body.
	 * @param body the body of the response
	 * @return a {@code BodyInserter} that writes a single object
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromObject(T body) {
		Assert.notNull(body, "'body' must not be null");
		return bodyInserterFor(Mono.just(body), ResolvableType.forInstance(body));
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@link Publisher}.
	 * <p>Note also that
	 * {@link org.springframework.web.reactive.function.client.WebClient WebClient} and
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse}
	 * each offer {@code body} shortcut methods for providing a Publisher as the body.
	 * @param publisher the publisher to stream to the response body
	 * @param elementClass the class of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, Class<T> elementClass) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return bodyInserterFor(publisher, ResolvableType.forClass(elementClass));
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@link Publisher}.
	 * <p>Note also that
	 * {@link org.springframework.web.reactive.function.client.WebClient WebClient} and
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse}
	 * each offer {@code body} shortcut methods for providing a Publisher as the body.
	 * @param publisher the publisher to stream to the response body
	 * @param typeReference the type of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, ParameterizedTypeReference<T> typeReference) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(typeReference, "'typeReference' must not be null");
		return bodyInserterFor(publisher, ResolvableType.forType(typeReference.getType()));
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code Resource}.
	 * <p>If the resource can be resolved to a {@linkplain Resource#getFile() file}, it will
	 * be copied using <a href="https://en.wikipedia.org/wiki/Zero-copy">zero-copy</a>.
	 * @param resource the resource to write to the output message
	 * @param <T> the type of the {@code Resource}
	 * @return a {@code BodyInserter} that writes a {@code Publisher}
	 */
	public static <T extends Resource> BodyInserter<T, ReactiveHttpOutputMessage> fromResource(T resource) {
		Assert.notNull(resource, "'resource' must not be null");
		return (outputMessage, context) -> {
			Mono<T> inputStream = Mono.just(resource);
			HttpMessageWriter<Resource> messageWriter = resourceHttpMessageWriter(context);
			Optional<ServerHttpRequest> serverRequest = context.serverRequest();
			if (serverRequest.isPresent() && outputMessage instanceof ServerHttpResponse) {
				return messageWriter.write(inputStream, RESOURCE_TYPE, RESOURCE_TYPE, null,
						serverRequest.get(), (ServerHttpResponse) outputMessage, context.hints());
			}
			else {
				return messageWriter.write(inputStream, RESOURCE_TYPE, null, outputMessage, context.hints());
			}
		};
	}

	private static HttpMessageWriter<Resource> resourceHttpMessageWriter(BodyInserter.Context context) {
		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(RESOURCE_TYPE, null))
				.findFirst()
				.map(BodyInserters::<Resource>cast)
				.orElseThrow(() -> new IllegalStateException(
						"Could not find HttpMessageWriter that supports Resource objects"));
	}

	/**
	 * Return a {@code BodyInserter} that writes the given {@code ServerSentEvent} publisher.
	 * <p>Note that a SSE {@code BodyInserter} can also be obtained by passing a stream of strings
	 * or POJOs (to be encoded as JSON) to {@link #fromPublisher(Publisher, Class)}, and specifying a
	 * {@link MediaType#TEXT_EVENT_STREAM text/event-stream} Content-Type.
	 * @param eventsPublisher the {@code ServerSentEvent} publisher to write to the response body
	 * @param <T> the type of the elements contained in the {@link ServerSentEvent}
	 * @return a {@code BodyInserter} that writes a {@code ServerSentEvent} publisher
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	// Note that the returned BodyInserter is parameterized to ServerHttpResponse, not
	// ReactiveHttpOutputMessage like other methods, since sending SSEs only typically happens on
	// the server-side
	public static <T, S extends Publisher<ServerSentEvent<T>>> BodyInserter<S, ServerHttpResponse> fromServerSentEvents(
			S eventsPublisher) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		return (serverResponse, context) -> {
			HttpMessageWriter<ServerSentEvent<T>> messageWriter =
					findMessageWriter(context, SERVER_SIDE_EVENT_TYPE, MediaType.TEXT_EVENT_STREAM);
			return context.serverRequest()
					.map(serverRequest -> messageWriter.write(eventsPublisher, SERVER_SIDE_EVENT_TYPE,
							SERVER_SIDE_EVENT_TYPE, MediaType.TEXT_EVENT_STREAM, serverRequest,
							serverResponse, context.hints()))
					.orElseGet(() -> messageWriter.write(eventsPublisher, SERVER_SIDE_EVENT_TYPE,
							MediaType.TEXT_EVENT_STREAM, serverResponse, context.hints()));
		};
	}

	/**
	 * Return a {@link FormInserter} that writes the given {@code MultiValueMap}
	 * as URL-encoded form data. The returned inserter allows for additional
	 * entries to be added via {@link FormInserter#with(String, Object)}.
	 *
	 * <p>Note that you can also use the {@code syncBody(Object)} method in the
	 * request builders of both the {@code WebClient} and {@code WebTestClient}.
	 * In that case the setting of the content type is also not required, just
	 * be sure the map contains String values only or otherwise it would be
	 * interpreted as a multipart request.
	 *
	 * @param formData the form data to write to the output message
	 * @return a {@code FormInserter} that writes form data
	 */
	// Note that the returned FormInserter is parameterized to ClientHttpRequest, not
	// ReactiveHttpOutputMessage like other methods, since sending form data only typically happens
	// on the client-side
	public static FormInserter<String> fromFormData(MultiValueMap<String, String> formData) {

		Assert.notNull(formData, "'formData' must not be null");

		return DefaultFormInserter.forFormData().with(formData);
	}

	/**
	 * Return a {@link FormInserter} that writes the given key-value pair as
	 * URL-encoded form data. The returned inserter allows for additional
	 * entries to be added via {@link FormInserter#with(String, Object)}.
	 * @param key the key to add to the form
	 * @param value the value to add to the form
	 * @return a {@code FormInserter} that writes form data
	 */
	// Note that the returned FormInserter is parameterized to ClientHttpRequest, not
	// ReactiveHttpOutputMessage like other methods, since sending form data only typically happens
	// on the client-side
	public static FormInserter<String> fromFormData(String key, String value) {
		Assert.notNull(key, "'key' must not be null");
		Assert.notNull(value, "'value' must not be null");

		return DefaultFormInserter.forFormData().with(key, value);
	}

	/**
	 * Return a {@code FormInserter} that writes the given {@code MultiValueMap}
	 * as multipart data. The values in the {@code MultiValueMap} can be any
	 * Object representing the body of the part, or an
	 * {@link org.springframework.http.HttpEntity HttpEntity} representing a part
	 * with body and headers. The {@code MultiValueMap} can be built conveniently
	 * using {@link org.springframework.http.client.MultipartBodyBuilder
	 * MultipartBodyBuilder}. Also the returned inserter allows for additional
	 * entries to be added via {@link FormInserter#with(String, Object)}.
	 *
	 * <p>Note that you can also use the {@code syncBody(Object)} method in the
	 * request builders of both the {@code WebClient} and {@code WebTestClient}.
	 * In that case the setting of the content type is also not required, just
	 * be sure the map contains at least one non-String value or otherwise,
	 * without a content-type header as a hint, it would be interpreted as a
	 * plain form data request.
	 *
	 * @param multipartData the form data to write to the output message
	 * @return a {@code BodyInserter} that writes multipart data
	 */
	// Note that the returned BodyInserter is parameterized to ClientHttpRequest, not
	// ReactiveHttpOutputMessage like other methods, since sending form data only typically happens
	// on the client-side
	public static <T> FormInserter<T> fromMultipartData(MultiValueMap<String, T> multipartData) {

		Assert.notNull(multipartData, "'multipartData' must not be null");

		return DefaultFormInserter.<T>forMultipartData().with(multipartData);
	}

	/**
	 * A variant of {@link #fromMultipartData(MultiValueMap)} for adding
	 * parts as name-value pairs in-line vs building a {@code MultiValueMap}
	 * and passing it in.
	 * @param key the part name
	 * @param value the part value, an Object or {@code HttpEntity}
	 * @return a {@code FormInserter} that can writes the provided multipart
	 * data and also allows adding more parts
	 */
	// Note that the returned BodyInserter is parameterized to ClientHttpRequest, not
	// ReactiveHttpOutputMessage like other methods, since sending form data only typically happens
	// on the client-side
	public static <T> FormInserter<T> fromMultipartData(String key, T value) {
		Assert.notNull(key, "'key' must not be null");
		Assert.notNull(value, "'value' must not be null");

		return DefaultFormInserter.<T>forMultipartData().with(key, value);
	}

	/**
	 * Return a {@code BodyInserter} that writes the given
	 * {@code Publisher<DataBuffer>} to the body.
	 * @param publisher the data buffer publisher to write
	 * @param <T> the type of the publisher
	 * @return a {@code BodyInserter} that writes directly to the body
	 * @see ReactiveHttpOutputMessage#writeWith(Publisher)
	 */
	public static <T extends Publisher<DataBuffer>> BodyInserter<T, ReactiveHttpOutputMessage> fromDataBuffers(
			T publisher) {

		Assert.notNull(publisher, "'publisher' must not be null");
		return (outputMessage, context) -> outputMessage.writeWith(publisher);
	}


	private static <T, P extends Publisher<?>, M extends ReactiveHttpOutputMessage> BodyInserter<T, M> bodyInserterFor(
			P body, ResolvableType bodyType) {

		return (outputMessage, context) -> {
			MediaType contentType = outputMessage.getHeaders().getContentType();
			List<HttpMessageWriter<?>> messageWriters = context.messageWriters();
			return messageWriters.stream()
					.filter(messageWriter -> messageWriter.canWrite(bodyType, contentType))
					.findFirst()
					.map(BodyInserters::cast)
					.map(messageWriter -> {
						Optional<ServerHttpRequest> serverRequest = context.serverRequest();
						if (serverRequest.isPresent() && outputMessage instanceof ServerHttpResponse) {
							return messageWriter.write(body, bodyType, bodyType, contentType,
									serverRequest.get(), (ServerHttpResponse) outputMessage,
									context.hints());
						} else {
							return messageWriter.write(body, bodyType, contentType, outputMessage,
											context.hints());
						}
					})
					.orElseGet(() -> {
						List<MediaType> supportedMediaTypes = messageWriters.stream()
								.flatMap(reader -> reader.getWritableMediaTypes().stream())
								.collect(Collectors.toList());
						UnsupportedMediaTypeException error =
								new UnsupportedMediaTypeException(contentType, supportedMediaTypes);
						return Mono.error(error);
					});
		};
	}

	private static <T> HttpMessageWriter<T> findMessageWriter(
			BodyInserter.Context context, ResolvableType type, MediaType mediaType) {

		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(type, mediaType))
				.findFirst()
				.map(BodyInserters::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"Could not find HttpMessageWriter that supports " + mediaType));
	}

	@SuppressWarnings("unchecked")
	private static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}


	/**
	 * Sub-interface of {@link BodyInserter} that allows for additional (multipart) form data to be
	 * added.
	 */
	public interface FormInserter<T> extends
			BodyInserter<MultiValueMap<String, T>, ClientHttpRequest> {

		/**
		 * Adds the specified key-value pair to the form.
		 * @param key the key to be added
		 * @param value the value to be added
		 * @return this inserter
		 */
		FormInserter<T> with(String key, @Nullable T value);

		/**
		 * Adds the specified values to the form.
		 * @param values the values to be added
		 * @return this inserter
		 */
		FormInserter<T> with(MultiValueMap<String, T> values);

	}

	private static class DefaultFormInserter<T> implements FormInserter<T> {

		private final MultiValueMap<String, T> data = new LinkedMultiValueMap<>();

		private final ResolvableType type;

		private final MediaType mediaType;


		private DefaultFormInserter(ResolvableType type, MediaType mediaType) {
			this.type = type;
			this.mediaType = mediaType;
		}

		public static FormInserter<String> forFormData() {
			return new DefaultFormInserter<>(FORM_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		}

		public static <T> FormInserter<T> forMultipartData() {
			return new DefaultFormInserter<>(MULTIPART_VALUE_TYPE, MediaType.MULTIPART_FORM_DATA);
		}

		@Override
		public FormInserter<T> with(String key, @Nullable T value) {
			this.data.add(key, value);
			return this;
		}

		@Override
		public FormInserter<T> with(MultiValueMap<String, T> values) {
			this.data.addAll(values);
			return this;
		}

		@Override
		public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
			HttpMessageWriter<MultiValueMap<String, T>> messageWriter =
					findMessageWriter(context, this.type, this.mediaType);
			return messageWriter.write(Mono.just(this.data), this.type, this.mediaType,
					outputMessage, context.hints());
		}
	}
}
