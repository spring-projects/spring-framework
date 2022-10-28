/*
 * Copyright 2002-2022 the original author or authors.
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

import io.netty5.buffer.Buffer;
import io.netty5.buffer.DefaultBufferAllocators;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class Netty5BufferEncoderTests extends AbstractEncoderTests<Netty5BufferEncoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

	Netty5BufferEncoderTests() {
		super(new Netty5BufferEncoder());
	}

	@Test
	@Override
	public void canEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Buffer.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Buffer.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();

		// gh-20024
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
	}

	@Test
	@Override
	@SuppressWarnings("resource")
	public void encode() {
		Flux<Buffer> input = Flux.just(this.fooBytes, this.barBytes)
				.map(DefaultBufferAllocators.preferredAllocator()::copyOf);

		testEncodeAll(input, Buffer.class, step -> step
				.consumeNextWith(expectBytes(this.fooBytes))
				.consumeNextWith(expectBytes(this.barBytes))
				.verifyComplete());
	}

}
