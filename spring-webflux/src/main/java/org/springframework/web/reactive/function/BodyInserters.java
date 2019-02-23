/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Static factory methods for {@link BodyInserter} implementations.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class BodyInserters {

	private static final ResolvableType RESOURCE_TYPE = ResolvableType.forClass(Resource.class);

	private static final ResolvableType SSE_TYPE = ResolvableType.forClass(ServerSentEvent.class);

	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MULTIPART_DATA_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Object.class);

	private static final BodyInserter<Void, ReactiveHttpOutputMessage> EMPTY_INSERTER =
			(response, context) -> response.setComplete();


	/**
	 * Inserter that does not write.
	 * @return the inserter
	 */
	@SuppressWarnings("unchecked")
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> empty() {
		return (BodyInserter<T, ReactiveHttpOutputMessage>) EMPTY_INSERTER;
	}

	/**
	 * Inserter to write the given object.
	 * <p>Alternatively, consider using the {@code syncBody(Object)} shortcuts on
	 * {@link org.springframework.web.reactive.function.client.WebClient WebClient} and
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse}.
	 * @param body the body to write to the response
	 * @param <T> the type of the body
	 * @return the inserter to write a single object
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromObject(T body) {
		return (message, context) ->
				writeWithMessageWriters(message, context, Mono.just(body), ResolvableType.forInstance(body));
	}

	/**
	 * Inserter to write the given {@link Publisher}.
	 * <p>Alternatively, consider using the {@code body} shortcuts on
	 * {@link org.springframework.web.reactive.function.client.WebClient WebClient} and
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse}.
	 * @param publisher the publisher to write with
	 * @param elementClass the type of elements in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the {@code Publisher} type
	 * @return the inserter to write a {@code Publisher}
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, Class<T> elementClass) {

		Assert.notNull(publisher, "Publisher must not be null");
		Assert.notNull(elementClass, "Element Class must not be null");
		return (message, context) ->
				writeWithMessageWriters(message, context, publisher, ResolvableType.forClass(elementClass));
	}

	/**
	 * Inserter to write the given {@link Publisher}.
	 * <p>Alternatively, consider using the {@code body} shortcuts on
	 * {@link org.springframework.web.reactive.function.client.WebClient WebClient} and
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse}.
	 * @param publisher the publisher to write with
	 * @param typeReference the type of elements contained in the publisher
	 * @param <T> the type of the elements contained in the publisher
	 * @param <P> the {@code Publisher} type
	 * @return the inserter to write a {@code Publisher}
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, ParameterizedTypeReference<T> typeReference) {

		Assert.notNull(publisher, "Publisher must not be null");
		Assert.notNull(typeReference, "ParameterizedTypeReference must not be null");
		return (message, context) ->
				writeWithMessageWriters(message, context, publisher, ResolvableType.forType(typeReference.getType()));
	}

	/**
	 * Inserter to write the given {@code Resource}.
	 * <p>If the resource can be resolved to a {@linkplain Resource#getFile() file}, it will
	 * be copied using <a href="https://en.wikipedia.org/wiki/Zero-copy">zero-copy</a>.
	 * @param resource the resource to write to the output message
	 * @param <T> the type of the {@code Resource}
	 * @return the inserter to write a {@code Publisher}
	 */
	public static <T extends Resource> BodyInserter<T, ReactiveHttpOutputMessage> fromResource(T resource) {
		Assert.notNull(resource, "Resource must not be null");
		return (outputMessage, context) -> {
			ResolvableType elementType = RESOURCE_TYPE;
			HttpMessageWriter<Resource> writer = findWriter(context, elementType, null);
			return write(Mono.just(resource), elementType, null, outputMessage, context, writer);
		};
	}

	/**
	 * Inserter to write the given {@code ServerSentEvent} publisher.
	 * <p>Alternatively, you can provide event data objects via
	 * {@link #fromPublisher(Publisher, Class)}, and set the "Content-Type" to
	 * {@link MediaType#TEXT_EVENT_STREAM text/event-stream}.
	 * @param eventsPublisher the {@code ServerSentEvent} publisher to write to the response body
	 * @param <T> the type of the data elements in the {@link ServerSentEvent}
	 * @return the inserter to write a {@code ServerSentEvent} publisher
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	// Parameterized for server-side use
	public static <T, S extends Publisher<ServerSentEvent<T>>> BodyInserter<S, ServerHttpResponse> fromServerSentEvents(
			S eventsPublisher) {

		Assert.notNull(eventsPublisher, "Publisher must not be null");
		return (serverResponse, context) -> {
			ResolvableType elementType = SSE_TYPE;
			MediaType mediaType = MediaType.TEXT_EVENT_STREAM;
			HttpMessageWriter<ServerSentEvent<T>> writer = findWriter(context, elementType, mediaType);
			return write(eventsPublisher, elementType, mediaType, serverResponse, context, writer);
		};
	}

	/**
	 * Return a {@link FormInserter} to write the given {@code MultiValueMap}
	 * as URL-encoded form data. The returned inserter allows for additional
	 * entries to be added via {@link FormInserter#with(String, Object)}.
	 * <p>Note that you can also use the {@code syncBody(Object)} method in the
	 * request builders of both the {@code WebClient} and {@code WebTestClient}.
	 * In that case the setting of the request content type is also not required,
	 * just be sure the map contains String values only or otherwise it would be
	 * interpreted as a multipart request.
	 * @param formData the form data to write to the output message
	 * @return the inserter that allows adding more form data
	 */
	public static FormInserter<String> fromFormData(MultiValueMap<String, String> formData) {
		return new DefaultFormInserter().with(formData);
	}

	/**
	 * Return a {@link FormInserter} to write the given key-value pair as
	 * URL-encoded form data. The returned inserter allows for additional
	 * entries to be added via {@link FormInserter#with(String, Object)}.
	 * @param name the key to add to the form
	 * @param value the value to add to the form
	 * @return the inserter that allows adding more form data
	 */
	public static FormInserter<String> fromFormData(String name, String value) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(value, "'value' must not be null");
		return new DefaultFormInserter().with(name, value);
	}

	/**
	 * Return a {@link MultipartInserter} to write the given
	 * {@code MultiValueMap} as multipart data. Values in the map can be an
	 * Object or an {@link HttpEntity}.
	 * <p>Note that you can also build the multipart data externally with
	 * {@link MultipartBodyBuilder}, and pass the resulting map directly to the
	 * {@code syncBody(Object)} shortcut method in {@code WebClient}.
	 * @param multipartData the form data to write to the output message
	 * @return the inserter that allows adding more parts
	 * @see MultipartBodyBuilder
	 */
	public static MultipartInserter fromMultipartData(MultiValueMap<String, ?> multipartData) {
		Assert.notNull(multipartData, "'multipartData' must not be null");
		return new DefaultMultipartInserter().withInternal(multipartData);
	}

	/**
	 * Return a {@link MultipartInserter} to write the given parts,
	 * as multipart data. Values in the map can be an Object or an
	 * {@link HttpEntity}.
	 * <p>Note that you can also build the multipart data externally with
	 * {@link MultipartBodyBuilder}, and pass the resulting map directly to the
	 * {@code syncBody(Object)} shortcut method in {@code WebClient}.
	 * @param name the part name
	 * @param value the part value, an Object or {@code HttpEntity}
	 * @return the inserter that allows adding more parts
	 */
	public static MultipartInserter fromMultipartData(String name, Object value) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(value, "'value' must not be null");
		return new DefaultMultipartInserter().with(name, value);
	}

	/**
	 * Return a {@link MultipartInserter} to write the given asynchronous parts,
	 * as multipart data.
	 * <p>Note that you can also build the multipart data externally with
	 * {@link MultipartBodyBuilder}, and pass the resulting map directly to the
	 * {@code syncBody(Object)} shortcut method in {@code WebClient}.
	 * @param name the part name
	 * @param publisher the publisher that forms the part value
	 * @param elementClass the class contained in the {@code publisher}
	 * @return the inserter that allows adding more parts
	 */
	public static <T, P extends Publisher<T>> MultipartInserter fromMultipartAsyncData(
			String name, P publisher, Class<T> elementClass) {

		return new DefaultMultipartInserter().withPublisher(name, publisher, elementClass);
	}

	/**
	 * Variant of {@link #fromMultipartAsyncData(String, Publisher, Class)} that
	 * accepts a {@link ParameterizedTypeReference} for the element type, which
	 * allows specifying generic type information.
	 * <p>Note that you can also build the multipart data externally with
	 * {@link MultipartBodyBuilder}, and pass the resulting map directly to the
	 * {@code syncBody(Object)} shortcut method in {@code WebClient}.
	 * @param name the part name
	 * @param publisher the publisher that forms the part value
	 * @param typeReference the type contained in the {@code publisher}
	 * @return the inserter that allows adding more parts
	 */
	public static <T, P extends Publisher<T>> MultipartInserter fromMultipartAsyncData(
			String name, P publisher, ParameterizedTypeReference<T> typeReference) {

		return new DefaultMultipartInserter().withPublisher(name, publisher, typeReference);
	}

	/**
	 * Inserter to write the given {@code Publisher<DataBuffer>} to the body.
	 * @param publisher the data buffer publisher to write
	 * @param <T> the type of the publisher
	 * @return the inserter to write directly to the body
	 * @see ReactiveHttpOutputMessage#writeWith(Publisher)
	 */
	public static <T extends Publisher<DataBuffer>> BodyInserter<T, ReactiveHttpOutputMessage> fromDataBuffers(
			T publisher) {

		Assert.notNull(publisher, "Publisher must not be null");
		return (outputMessage, context) -> outputMessage.writeWith(publisher);
	}


	private static <P extends Publisher<?>, M extends ReactiveHttpOutputMessage> Mono<Void> writeWithMessageWriters(
			M outputMessage, BodyInserter.Context context, P body, ResolvableType bodyType) {

		MediaType mediaType = outputMessage.getHeaders().getContentType();
		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(bodyType, mediaType))
				.findFirst()
				.map(BodyInserters::cast)
				.map(writer -> write(body, bodyType, mediaType, outputMessage, context, writer))
				.orElseGet(() -> Mono.error(unsupportedError(bodyType, context, mediaType)));
	}

	private static UnsupportedMediaTypeException unsupportedError(ResolvableType bodyType,
			BodyInserter.Context context, @Nullable MediaType mediaType) {

		List<MediaType> supportedMediaTypes = context.messageWriters().stream()
				.flatMap(reader -> reader.getWritableMediaTypes().stream())
				.collect(Collectors.toList());

		return new UnsupportedMediaTypeException(mediaType, supportedMediaTypes, bodyType);
	}

	private static <T> Mono<Void> write(Publisher<? extends T> input, ResolvableType type,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message,
			BodyInserter.Context context, HttpMessageWriter<T> writer) {

		return context.serverRequest()
				.map(request -> {
					ServerHttpResponse response = (ServerHttpResponse) message;
					return writer.write(input, type, type, mediaType, request, response, context.hints());
				})
				.orElseGet(() -> writer.write(input, type, mediaType, message, context.hints()));
	}

	private static <T> HttpMessageWriter<T> findWriter(
			BodyInserter.Context context, ResolvableType elementType, @Nullable MediaType mediaType) {

		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(elementType, mediaType))
				.findFirst()
				.map(BodyInserters::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"No HttpMessageWriter for \"" + mediaType + "\" and \"" + elementType + "\""));
	}

	@SuppressWarnings("unchecked")
	private static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}


	/**
	 * Extension of {@link BodyInserter} that allows for adding form data or
	 * multipart form data.
	 *
	 * @param <T> the value type
	 */
	public interface FormInserter<T> extends BodyInserter<MultiValueMap<String, T>, ClientHttpRequest> {

		// FormInserter is parameterized to ClientHttpRequest (for client-side use only)

		/**
		 * Adds the specified key-value pair to the form.
		 * @param key the key to be added
		 * @param value the value to be added
		 * @return this inserter for adding more parts
		 */
		FormInserter<T> with(String key, T value);

		/**
		 * Adds the specified values to the form.
		 * @param values the values to be added
		 * @return this inserter for adding more parts
		 */
		FormInserter<T> with(MultiValueMap<String, T> values);

	}


	/**
	 * Extension of {@link FormInserter} that allows for adding asynchronous parts.
	 */
	public interface MultipartInserter extends FormInserter<Object> {

		/**
		 * Add an asynchronous part with {@link Publisher}-based content.
		 * @param name the name of the part to add
		 * @param publisher the part contents
		 * @param elementClass the type of elements contained in the publisher
		 * @return this inserter for adding more parts
		 */
		<T, P extends Publisher<T>> MultipartInserter withPublisher(String name, P publisher,
				Class<T> elementClass);

		/**
		 * Variant of {@link #withPublisher(String, Publisher, Class)} that accepts a
		 * {@link ParameterizedTypeReference} for the element type, which allows
		 * specifying generic type information.
		 * @param name the key to be added
		 * @param publisher the publisher to be added as value
		 * @param typeReference the type of elements contained in {@code publisher}
		 * @return this inserter for adding more parts
		 */
		<T, P extends Publisher<T>> MultipartInserter withPublisher(String name, P publisher,
				ParameterizedTypeReference<T> typeReference);

	}


	private static class DefaultFormInserter implements FormInserter<String> {

		private final MultiValueMap<String, String> data = new LinkedMultiValueMap<>();

		@Override
		public FormInserter<String> with(String key, @Nullable String value) {
			this.data.add(key, value);
			return this;
		}

		@Override
		public FormInserter<String> with(MultiValueMap<String, String> values) {
			this.data.addAll(values);
			return this;
		}

		@Override
		public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
			HttpMessageWriter<MultiValueMap<String, String>> messageWriter =
					findWriter(context, FORM_DATA_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
			return messageWriter.write(Mono.just(this.data), FORM_DATA_TYPE,
					MediaType.APPLICATION_FORM_URLENCODED,
					outputMessage, context.hints());
		}
	}


	private static class DefaultMultipartInserter implements MultipartInserter {

		private final MultipartBodyBuilder builder = new MultipartBodyBuilder();

		@Override
		public MultipartInserter with(String key, Object value) {
			this.builder.part(key, value);
			return this;
		}

		@Override
		public MultipartInserter with(MultiValueMap<String, Object> values) {
			return withInternal(values);
		}

		@SuppressWarnings("unchecked")
		private MultipartInserter withInternal(MultiValueMap<String, ?> values) {
			values.forEach((key, valueList) -> {
				for (Object value : valueList) {
					this.builder.part(key, value);
				}
			});
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> MultipartInserter withPublisher(
				String name, P publisher, Class<T> elementClass) {

			this.builder.asyncPart(name, publisher, elementClass);
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> MultipartInserter withPublisher(
				String name, P publisher, ParameterizedTypeReference<T> typeReference) {

			this.builder.asyncPart(name, publisher, typeReference);
			return this;
		}

		@Override
		public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
			HttpMessageWriter<MultiValueMap<String, HttpEntity<?>>> messageWriter =
					findWriter(context, MULTIPART_DATA_TYPE, MediaType.MULTIPART_FORM_DATA);
			MultiValueMap<String, HttpEntity<?>> body = this.builder.build();
			return messageWriter.write(Mono.just(body), MULTIPART_DATA_TYPE,
					MediaType.MULTIPART_FORM_DATA, outputMessage, context.hints());
		}
	}

}
