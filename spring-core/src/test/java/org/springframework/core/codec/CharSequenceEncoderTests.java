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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.core.publisher.Flux;
import reactor.test.subscriber.Verifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
@RunWith(Parameterized.class)
public class CharSequenceEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private CharSequenceEncoder encoder;

	@Before
	public void createEncoder() {
		this.encoder = new CharSequenceEncoder();
	}

	@Test
	public void canWrite() {
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(String.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(StringBuilder.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(StringBuffer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(String.class),
				MimeTypeUtils.APPLICATION_JSON));
	}

	@Test
	public void writeString() {
		Flux<String> stringFlux = Flux.just("foo");
		Flux<DataBuffer> output = Flux.from(
				this.encoder.encode(stringFlux, this.bufferFactory, null, null, Collections.emptyMap()));
		Verifier.create(output)
				.consumeNextWith(stringConsumer("foo"))
				.expectComplete()
				.verify();
	}

	@Test
	public void writeStringBuilder() {
		Flux<StringBuilder> stringBuilderFlux = Flux.just(new StringBuilder("foo"));
		Flux<DataBuffer> output = Flux.from(
				this.encoder.encode(stringBuilderFlux, this.bufferFactory, null, null, Collections.emptyMap()));
		Verifier.create(output)
				.consumeNextWith(stringConsumer("foo"))
				.expectComplete()
				.verify();
	}

}
