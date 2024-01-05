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

import java.util.Map;

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
import org.springframework.lang.Nullable;
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

		Flux<DataBuffer> result = ((Encoder<Resource>) this.encoder).encode(i,
				this.bufferFactory, outputType,
				mimeType, hints);

		StepVerifier.create(result)
				.expectError(InputException.class)
				.verify();
	}

}
