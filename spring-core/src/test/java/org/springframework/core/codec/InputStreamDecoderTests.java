/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.codec;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Vladislav Kisel
 */
class InputStreamDecoderTests extends AbstractDecoderTests<InputStreamDecoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);


	InputStreamDecoderTests() {
		super(new InputStreamDecoder());
	}

	@Override
	@Test
	public void canDecode() {
		assertThat(this.decoder.canDecode(ResolvableType.forClass(InputStream.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(InputStream.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();
	}

	@Override
	@Test
	public void decode() {
		Flux<DataBuffer> input = Flux.just(
				this.bufferFactory.wrap(this.fooBytes),
				this.bufferFactory.wrap(this.barBytes));

		testDecodeAll(input, InputStream.class, step -> step
				.consumeNextWith(expectInputStream(this.fooBytes))
				.consumeNextWith(expectInputStream(this.barBytes))
				.verifyComplete());
	}

	@Override
	@Test
	public void decodeToMono() {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));

		byte[] expected = new byte[this.fooBytes.length + this.barBytes.length];
		System.arraycopy(this.fooBytes, 0, expected, 0, this.fooBytes.length);
		System.arraycopy(this.barBytes, 0, expected, this.fooBytes.length, this.barBytes.length);

		testDecodeToMonoAll(input, InputStream.class, step -> step
				.consumeNextWith(expectInputStream(expected))
				.verifyComplete());
		testDecodeToMonoErrorFailLast(input, expected);
	}

	@Override
	protected void testDecodeToMonoError(Publisher<DataBuffer> input, ResolvableType outputType,
										 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		input = Flux.from(input).concatWith(Flux.error(new InputException()));
		try (InputStream result = this.decoder.decodeToMono(input, outputType, mimeType, hints).block()) {
			assertThatThrownBy(() -> result.read()).isInstanceOf(InputException.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void testDecodeToMonoErrorFailLast(Publisher<DataBuffer> input, byte[] expected) {
		input = Flux.concatDelayError(Flux.from(input), Flux.error(new InputException()));
		try (InputStream result = this.decoder.decodeToMono(input,
				ResolvableType.forType(InputStream.class),
				null,
				Collections.singletonMap(InputStreamDecoder.FAIL_FAST, false)).block()) {
			byte[] actual = new byte[expected.length];
			assertThat(result.read(actual)).isEqualTo(expected.length);
			assertThat(actual).isEqualTo(expected);
			assertThatThrownBy(() -> result.read()).isInstanceOf(InputException.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	protected void testDecodeToMonoCancel(Publisher<DataBuffer> input, ResolvableType outputType,
										  @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) { }

	@Override
	protected void testDecodeToMonoEmpty(ResolvableType outputType, @Nullable MimeType mimeType,
										 @Nullable Map<String, Object> hints) {

		try (InputStream result = this.decoder.decodeToMono(Flux.empty(), outputType, mimeType, hints).block()) {
			assertThat(result.read()).isEqualTo(-1);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Consumer<InputStream> expectInputStream(byte[] expected) {
		return actual -> {
			try (actual) {
				byte[] actualBytes = actual.readAllBytes();
				assertThat(actualBytes).isEqualTo(expected);
			} catch (IOException ignored) {
			}
		};
	}

}
