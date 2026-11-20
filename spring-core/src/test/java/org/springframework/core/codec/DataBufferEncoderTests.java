/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class DataBufferEncoderTests extends AbstractEncoderTests<DataBufferEncoder> {

	private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

	private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

	DataBufferEncoderTests() {
		super(new DataBufferEncoder());
	}


	@Override
	@Test
	protected void canEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(DataBuffer.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(DataBuffer.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
	}

	@Override
	@Test
	protected void encode() {
		Flux<DataBuffer> input = Flux.just(this.fooBytes, this.barBytes)
				.flatMap(bytes -> Mono.defer(() -> {
					DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(bytes.length);
					dataBuffer.write(bytes);
					return Mono.just(dataBuffer);
				}));

		testEncodeAll(input, DataBuffer.class, step -> step
				.consumeNextWith(expectBytes(this.fooBytes))
				.consumeNextWith(expectBytes(this.barBytes))
				.verifyComplete());

	}

}
