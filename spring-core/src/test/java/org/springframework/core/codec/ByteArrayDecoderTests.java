/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

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
 * @author Arjen Poutsma
 */
class ByteArrayDecoderTests extends AbstractDecoderTests<ByteArrayDecoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);


	ByteArrayDecoderTests() {
		super(new ByteArrayDecoder());
	}

	@Override
	@Test
	protected void canDecode() {
		assertThat(this.decoder.canDecode(ResolvableType.forClass(byte[].class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(byte[].class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();
	}

	@Override
	@Test
	protected void decode() {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));

		testDecodeAll(input, byte[].class, step -> step
				.consumeNextWith(expectBytes(this.fooBytes))
				.consumeNextWith(expectBytes(this.barBytes))
				.verifyComplete());

	}

	@Override
	@Test
	protected void decodeToMono() {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));

		byte[] expected = new byte[this.fooBytes.length + this.barBytes.length];
		System.arraycopy(this.fooBytes, 0, expected, 0, this.fooBytes.length);
		System.arraycopy(this.barBytes, 0, expected, this.fooBytes.length, this.barBytes.length);

		testDecodeToMonoAll(input, byte[].class, step -> step
				.consumeNextWith(expectBytes(expected))
				.verifyComplete());
	}

	private Consumer<byte[]> expectBytes(byte[] expected) {
		return bytes -> assertThat(bytes).isEqualTo(expected);
	}

}
