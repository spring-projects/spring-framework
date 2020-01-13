/*
 * Copyright 2002-2020 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link StringDecoder}.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Mark Paluch
 */
public class StringDecoderTests extends AbstractDecoderTestCase<StringDecoder> {

	private static final ResolvableType TYPE = ResolvableType.forClass(String.class);


	public StringDecoderTests() {
		super(StringDecoder.allMimeTypes());
	}


	@Override
	@Test
	public void canDecode() {
		assertTrue(this.decoder.canDecode(TYPE, MimeTypeUtils.TEXT_PLAIN));
		assertTrue(this.decoder.canDecode(TYPE, MimeTypeUtils.TEXT_HTML));
		assertTrue(this.decoder.canDecode(TYPE, MimeTypeUtils.APPLICATION_JSON));
		assertTrue(this.decoder.canDecode(TYPE, MimeTypeUtils.parseMimeType("text/plain;charset=utf-8")));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(Integer.class), MimeTypeUtils.TEXT_PLAIN));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(Object.class), MimeTypeUtils.APPLICATION_JSON));
	}

	@Override
	@Test
	public void decode() {
		String u = "ü";
		String e = "é";
		String o = "ø";
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> input = toDataBuffers(s, 1, UTF_8);

		testDecodeAll(input, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), null, null);
	}

	@Test
	public void decodeMultibyteCharacterUtf16() {
		String u = "ü";
		String e = "é";
		String o = "ø";
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> source = toDataBuffers(s, 2, UTF_16BE);
		MimeType mimeType = MimeTypeUtils.parseMimeType("text/plain;charset=utf-16be");

		testDecode(source, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), mimeType, null);
	}

	private Flux<DataBuffer> toDataBuffers(String s, int length, Charset charset) {
		byte[] bytes = s.getBytes(charset);
		List<byte[]> chunks = new ArrayList<>();
		for (int i = 0; i < bytes.length; i += length) {
			chunks.add(Arrays.copyOfRange(bytes, i, i + length));
		}
		return Flux.fromIterable(chunks)
				.map(chunk -> {
					DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(length);
					dataBuffer.write(chunk, 0, chunk.length);
					return dataBuffer;
				});
	}

	@Test
	public void decodeNewLine() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("\r\nabc\n"),
				stringBuffer("def"),
				stringBuffer("ghi\r\n\n"),
				stringBuffer("jkl"),
				stringBuffer("mno\npqr\n"),
				stringBuffer("stu"),
				stringBuffer("vw"),
				stringBuffer("xyz")
		);

		testDecode(input, String.class, step -> step
				.expectNext("")
				.expectNext("abc")
				.expectNext("defghi")
				.expectNext("")
				.expectNext("jklmno")
				.expectNext("pqr")
				.expectNext("stuvwxyz")
				.expectComplete()
				.verify());
	}

	@Test
	public void maxInMemoryLimit() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("abc\n"), stringBuffer("defg\n"), stringBuffer("hijkl\n"));

		this.decoder.setMaxInMemorySize(4);
		testDecode(input, String.class, step ->
				step.expectNext("abc", "defg").verifyError(DataBufferLimitException.class));
	}

	@Test // gh-24312
	public void maxInMemoryLimitReleaseUnprocessedLinesFromCurrentBuffer() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("TOO MUCH DATA\nanother line\n\nand another\n"));

		this.decoder.setMaxInMemorySize(5);
		testDecode(input, String.class, step -> step.verifyError(DataBufferLimitException.class));
	}

	@Test // gh-24339
	public void maxInMemoryLimitReleaseUnprocessedLinesWhenUnlimited() {
		Flux<DataBuffer> input = Flux.just(stringBuffer("Line 1\nLine 2\nLine 3\n"));

		this.decoder.setMaxInMemorySize(-1);
		testDecodeCancel(input, ResolvableType.forClass(String.class), null, Collections.emptyMap());
	}

	@Test
	public void decodeNewLineIncludeDelimiters() {
		this.decoder = StringDecoder.allMimeTypes(StringDecoder.DEFAULT_DELIMITERS, false);

		Flux<DataBuffer> input = Flux.just(
				stringBuffer("\r\nabc\n"),
				stringBuffer("def"),
				stringBuffer("ghi\r\n\n"),
				stringBuffer("jkl"),
				stringBuffer("mno\npqr\n"),
				stringBuffer("stu"),
				stringBuffer("vw"),
				stringBuffer("xyz")
		);

		testDecode(input, String.class, step -> step
				.expectNext("\r\n")
				.expectNext("abc\n")
				.expectNext("defghi\r\n")
				.expectNext("\n")
				.expectNext("jklmno\n")
				.expectNext("pqr\n")
				.expectNext("stuvwxyz")
				.expectComplete()
				.verify());
	}

	@Test
	public void decodeEmptyFlux() {
		Flux<DataBuffer> input = Flux.empty();

		testDecode(input, String.class, step -> step
				.expectComplete()
				.verify());
	}

	@Test
	public void decodeEmptyDataBuffer() {
		Flux<DataBuffer> input = Flux.just(stringBuffer(""));
		Flux<String> output = this.decoder.decode(input,
				TYPE, null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext("")
				.expectComplete().verify();

	}

	@Override
	@Test
	public void decodeToMono() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("foo"),
				stringBuffer("bar"),
				stringBuffer("baz"));

		testDecodeToMonoAll(input, String.class, step -> step
				.expectNext("foobarbaz")
				.expectComplete()
				.verify());
	}

	@Test
	public void decodeToMonoWithEmptyFlux() {
		Flux<DataBuffer> input = Flux.empty();

		testDecodeToMono(input, String.class, step -> step
				.expectComplete()
				.verify());
	}

	private DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}


}
