/*
 * Copyright 2002-2016 the original author or authors.
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

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
@RunWith(Parameterized.class)
public class StringEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private StringEncoder encoder;

	@Before
	public void createEncoder() {
		this.encoder = new StringEncoder();
	}

	@Test
	public void canWrite() {
		assertTrue(this.encoder
				.canEncode(ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN));
		assertFalse(this.encoder
				.canEncode(ResolvableType.forClass(Integer.class), MediaType.TEXT_PLAIN));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(String.class),
				MediaType.APPLICATION_JSON));
	}

	@Test
	public void write() throws InterruptedException {
		Flux<String> output = Flux.from(
				this.encoder.encode(Flux.just("foo"), this.bufferFactory, null, null))
						.map(chunk -> {
							byte[] b = new byte[chunk.readableByteCount()];
							chunk.read(b);
							DataBufferUtils.release(chunk);
							return new String(b, StandardCharsets.UTF_8);
		});
		TestSubscriber
				.subscribe(output)
				.assertValues("foo");
	}

}
