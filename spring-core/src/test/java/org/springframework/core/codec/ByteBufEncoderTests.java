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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vladislav Kisel
 */
class ByteBufEncoderTests extends AbstractEncoderTests<ByteBufEncoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

	ByteBufEncoderTests() {
		super(new ByteBufEncoder());
	}

	@Override
	@Test
	public void canEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteBuf.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteBuf.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
	}

	@Override
	@Test
	public void encode() {
		Flux<ByteBuf> input = Flux.just(this.fooBytes, this.barBytes).map(Unpooled::copiedBuffer);

		Unpooled.copiedBuffer(this.fooBytes, this.barBytes);

		testEncodeAll(input, ByteBuf.class, step -> step
				.consumeNextWith(expectBytes(this.fooBytes))
				.consumeNextWith(expectBytes(this.barBytes))
				.verifyComplete());
	}

}
