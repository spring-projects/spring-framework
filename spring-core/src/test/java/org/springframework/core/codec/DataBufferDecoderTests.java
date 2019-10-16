/*
 * Copyright 2002-2019 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class DataBufferDecoderTests extends AbstractDecoderTests<DataBufferDecoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);


	DataBufferDecoderTests() {
		super(new DataBufferDecoder());
	}

	@Override
	@Test
	public void canDecode() {
		assertThat(this.decoder.canDecode(ResolvableType.forClass(DataBuffer.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(DataBuffer.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();
	}

	@Override
	@Test
	public void decode() {
		Flux<DataBuffer> input = Flux.just(
				this.bufferFactory.wrap(this.fooBytes),
				this.bufferFactory.wrap(this.barBytes));

		testDecodeAll(input, DataBuffer.class, step -> step
				.consumeNextWith(expectDataBuffer(this.fooBytes))
				.consumeNextWith(expectDataBuffer(this.barBytes))
				.verifyComplete());
	}

	@Override
	@Test
	public void decodeToMono() throws Exception {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));

		byte[] expected = new byte[this.fooBytes.length + this.barBytes.length];
		System.arraycopy(this.fooBytes, 0, expected, 0, this.fooBytes.length);
		System.arraycopy(this.barBytes, 0, expected, this.fooBytes.length, this.barBytes.length);

		testDecodeToMonoAll(input, DataBuffer.class, step -> step
				.consumeNextWith(expectDataBuffer(expected))
				.verifyComplete());
	}

	private Consumer<DataBuffer> expectDataBuffer(byte[] expected) {
		return actual -> {
			byte[] actualBytes = new byte[actual.readableByteCount()];
			actual.read(actualBytes);
			assertThat(actualBytes).isEqualTo(expected);

			DataBufferUtils.release(actual);
		};
	}

}
