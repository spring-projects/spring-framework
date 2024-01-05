/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class ByteBufferEncoderTests extends AbstractEncoderTests<ByteBufferEncoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

	ByteBufferEncoderTests() {
		super(new ByteBufferEncoder());
	}

	@Override
	@Test
	protected void canEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
	}

	@Override
	@Test
	protected void encode() {
		Flux<ByteBuffer> input = Flux.just(this.fooBytes, this.barBytes)
				.map(ByteBuffer::wrap);

		testEncodeAll(input, ByteBuffer.class, step -> step
				.consumeNextWith(expectBytes(this.fooBytes))
				.consumeNextWith(expectBytes(this.barBytes))
				.verifyComplete());
	}

}
