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

import java.nio.charset.Charset;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class CharSequenceEncoderTests extends AbstractEncoderTests<CharSequenceEncoder> {

	private final String foo = "foo";

	private final String bar = "bar";

	CharSequenceEncoderTests() {
		super(CharSequenceEncoder.textPlainOnly());
	}


	@Override
	@Test
	public void canEncode() throws Exception {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(StringBuilder.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(StringBuffer.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class),
				MimeTypeUtils.APPLICATION_JSON)).isFalse();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
	}

	@Override
	@Test
	public void encode() {
		Flux<CharSequence> input = Flux.just(this.foo, this.bar);

		testEncodeAll(input, CharSequence.class, step -> step
				.consumeNextWith(expectString(this.foo))
				.consumeNextWith(expectString(this.bar))
				.verifyComplete());
	}

	@Test
	void calculateCapacity() {
		String sequence = "Hello World!";
		Stream.of(UTF_8, UTF_16, ISO_8859_1, US_ASCII, Charset.forName("BIG5"))
				.forEach(charset -> {
					int capacity = this.encoder.calculateCapacity(sequence, charset);
					int length = sequence.length();
					assertThat(capacity >= length).as(String.format("%s has capacity %d; length %d", charset, capacity, length)).isTrue();
				});
	}

}
