/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ResourceEncoderTests extends AbstractEncoderTests<ResourceEncoder> {

	private final byte[] bytes = "foo".getBytes(UTF_8);


	ResourceEncoderTests() {
		super(new ResourceEncoder());
	}

	@Override
	@Test
	protected void canEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(InputStreamResource.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(ByteArrayResource.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Resource.class),
				MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(InputStreamResource.class),
				MimeTypeUtils.APPLICATION_JSON)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
	}

	@Override
	@Test
	protected void encode() {
		Flux<Resource> input = Flux.just(new ByteArrayResource(this.bytes));

		testEncodeAll(input, Resource.class, step -> step
				.consumeNextWith(expectBytes(this.bytes))
				.verifyComplete());
	}

	@Override
	protected void testEncodeError(Publisher<?> input, ResolvableType outputType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Flux<Resource> i = Flux.error(new InputException());

		Flux<DataBuffer> result = this.encoder.encode(i,
				this.bufferFactory, outputType,
				mimeType, hints);

		StepVerifier.create(result)
				.expectError(InputException.class)
				.verify();
	}

}
