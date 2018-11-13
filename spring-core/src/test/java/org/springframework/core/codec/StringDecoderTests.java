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

import java.nio.charset.Charset;
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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_8;
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
		String u = "ü";
		String e = "é";
		String o = "ø";
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> source = toDataBuffers(s, 1, UTF_8);

		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				null, Collections.emptyMap());
		StepVerifier.create(output)
				.expectNext(u, e, o)
				.verifyComplete();
	}

	@Test
	public void decodeMultibyteCharacterUtf16() {
		String u = "ü";
		String e = "é";
		String o = "ø";
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> source = toDataBuffers(s, 2, UTF_16BE);

		MimeType mimeType = MimeTypeUtils.parseMimeType("text/plain;charset=utf-16be");
		Flux<String> output = this.decoder.decode(source, ResolvableType.forClass(String.class),
				mimeType, Collections.emptyMap());
		StepVerifier.create(output)
				.expectNext(u, e, o)
				.verifyComplete();
	}

	private Flux<DataBuffer> toDataBuffers(String s, int length, Charset charset) {
		byte[] bytes = s.getBytes(charset);

		List<DataBuffer> dataBuffers = new ArrayList<>();
		for (int i = 0; i < bytes.length; i += length) {
			DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(length);
			dataBuffer.write(bytes, i, length);
			dataBuffers.add(dataBuffer);
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
		DataBuffer barBuffer = stringBuffer("bar");
		Flux<DataBuffer> source =
				Flux.just(fooBuffer, barBuffer).concatWith(Flux.error(new RuntimeException()));

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
