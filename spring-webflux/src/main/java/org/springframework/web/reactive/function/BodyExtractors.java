/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;

/**
 * Static factory methods for {@link BodyExtractor} implementations.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class BodyExtractors {

	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MULTIPART_DATA_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Part.class);

	private static final ResolvableType PART_TYPE = ResolvableType.forClass(Part.class);

	private static final ResolvableType VOID_TYPE = ResolvableType.forClass(Void.class);


	/**
	 * Extractor to decode the input content into {@code Mono<T>}.
	 * @param elementClass the class of the element type to decode to
	 * @param <T> the element type to decode to
	 * @return {@code BodyExtractor} for {@code Mono<T>}
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(Class<? extends T> elementClass) {
		return toMono(ResolvableType.forClass(elementClass));
	}

	/**
	 * Variant of {@link #toMono(Class)} for type information with generics.
	 * @param elementTypeRef the type reference for the type to decode to
	 * @param <T> the element type to decode to
	 * @return {@code BodyExtractor} for {@code Mono<T>}
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(ParameterizedTypeReference<T> elementTypeRef) {
		return toMono(ResolvableType.forType(elementTypeRef.getType()));
	}

	private static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(ResolvableType elementType) {
		return (inputMessage, context) ->
				readWithMessageReaders(inputMessage, context, elementType,
						(HttpMessageReader<T> reader) -> readToMono(inputMessage, context, elementType, reader),
						ex -> Mono.from(unsupportedErrorHandler(inputMessage, ex)),
						skipBodyAsMono(inputMessage));
	}

	/**
	 * Extractor to decode the input content into {@code Flux<T>}.
	 * @param elementClass the class of the element type to decode to
	 * @param <T> the element type to decode to
	 * @return {@code BodyExtractor} for {@code Flux<T>}
	 */
	public static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(Class<? extends T> elementClass) {
		return toFlux(ResolvableType.forClass(elementClass));
	}

	/**
	 * Variant of {@link #toFlux(Class)} for type information with generics.
	 * @param typeRef the type reference for the type to decode to
	 * @param <T> the element type to decode to
	 * @return {@code BodyExtractor} for {@code Flux<T>}
	 */
	public static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(ParameterizedTypeReference<T> typeRef) {
		return toFlux(ResolvableType.forType(typeRef.getType()));
	}

	@SuppressWarnings("unchecked")
	private static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(ResolvableType elementType) {
		return (inputMessage, context) ->
				readWithMessageReaders(inputMessage, context, elementType,
						(HttpMessageReader<T> reader) -> readToFlux(inputMessage, context, elementType, reader),
						ex -> unsupportedErrorHandler(inputMessage, ex),
						skipBodyAsFlux(inputMessage));
	}


	// Extractors for specific content ..

	/**
	 * Extractor to read form data into {@code MultiValueMap<String, String>}.
	 * <p>As of 5.1 this method can also be used on the client side to read form
	 * data from a server response (e.g. OAuth).
	 * @return {@code BodyExtractor} for form data
	 */
	public static BodyExtractor<Mono<MultiValueMap<String, String>>, ReactiveHttpInputMessage> toFormData() {
		return (message, context) -> {
			ResolvableType elementType = FORM_DATA_TYPE;
			MediaType mediaType = MediaType.APPLICATION_FORM_URLENCODED;
			HttpMessageReader<MultiValueMap<String, String>> reader = findReader(elementType, mediaType, context);
			return readToMono(message, context, elementType, reader);
		};
	}

	/**
	 * Extractor to read multipart data into a {@code MultiValueMap<String, Part>}.
	 * @return {@code BodyExtractor} for multipart data
	 */
	// Parameterized for server-side use
	public static BodyExtractor<Mono<MultiValueMap<String, Part>>, ServerHttpRequest> toMultipartData() {
		return (serverRequest, context) -> {
			ResolvableType elementType = MULTIPART_DATA_TYPE;
			MediaType mediaType = MediaType.MULTIPART_FORM_DATA;
			HttpMessageReader<MultiValueMap<String, Part>> reader = findReader(elementType, mediaType, context);
			return readToMono(serverRequest, context, elementType, reader);
		};
	}

	/**
	 * Extractor to read multipart data into {@code Flux<Part>}.
	 * @return {@code BodyExtractor} for multipart request parts
	 */
	// Parameterized for server-side use
	public static BodyExtractor<Flux<Part>, ServerHttpRequest> toParts() {
		return (serverRequest, context) -> {
			ResolvableType elementType = PART_TYPE;
			MediaType mediaType = MediaType.MULTIPART_FORM_DATA;
			HttpMessageReader<Part> reader = findReader(elementType, mediaType, context);
			return readToFlux(serverRequest, context, elementType, reader);
		};
	}

	/**
	 * Extractor that returns the raw {@link DataBuffer DataBuffers}.
	 * <p><strong>Note:</strong> the data buffers should be
	 * {@link org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)
	 * released} after being used.
	 * @return {@code BodyExtractor} for data buffers
	 */
	public static BodyExtractor<Flux<DataBuffer>, ReactiveHttpInputMessage> toDataBuffers() {
		return (inputMessage, context) -> inputMessage.getBody();
	}


	// Private support methods

	private static <T, S extends Publisher<T>> S readWithMessageReaders(
			ReactiveHttpInputMessage message, BodyExtractor.Context context, ResolvableType elementType,
			Function<HttpMessageReader<T>, S> readerFunction,
			Function<UnsupportedMediaTypeException, S> errorFunction,
			Supplier<S> emptySupplier) {

		if (VOID_TYPE.equals(elementType)) {
			return emptySupplier.get();
		}
		MediaType contentType = Optional.ofNullable(message.getHeaders().getContentType())
				.orElse(MediaType.APPLICATION_OCTET_STREAM);

		return context.messageReaders().stream()
				.filter(reader -> reader.canRead(elementType, contentType))
				.findFirst()
				.map(BodyExtractors::<T>cast)
				.map(readerFunction)
				.orElseGet(() -> {
					List<MediaType> mediaTypes = context.messageReaders().stream()
							.flatMap(reader -> reader.getReadableMediaTypes().stream())
							.collect(Collectors.toList());
					return errorFunction.apply(
							new UnsupportedMediaTypeException(contentType, mediaTypes, elementType));
				});
	}

	private static <T> Mono<T> readToMono(ReactiveHttpInputMessage message, BodyExtractor.Context context,
			ResolvableType type, HttpMessageReader<T> reader) {

		return context.serverResponse()
				.map(response -> reader.readMono(type, type, (ServerHttpRequest) message, response, context.hints()))
				.orElseGet(() -> reader.readMono(type, message, context.hints()));
	}

	private static <T> Flux<T> readToFlux(ReactiveHttpInputMessage message, BodyExtractor.Context context,
			ResolvableType type, HttpMessageReader<T> reader) {

		return context.serverResponse()
				.map(response -> reader.read(type, type, (ServerHttpRequest) message, response, context.hints()))
				.orElseGet(() -> reader.read(type, message, context.hints()));
	}

	private static <T> Flux<T> unsupportedErrorHandler(
			ReactiveHttpInputMessage message, UnsupportedMediaTypeException ex) {

		Flux<T> result;
		if (message.getHeaders().getContentType() == null) {
			// Maybe it's okay there is no content type, if there is no content..
			result = message.getBody().map(buffer -> {
				DataBufferUtils.release(buffer);
				throw ex;
			});
		}
		else {
			result = message instanceof ClientHttpResponse ?
					consumeAndCancel(message).thenMany(Flux.error(ex)) : Flux.error(ex);
		}
		return result;
	}

	private static <T> HttpMessageReader<T> findReader(
			ResolvableType elementType, MediaType mediaType, BodyExtractor.Context context) {

		return context.messageReaders().stream()
				.filter(messageReader -> messageReader.canRead(elementType, mediaType))
				.findFirst()
				.map(BodyExtractors::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"No HttpMessageReader for \"" + mediaType + "\" and \"" + elementType + "\""));
	}

	@SuppressWarnings("unchecked")
	private static <T> HttpMessageReader<T> cast(HttpMessageReader<?> reader) {
		return (HttpMessageReader<T>) reader;
	}

	private static <T> Supplier<Flux<T>> skipBodyAsFlux(ReactiveHttpInputMessage message) {
		return message instanceof ClientHttpResponse ?
				() -> consumeAndCancel(message).thenMany(Mono.empty()) : Flux::empty;
	}

	@SuppressWarnings("unchecked")
	private static <T> Supplier<Mono<T>> skipBodyAsMono(ReactiveHttpInputMessage message) {
		return message instanceof ClientHttpResponse ?
				() -> consumeAndCancel(message).then(Mono.empty()) : Mono::empty;
	}

	private static Mono<Void> consumeAndCancel(ReactiveHttpInputMessage message) {
		return message.getBody()
				.map(buffer -> {
					DataBufferUtils.release(buffer);
					throw new ReadCancellationException();
				})
				.onErrorResume(ReadCancellationException.class, ex -> Mono.empty())
				.then();
	}

	@SuppressWarnings("serial")
	private static class ReadCancellationException extends RuntimeException {
	}

}
