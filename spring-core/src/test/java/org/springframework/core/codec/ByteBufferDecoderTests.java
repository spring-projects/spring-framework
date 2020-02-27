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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class ByteBufferDecoderTests extends AbstractDecoderTests<ByteBufferDecoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);


	ByteBufferDecoderTests() {
		super(new ByteBufferDecoder());
	}

	@Override
	@Test
	public void canDecode() {
		assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();
	}

	@Override
	@Test
	public void decode() {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));

		testDecodeAll(input, ByteBuffer.class, step -> step
				.consumeNextWith(expectByteBuffer(ByteBuffer.wrap(this.fooBytes)))
				.consumeNextWith(expectByteBuffer(ByteBuffer.wrap(this.barBytes)))
				.verifyComplete());


	}

	@Override
	@Test
	public void decodeToMono() {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));
		ByteBuffer expected = ByteBuffer.allocate(this.fooBytes.length + this.barBytes.length);
		expected.put(this.fooBytes).put(this.barBytes).flip();

		testDecodeToMonoAll(input, ByteBuffer.class, step -> step
				.consumeNextWith(expectByteBuffer(expected))
				.verifyComplete());

	}

	private Consumer<ByteBuffer> expectByteBuffer(ByteBuffer expected) {
		return actual -> assertThat(actual).isEqualTo(expected);
	}

}
