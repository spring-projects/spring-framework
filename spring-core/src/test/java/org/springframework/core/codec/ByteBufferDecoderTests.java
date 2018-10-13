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

import java.nio.ByteBuffer;
import java.util.Collections;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class ByteBufferDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private final ByteBufferDecoder decoder = new ByteBufferDecoder();

	@Test
	public void canDecode() {
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(ByteBuffer.class),
				MimeTypeUtils.APPLICATION_JSON));
	}

	@Test
	public void decode() {
		DataBuffer fooBuffer = stringBuffer("foo");
		DataBuffer barBuffer = stringBuffer("bar");
		Flux<DataBuffer> source = Flux.just(fooBuffer, barBuffer);
		Flux<ByteBuffer> output = this.decoder.decode(source,
				ResolvableType.forClassWithGenerics(Publisher.class, ByteBuffer.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(ByteBuffer.wrap("foo".getBytes()), ByteBuffer.wrap("bar".getBytes()))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeError() {
		DataBuffer fooBuffer = stringBuffer("foo");
		Flux<DataBuffer> source =
				Flux.just(fooBuffer).concatWith(Flux.error(new RuntimeException()));
		Flux<ByteBuffer> output = this.decoder.decode(source,
				ResolvableType.forClassWithGenerics(Publisher.class, ByteBuffer.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(ByteBuffer.wrap("foo".getBytes()))
				.expectError()
				.verify();
	}

	@Test
	public void decodeToMono() {
		DataBuffer fooBuffer = stringBuffer("foo");
		DataBuffer barBuffer = stringBuffer("bar");
		Flux<DataBuffer> source = Flux.just(fooBuffer, barBuffer);
		Mono<ByteBuffer> output = this.decoder.decodeToMono(source,
				ResolvableType.forClassWithGenerics(Publisher.class, ByteBuffer.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(ByteBuffer.wrap("foobar".getBytes()))
				.expectComplete()
				.verify();
	}
}
