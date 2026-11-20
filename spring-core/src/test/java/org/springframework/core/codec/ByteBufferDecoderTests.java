/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	protected void canDecode() {
		assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();
	}

	@Override
	@Test
	protected void decode() {
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
	protected void decodeToMono() {
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
