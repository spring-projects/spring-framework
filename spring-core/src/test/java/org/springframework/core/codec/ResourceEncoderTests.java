/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.codec;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ResourceEncoderTests extends AbstractEncoderTestCase<Resource, ResourceEncoder> {

	private final byte[] bytes = "foo".getBytes(UTF_8);

	public ResourceEncoderTests() {
		super(new ResourceEncoder(), Resource.class);
	}

	@Override
	protected Flux<Resource> input() {
		return Flux.just(new ByteArrayResource(this.bytes));
	}

	@Override
	protected Stream<Consumer<DataBuffer>> outputConsumers() {
		return Stream.<Consumer<DataBuffer>>builder()
				.add(resultConsumer(this.bytes))
				.build();
	}


	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(InputStreamResource.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(ByteArrayResource.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(Resource.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(InputStreamResource.class),
				MimeTypeUtils.APPLICATION_JSON));

		// SPR-15464
		assertFalse(this.encoder.canEncode(ResolvableType.NONE, null));
	}

}
