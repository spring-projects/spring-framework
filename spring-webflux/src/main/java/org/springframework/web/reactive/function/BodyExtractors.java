/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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
	 * Variant of {@link BodyExtractors#toMono(InputStreamMapper, boolean, int)} with a
	 * default buffer size and fast failure.
	 * @param streamMapper the mapper that is reading the body
	 * @return {@code BodyExtractor} that reads the response body as input stream
	 * @param <T> the type of the value that is resolved from the returned stream
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(
			InputStreamMapper<T> streamMapper) {
		return toMono(streamMapper, true);
	}

	/**
	 * Variant of {@link BodyExtractors#toMono(InputStreamMapper, boolean, int)} with a
	 * default buffer size.
	 * @param streamMapper the mapper that is reading the body
	 * @param failFast {@code false} if previously read bytes are discarded upon an error
	 * @return {@code BodyExtractor} that reads the response body as input stream
	 * @param <T> the type of the value that is resolved from the returned stream
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(
			InputStreamMapper<T> streamMapper,
			boolean failFast) {
		return toMono(streamMapper, failFast, 256 * 1024, true);
	}

	/**
	 * Extractor where the response body is processed by reading an input stream of the
	 * response body.
	 * @param streamMapper the mapper that is reading the body
	 * @param failFast {@code false} if previously read bytes are discarded upon an error
	 * @param maximumMemorySize the amount of memory that is buffered until reading is suspended
	 * @return {@code BodyExtractor} that reads the response body as input stream
	 * @param <T> the type of the value that is resolved from the returned stream
	 */
	public static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(
			InputStreamMapper<T> streamMapper,
			boolean failFast,
			int maximumMemorySize) {
		return toMono(streamMapper, failFast, maximumMemorySize, true);
	}

	static BodyExtractor<Mono<InputStream>, ReactiveHttpInputMessage> toInputStream() {
		return toMono(stream -> stream, true, 256 * 1024, false);
	}

	private static <T> BodyExtractor<Mono<T>, ReactiveHttpInputMessage> toMono(
			InputStreamMapper<T> streamMapper,
			boolean failFast,
			int maximumMemorySize,
			boolean close) {

		Assert.notNull(streamMapper, "'streamMapper' must not be null");
		Assert.isTrue(maximumMemorySize > 0, "'maximumMemorySize' must be positive");
		return (inputMessage, context) -> {
			FlowBufferInputStream inputStream = new FlowBufferInputStream(maximumMemorySize, failFast);
			try {
				inputMessage.getBody().subscribe(inputStream);
				T value = streamMapper.apply(inputStream);
				if (close) {
					inputStream.close();
				}
				return Mono.just(value);
			} catch (Throwable t) {
				try {
					inputStream.close();
				} catch (Throwable suppressed) {
					t.addSuppressed(suppressed);
				}
				return Mono.error(t);
			}
		};
	}

	/**
	 * Variant of {@link BodyExtractors#toMono(InputStreamMapper, boolean, int, boolean)} with a
	 * default buffer size.
	 * @param streamSupplier the supplier of the output stream
	 * @return {@code BodyExtractor} that reads the response body as input stream
	 */
	public static BodyExtractor<Mono<Void>, ReactiveHttpInputMessage> toMono(
			Supplier<? extends OutputStream> streamSupplier) {

		Assert.notNull(streamSupplier, "'streamSupplier' must not be null");
		return (inputMessage, context) -> {
			try (OutputStream outputStream = streamSupplier.get()) {
				Flux<DataBuffer> writeResult = DataBufferUtils.write(inputMessage.getBody(), outputStream);
				writeResult.blockLast();
				return Mono.empty();
			} catch (Throwable t) {
				return Mono.error(t);
			}
		};
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
	 * <p><strong>Note:</strong> that resources used for part handling,
	 * like storage for the uploaded files, is not deleted automatically, but
	 * should be done via {@link Part#delete()}.
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
	 * <p><strong>Note:</strong> that resources used for part handling,
	 * like storage for the uploaded files, is not deleted automatically, but
	 * should be done via {@link Part#delete()}.
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
							.flatMap(reader -> reader.getReadableMediaTypes(elementType).stream())
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

	private static Flux<DataBuffer> consumeAndCancel(ReactiveHttpInputMessage message) {
		return message.getBody().takeWhile(buffer -> {
			DataBufferUtils.release(buffer);
			return false;
		});
	}

	@FunctionalInterface
	public interface InputStreamMapper<T> {

		T apply(InputStream stream) throws IOException;
	}

	static class FlowBufferInputStream extends InputStream implements Subscriber<DataBuffer> {

		private static final Object END = new Object();

		private final AtomicBoolean closed = new AtomicBoolean();

		private final BlockingQueue<Object> backlog;

		private final int maximumMemorySize;

		private final boolean failFast;

		private final AtomicInteger buffered = new AtomicInteger();

		@Nullable
		private InputStreamWithSize current = new InputStreamWithSize(0, InputStream.nullInputStream());

		@Nullable
		private Subscription subscription;

		FlowBufferInputStream(int maximumMemorySize, boolean failFast) {
			this.backlog = new LinkedBlockingDeque<>();
			this.maximumMemorySize = maximumMemorySize;
			this.failFast = failFast;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			if (this.closed.get()) {
				subscription.cancel();
			} else {
				subscription.request(1);
			}
		}

		@Override
		public void onNext(DataBuffer buffer) {
			if (this.closed.get()) {
				DataBufferUtils.release(buffer);
				return;
			}
			int readableByteCount = buffer.readableByteCount();
			int current = this.buffered.addAndGet(readableByteCount);
			if (current < this.maximumMemorySize) {
				this.subscription.request(1);
			}
			InputStream stream = buffer.asInputStream(true);
			this.backlog.add(new InputStreamWithSize(readableByteCount, stream));
			if (this.closed.get()) {
				DataBufferUtils.release(buffer);
			}
		}

		@Override
		public void onError(Throwable throwable) {
			if (failFast) {
				Object next;
				while ((next = this.backlog.poll()) != null) {
					if (next instanceof InputStreamWithSize) {
						try {
							((InputStreamWithSize) next).inputStream.close();
						} catch (Throwable t) {
							throwable.addSuppressed(t);
						}
					}
				}
			}
			this.backlog.add(throwable);
		}

		@Override
		public void onComplete() {
			this.backlog.add(END);
		}

		private boolean forward() throws IOException {
			this.current.inputStream.close();
			try {
				Object next = this.backlog.take();
				if (next == END) {
					this.current = null;
					return true;
				} else if (next instanceof RuntimeException) {
					close();
					throw (RuntimeException) next;
				} else if (next instanceof IOException) {
					close();
					throw (IOException) next;
				} else if (next instanceof Throwable) {
					close();
					throw new IllegalStateException((Throwable) next);
				} else {
					int buffer = buffered.addAndGet(-this.current.size);
					if (buffer < this.maximumMemorySize) {
						this.subscription.request(1);
					}
					this.current = (InputStreamWithSize) next;
					return false;
				}
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public int read() throws IOException {
			if (this.closed.get()) {
				throw new IOException("closed");
			} else if (this.current == null) {
				return -1;
			}
			int read;
			while ((read = this.current.inputStream.read()) == -1) {
				if (forward()) {
					return -1;
				}
			}
			return read;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			Objects.checkFromIndexSize(off, len, b.length);
			if (this.closed.get()) {
				throw new IOException("closed");
			} else if (this.current == null) {
				return -1;
			}
			int sum = 0;
			do {
				int read = this.current.inputStream.read(b, off + sum, len - sum);
				if (read == -1) {
					if (this.backlog.isEmpty()) {
						return sum;
					} else if (forward()) {
						return sum == 0 ? -1 : sum;
					}
				} else {
					sum += read;
				}
			} while (sum < len);
			return sum;
		}

		@Override
		public int available() throws IOException {
			if (this.closed.get()) {
				throw new IOException("closed");
			} else if (this.current == null) {
				return 0;
			}
			int available = this.current.inputStream.available();
			for (Object value : this.backlog) {
				if (value instanceof InputStreamWithSize) {
					available += ((InputStreamWithSize) value).inputStream.available();
				} else {
					break;
				}
			}
			return available;
		}

		@Override
		public void close() throws IOException {
			if (this.closed.compareAndSet(false, true)) {
				if (this.subscription != null) {
					this.subscription.cancel();
				}
				IOException exception = null;
				if (this.current != null) {
					try {
						this.current.inputStream.close();
					} catch (IOException e) {
						exception = e;
					}
				}
				for (Object value : this.backlog) {
					if (value instanceof InputStreamWithSize) {
						try {
							((InputStreamWithSize) value).inputStream.close();
						} catch (IOException e) {
							if (exception == null) {
								exception = e;
							} else {
								exception.addSuppressed(e);
							}
						}
					}
				}
				if (exception != null) {
					throw exception;
				}
			}
		}
	}

	static class InputStreamWithSize {

		final int size;

		final InputStream inputStream;

		InputStreamWithSize(int size, InputStream inputStream) {
			this.size = size;
			this.inputStream = inputStream;
		}
	}

}
