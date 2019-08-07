/*
 * Copyright 2002-2017 the original author or authors.
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
public class ByteArrayEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private ByteArrayEncoder encoder;

	@Before
	public void createEncoder() {
		this.encoder = new ByteArrayEncoder();
	}

	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(byte[].class),
				MimeTypeUtils.TEXT_PLAIN));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(byte[].class),
				MimeTypeUtils.APPLICATION_JSON));

		// SPR-15464
		assertFalse(this.encoder.canEncode(ResolvableType.NONE, null));
	}

	@Test
	public void encode() {
		byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);
		byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);
		Flux<byte[]> source = Flux.just(fooBytes, barBytes);

		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory,
				ResolvableType.forClassWithGenerics(Publisher.class, ByteBuffer.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.consumeNextWith(b -> {
					byte[] buf = new byte[3];
					b.read(buf);
					assertArrayEquals(fooBytes, buf);
				})
				.consumeNextWith(b -> {
					byte[] buf = new byte[3];
					b.read(buf);
					assertArrayEquals(barBytes, buf);
				})
				.expectComplete()
				.verify();
	}

}
