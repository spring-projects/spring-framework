/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.nio.charset.Charset;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
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
	protected void canEncode() {
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
	protected void encode() {
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
					assertThat(capacity).as(String.format("%s has capacity %d; length %d", charset, capacity, length))
							.isGreaterThanOrEqualTo(length);
				});
	}

}
