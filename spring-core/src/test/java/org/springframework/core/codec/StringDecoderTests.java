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

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Mark Paluch
 */
public class StringDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private StringDecoder decoder = StringDecoder.allMimeTypes(true);


	@Test
	public void canDecode() {
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(String.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(String.class),
				MimeTypeUtils.TEXT_HTML));
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(String.class),
				MimeTypeUtils.APPLICATION_JSON));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(Integer.class),
				MimeTypeUtils.TEXT_PLAIN));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(Object.class),
				MimeTypeUtils.APPLICATION_JSON));
	}

	@Test
	public void decode() throws InterruptedException {
		this.decoder = StringDecoder.allMimeTypes(false);
		Flux<DataBuffer> source = Flux.just(stringBuffer("foo"), stringBuffer("bar"), stringBuffer("baz"));
		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("foo", "bar", "baz")
				.expectComplete()
				.verify();

	}

	@Test
	public void decodeNewLine() throws InterruptedException {
		DataBuffer fooBar = stringBuffer("\nfoo\r\nbar\r");
		DataBuffer baz = stringBuffer("\nbaz");
		Flux<DataBuffer> source = Flux.just(fooBar, baz);
		Flux<String> output = decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("\n", "foo\r", "\n", "bar\r", "\n", "baz")
				.expectComplete()
				.verify();

	}

	@Test
	public void decodeEmptyFlux() throws InterruptedException {
		Flux<DataBuffer> source = Flux.empty();
		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNextCount(0)
				.expectComplete()
				.verify();

	}

	@Test
	public void decodeEmptyString() throws InterruptedException {
		Flux<DataBuffer> source = Flux.just(stringBuffer(""));
		Flux<String> output = this.decoder.decode(source,
				ResolvableType.forClass(String.class), null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("")
				.expectComplete().verify();

	}

	@Test
	public void decodeToMono() throws InterruptedException {
		this.decoder = StringDecoder.allMimeTypes(false);
		Flux<DataBuffer> source = Flux.just(stringBuffer("foo"), stringBuffer("bar"), stringBuffer("baz"));
		Mono<String> output = this.decoder.decodeToMono(source,
				ResolvableType.forClass(String.class), null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("foobarbaz")
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeToMonoWithEmptyFlux() throws InterruptedException {
		Flux<DataBuffer> source = Flux.empty();
		Mono<String> output = this.decoder.decodeToMono(source,
				ResolvableType.forClass(String.class), null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

}
