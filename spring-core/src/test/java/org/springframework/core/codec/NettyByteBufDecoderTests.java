/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vladislav Kisel
 */
class NettyByteBufDecoderTests extends AbstractDecoderTests<NettyByteBufDecoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);


	NettyByteBufDecoderTests() {
		super(new NettyByteBufDecoder());
	}

	@Override
	@Test
	protected void canDecode() {
		assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuf.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(ByteBuf.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();
	}

	@Override
	@Test
	protected void decode() {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));

		testDecodeAll(input, ByteBuf.class, step -> step
				.consumeNextWith(expectByteBuffer(Unpooled.copiedBuffer(this.fooBytes)))
				.consumeNextWith(expectByteBuffer(Unpooled.copiedBuffer(this.barBytes)))
				.verifyComplete());
	}

	@Override
	@Test
	protected void decodeToMono() {
		Flux<DataBuffer> input = Flux.concat(
				dataBuffer(this.fooBytes),
				dataBuffer(this.barBytes));

		ByteBuf expected = Unpooled.buffer(this.fooBytes.length + this.barBytes.length)
				.writeBytes(this.fooBytes)
				.writeBytes(this.barBytes)
				.readerIndex(0);

		testDecodeToMonoAll(input, ByteBuf.class, step -> step
				.consumeNextWith(expectByteBuffer(expected))
				.verifyComplete());
	}

	private Consumer<ByteBuf> expectByteBuffer(ByteBuf expected) {
		return actual -> assertThat(actual).isEqualTo(expected);
	}

}
