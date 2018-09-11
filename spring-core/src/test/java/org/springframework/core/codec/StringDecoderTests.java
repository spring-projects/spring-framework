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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StringDecoder}.
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Mark Paluch
 */
public class StringDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private StringDecoder decoder = StringDecoder.allMimeTypes();


	@Test
	public void canDecode() {

		assertTrue(this.decoder.canDecode(
				ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_PLAIN));

		assertTrue(this.decoder.canDecode(
				ResolvableType.forClass(String.class), MimeTypeUtils.TEXT_HTML));

		assertTrue(this.decoder.canDecode(
				ResolvableType.forClass(String.class), MimeTypeUtils.APPLICATION_JSON));

		assertTrue(this.decoder.canDecode(
				ResolvableType.forClass(String.class), MimeTypeUtils.parseMimeType("text/plain;charset=utf-8")));


		assertFalse(this.decoder.canDecode(
				ResolvableType.forClass(Integer.class), MimeTypeUtils.TEXT_PLAIN));

		assertFalse(this.decoder.canDecode(
				ResolvableType.forClass(Object.class), MimeTypeUtils.APPLICATION_JSON));
	}

	@Test
	public void decodeMultibyteCharacter() {
		String s = "üéø";
		Flux<DataBuffer> source = toSingleByteDataBuffers(s);

		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());
		StepVerifier.create(output)
				.expectNext(s)
				.verifyComplete();
	}

	private Flux<DataBuffer> toSingleByteDataBuffers(String s) {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

		List<DataBuffer> dataBuffers = new ArrayList<>();
		for (byte b : bytes) {
			dataBuffers.add(this.bufferFactory.wrap(new byte[]{b}));
		}
		return Flux.fromIterable(dataBuffers);
	}

	@Test
	public void decodeNewLine() {
		Flux<DataBuffer> source = Flux.just(
				stringBuffer("\r\nabc\n"),
				stringBuffer("def"),
				stringBuffer("ghi\r\n\n"),
				stringBuffer("jkl"),
				stringBuffer("mno\npqr\n"),
				stringBuffer("stu"),
				stringBuffer("vw"),
				stringBuffer("xyz")
		);

		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("")
				.expectNext("abc")
				.expectNext("defghi")
				.expectNext("")
				.expectNext("jklmno")
				.expectNext("pqr")
				.expectNext("stuvwxyz")
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeNewLineIncludeDelimiters() {

		decoder = StringDecoder.allMimeTypes(StringDecoder.DEFAULT_DELIMITERS, false);

		Flux<DataBuffer> source = Flux.just(
				stringBuffer("\r\nabc\n"),
				stringBuffer("def"),
				stringBuffer("ghi\r\n\n"),
				stringBuffer("jkl"),
				stringBuffer("mno\npqr\n"),
				stringBuffer("stu"),
				stringBuffer("vw"),
				stringBuffer("xyz")
		);

		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("\r\n")
				.expectNext("abc\n")
				.expectNext("defghi\r\n")
				.expectNext("\n")
				.expectNext("jklmno\n")
				.expectNext("pqr\n")
				.expectNext("stuvwxyz")
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeEmptyFlux() {
		Flux<DataBuffer> source = Flux.empty();
		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNextCount(0)
				.expectComplete()
				.verify();

	}

	@Test
	public void decodeEmptyDataBuffer() {
		Flux<DataBuffer> source = Flux.just(stringBuffer(""));
		Flux<String> output = this.decoder.decode(source,
				ResolvableType.forClass(String.class), null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("")
				.expectComplete().verify();

	}

	@Test
	public void decodeError() {
		DataBuffer fooBuffer = stringBuffer("foo\n");
		Flux<DataBuffer> source =
				Flux.just(fooBuffer).concatWith(Flux.error(new RuntimeException()));

		Flux<String> output = this.decoder.decode(source,
				ResolvableType.forClass(String.class), null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("foo")
				.expectError()
				.verify();

	}

	@Test
	public void decodeToMono() {
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
