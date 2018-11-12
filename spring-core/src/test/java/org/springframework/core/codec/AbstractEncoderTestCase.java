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

package org.springframework.core.codec;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.LeakAwareDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

/**
 * Abstract base class for {@link Encoder} unit tests. Subclasses need to implement
 * {@link #input()} and {@link #outputConsumers()}, from which {@link #encode()},
 * {@link #encodeError()} and {@link #encodeCancel()} are run.
 *
 * @author Arjen Poutsma
 */
@SuppressWarnings("ProtectedField")
public abstract class AbstractEncoderTestCase<T, E extends Encoder<T>> {

	/**
	 * The data buffer factory used by the encoder.
	 */
	protected final DataBufferFactory bufferFactory =
			new LeakAwareDataBufferFactory();

	/**
	 * The encoder to test.
	 */
	protected final E encoder;

	/**
	 * The type used for
	 * {@link Encoder#encode(Publisher, DataBufferFactory, ResolvableType, MimeType, Map)}.
	 */
	protected final ResolvableType elementType;

	/**
	 * The mime type used for
	 * {@link Encoder#encode(Publisher, DataBufferFactory, ResolvableType, MimeType, Map)}.
	 * May be {@code null}.
	 */
	@Nullable
	protected final MimeType mimeType;

	/**
	 * The hints used for
	 * {@link Encoder#encode(Publisher, DataBufferFactory, ResolvableType, MimeType, Map)}.
	 * May be {@code null}.
	 */
	@Nullable
	protected final Map<String, Object> hints;


	/**
	 * Construct a new {@code AbstractEncoderTestCase} for the given encoder and element class.
	 * @param encoder the encoder
	 * @param elementClass the element class
	 */
	protected AbstractEncoderTestCase(E encoder, Class<?> elementClass) {
		this(encoder, ResolvableType.forClass(elementClass), null, null);
	}

	/**
	 * Construct a new {@code AbstractEncoderTestCase} for the given parameters.
	 * @param encoder the encoder
	 * @param elementType the element type
	 * @param mimeType the mime type. May be {@code null}.
	 * @param hints the hints. May be {@code null}.
	 */
	protected AbstractEncoderTestCase(E encoder, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Assert.notNull(encoder, "Encoder must not be null");
		Assert.notNull(elementType, "ElementType must not be null");

		this.encoder = encoder;
		this.elementType = elementType;
		this.mimeType = mimeType;
		this.hints = hints;
	}

	/**
	 * Checks whether any of the data buffers created by {@link #bufferFactory} have not been
	 * released, throwing an assertion error if so.
	 */
	@After
	public final void checkForLeaks() {
		((LeakAwareDataBufferFactory) this.bufferFactory).checkForLeaks();
	}

	/**
	 * Abstract template method that provides input for the encoder.
	 * Used for {@link #encode()}, {@link #encodeError()}, and {@link #encodeCancel()}.
	 */
	protected abstract Flux<T> input();

	/**
	 * Abstract template method that verifies the output of the encoder.
	 * The returned stream should contain a buffer consumer for each expected output, given
	 * the {@linkplain #input()}.
	 */
	protected abstract Stream<Consumer<DataBuffer>> outputConsumers();

	private Stream<Consumer<DataBuffer>> outputAndReleaseConsumers() {
		return outputConsumers()
				.map(consumer -> consumer.andThen(DataBufferUtils::release));
	}

	/**
	 * Create a result consumer that expects the given String in UTF-8 encoding.
	 * @param expected the expected string
	 * @return a consumer that expects the given data buffer to be equal to {@code expected}
	 */
	protected final Consumer<DataBuffer> resultConsumer(String expected) {
		return dataBuffer -> {
			byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(resultBytes);
			String actual = new String(resultBytes, UTF_8);
			assertEquals(expected, actual);
		};

	}

	/**
	 * Create a result consumer that expects the given bytes.
	 * @param expected the expected string
	 * @return a consumer that expects the given data buffer to be equal to {@code expected}
	 */
	protected final Consumer<DataBuffer> resultConsumer(byte[] expected) {
		return dataBuffer -> {
			byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(resultBytes);
			assertArrayEquals(expected, resultBytes);
		};
	}

	/**
	 * Tests whether passing {@link #input()} to the encoder can be consumed with
	 * {@link #outputConsumers()}.
	 */
	@Test
	public final void encode() {
		Flux<T> input = input();

		Flux<DataBuffer> output = this.encoder.encode(input, this.bufferFactory,
				this.elementType, this.mimeType, this.hints);

		StepVerifier.Step<DataBuffer> step = StepVerifier.create(output);

		outputAndReleaseConsumers().forEach(step::consumeNextWith);

		step.expectComplete()
				.verify();
	}

	/**
	 * Tests whether passing an error to the encoder can be consumed with
	 * {@link #outputConsumers()}.
	 */
	@Test
	public final void encodeError() {

		boolean singleValue = this.encoder instanceof AbstractSingleValueEncoder;

		Flux<T> input;
		if (singleValue) {
			input = Flux.error(new RuntimeException());
		}
		else {
			input = Flux.concat(
					input().take(1),
					Flux.error(new RuntimeException()));
		}

		Flux<DataBuffer> output = this.encoder.encode(input, this.bufferFactory,
				this.elementType, this.mimeType, this.hints);

		if (singleValue) {
			StepVerifier.create(output)
					.expectError(RuntimeException.class)
					.verify();
		}
		else {
			Consumer<DataBuffer> firstResultConsumer = outputAndReleaseConsumers().findFirst()
					.orElseThrow(IllegalArgumentException::new);
			StepVerifier.create(output)
					.consumeNextWith(firstResultConsumer)
					.expectError(RuntimeException.class)
					.verify();
		}
	}

	/**
	 * Tests whether canceling the output of the encoder can be consumed with
	 * {@link #outputConsumers()}.
	 */
	@Test
	public final void encodeCancel() {
		Flux<T> input = input();

		Flux<DataBuffer> output = this.encoder.encode(input, this.bufferFactory,
				this.elementType, this.mimeType, this.hints);

		Consumer<DataBuffer> firstResultConsumer = outputAndReleaseConsumers().findFirst()
				.orElseThrow(IllegalArgumentException::new);
		StepVerifier.create(output)
				.consumeNextWith(firstResultConsumer)
				.thenCancel()
				.verify();
	}

}
