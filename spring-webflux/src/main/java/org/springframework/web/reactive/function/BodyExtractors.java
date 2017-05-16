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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Implementations of {@link BodyExtractor} that read various bodies, such a reactive streams.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class BodyExtractors {

	private static final ResolvableType FORM_MAP_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	private static final ResolvableType MULTIPART_MAP_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Part.class);

	private static final ResolvableType PART_TYPE = ResolvableType.forClass(Part.class);


	/**
	 * Return a {@code BodyExtractor} that reads into a Reactor {@link Mono}.
	 * @param elementClass the class of element in the {@code Mono}
	 * @param <T> the element type
	 * @return a {@code BodyExtractor} that reads a mono
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(Class<? extends T> elementClass) {
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return toMono(ResolvableType.forClass(elementClass));
	}

	/**
	 * Return a {@code BodyExtractor} that reads into a Reactor {@link Mono}.
	 * @param elementType the type of element in the {@code Mono}
	 * @param <T> the element type
	 * @return a {@code BodyExtractor} that reads a mono
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(ResolvableType elementType) {
		Assert.notNull(elementType, "'elementType' must not be null");
		return (inputMessage, context) -> readWithMessageReaders(inputMessage, context,
				elementType,
				reader -> {
					Optional<ServerHttpResponse> serverResponse = context.serverResponse();
					if (serverResponse.isPresent() && inputMessage instanceof ServerHttpRequest) {
						return reader.readMono(elementType, elementType, (ServerHttpRequest) inputMessage,
								serverResponse.get(), context.hints());
					}
					else {
						return reader.readMono(elementType, inputMessage, context.hints());
					}
				},
				Mono::error);
	}

	/**
	 * Return a {@code BodyExtractor} that reads into a Reactor {@link Flux}.
	 * @param elementClass the class of element in the {@code Flux}
	 * @param <T> the element type
	 * @return a {@code BodyExtractor} that reads a mono
	 */
	public static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(Class<? extends T> elementClass) {
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return toFlux(ResolvableType.forClass(elementClass));
	}

	/**
	 * Return a {@code BodyExtractor} that reads into a Reactor {@link Flux}.
	 * @param elementType the type of element in the {@code Flux}
	 * @param <T> the element type
	 * @return a {@code BodyExtractor} that reads a mono
	 */
	public static <T> BodyExtractor<Flux<T>, ReactiveHttpInputMessage> toFlux(ResolvableType elementType) {
		Assert.notNull(elementType, "'elementType' must not be null");
		return (inputMessage, context) -> readWithMessageReaders(inputMessage, context,
				elementType,
				reader -> {
					Optional<ServerHttpResponse> serverResponse = context.serverResponse();
					if (serverResponse.isPresent() && inputMessage instanceof ServerHttpRequest) {
						return reader.read(elementType, elementType, (ServerHttpRequest) inputMessage,
								serverResponse.get(), context.hints());
					}
					else {
						return reader.read(elementType, inputMessage, context.hints());
					}
				},
				Flux::error);
	}

	/**
	 * Return a {@code BodyExtractor} that reads form data into a {@link MultiValueMap}.
	 * @return a {@code BodyExtractor} that reads form data
	 */
	// Note that the returned BodyExtractor is parameterized to ServerHttpRequest, not
	// ReactiveHttpInputMessage like other methods, since reading form data only typically happens on
	// the server-side
	public static BodyExtractor<Mono<MultiValueMap<String, String>>, ServerHttpRequest> toFormData() {
		return (serverRequest, context) -> {
			HttpMessageReader<MultiValueMap<String, String>> messageReader =
					messageReader(FORM_MAP_TYPE, MediaType.APPLICATION_FORM_URLENCODED, context);
			return context.serverResponse()
					.map(serverResponse -> messageReader.readMono(FORM_MAP_TYPE, FORM_MAP_TYPE, serverRequest, serverResponse, context.hints()))
					.orElseGet(() -> messageReader.readMono(FORM_MAP_TYPE, serverRequest, context.hints()));
		};
	}

	/**
	 * Return a {@code BodyExtractor} that reads multipart (i.e. file upload) form data into a
	 * {@link MultiValueMap}.
	 * @return a {@code BodyExtractor} that reads multipart data
	 */
	// Note that the returned BodyExtractor is parameterized to ServerHttpRequest, not
	// ReactiveHttpInputMessage like other methods, since reading form data only typically happens on
	// the server-side
	public static BodyExtractor<Mono<MultiValueMap<String, Part>>, ServerHttpRequest> toMultipartData() {
		return (serverRequest, context) -> {
			HttpMessageReader<MultiValueMap<String, Part>> messageReader =
					messageReader(MULTIPART_MAP_TYPE, MediaType.MULTIPART_FORM_DATA, context);
			return context.serverResponse()
					.map(serverResponse -> messageReader.readMono(MULTIPART_MAP_TYPE,
							MULTIPART_MAP_TYPE, serverRequest, serverResponse, context.hints()))
					.orElseGet(() -> messageReader.readMono(MULTIPART_MAP_TYPE, serverRequest, context.hints()));
		};
	}

	/**
	 * Return a {@code BodyExtractor} that reads multipart (i.e. file upload) form data into a
	 * {@link MultiValueMap}.
	 * @return a {@code BodyExtractor} that reads multipart data
	 */
	// Note that the returned BodyExtractor is parameterized to ServerHttpRequest, not
	// ReactiveHttpInputMessage like other methods, since reading form data only typically happens on
	// the server-side
	public static BodyExtractor<Flux<Part>, ServerHttpRequest> toParts() {
		return (serverRequest, context) -> {
			HttpMessageReader<Part> messageReader =
					messageReader(PART_TYPE, MediaType.MULTIPART_FORM_DATA, context);
			return context.serverResponse()
					.map(serverResponse -> messageReader.read(PART_TYPE, PART_TYPE, serverRequest, serverResponse, context.hints()))
					.orElseGet(() -> messageReader.read(PART_TYPE, serverRequest, context.hints()));
		};
	}

	/**
	 * Return a {@code BodyExtractor} that returns the body of the message as a {@link Flux} of
	 * {@link DataBuffer}s.
	 * <p><strong>Note</strong> that the returned buffers should be released after usage by calling
	 * {@link org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer)}
	 * @return a {@code BodyExtractor} that returns the body
	 * @see ReactiveHttpInputMessage#getBody()
	 */
	public static BodyExtractor<Flux<DataBuffer>, ReactiveHttpInputMessage> toDataBuffers() {
		return (inputMessage, context) -> inputMessage.getBody();
	}


	private static <T, S extends Publisher<T>> S readWithMessageReaders(
			ReactiveHttpInputMessage inputMessage, BodyExtractor.Context context, ResolvableType elementType,
			Function<HttpMessageReader<T>, S> readerFunction, Function<Throwable, S> unsupportedError) {

		MediaType contentType = contentType(inputMessage);
		Supplier<Stream<HttpMessageReader<?>>> messageReaders = context.messageReaders();
		return messageReaders.get()
				.filter(r -> r.canRead(elementType, contentType))
				.findFirst()
				.map(BodyExtractors::<T>cast)
				.map(readerFunction)
				.orElseGet(() -> {
					List<MediaType> supportedMediaTypes = messageReaders.get()
							.flatMap(reader -> reader.getReadableMediaTypes().stream())
							.collect(Collectors.toList());
					UnsupportedMediaTypeException error =
							new UnsupportedMediaTypeException(contentType, supportedMediaTypes);
					return unsupportedError.apply(error);
				});
	}

	private static <T> HttpMessageReader<T> messageReader(ResolvableType elementType,
			MediaType mediaType, BodyExtractor.Context context) {
		return context.messageReaders().get()
				.filter(messageReader -> messageReader.canRead(elementType, mediaType))
				.findFirst()
				.map(BodyExtractors::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"Could not find HttpMessageReader that supports \"" + mediaType +
								"\" and \"" + elementType + "\""));
	}

	private static MediaType contentType(HttpMessage message) {
		MediaType result = message.getHeaders().getContentType();
		return result != null ? result : MediaType.APPLICATION_OCTET_STREAM;
	}

	@SuppressWarnings("unchecked")
	private static <T> HttpMessageReader<T> cast(HttpMessageReader<?> messageReader) {
		return (HttpMessageReader<T>) messageReader;
	}

}
