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

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CharBufferDecoder}.
 *
 * @author Markus Heiden
 * @author Arjen Poutsma
 */
class CharBufferDecoderTests extends AbstractDecoderTests<CharBufferDecoder> {

	private static final ResolvableType TYPE = ResolvableType.forClass(CharBuffer.class);

	CharBufferDecoderTests() {
		super(CharBufferDecoder.allMimeTypes());
	}

	@Override
	@Test
	protected void canDecode() {
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.TEXT_HTML)).isTrue();
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.APPLICATION_JSON)).isTrue();
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.parseMimeType("text/plain;charset=utf-8"))).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class), MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Object.class), MimeTypeUtils.APPLICATION_JSON)).isFalse();
	}

	@Override
	@Test
	protected void decode() {
		CharBuffer u = charBuffer("ü");
		CharBuffer e = charBuffer("é");
		CharBuffer o = charBuffer("ø");
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> input = toDataBuffers(s, 1, UTF_8);

		testDecodeAll(input, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), null, null);
	}

	@Test
	void decodeMultibyteCharacterUtf16() {
		CharBuffer u = charBuffer("ü");
		CharBuffer e = charBuffer("é");
		CharBuffer o = charBuffer("ø");
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
	void decodeNewLine() {
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

		testDecode(input, CharBuffer.class, step -> step
				.expectNext(charBuffer("")).as("1st")
				.expectNext(charBuffer("abc"))
				.expectNext(charBuffer("defghi"))
				.expectNext(charBuffer("")).as("2nd")
				.expectNext(charBuffer("jklmno"))
				.expectNext(charBuffer("pqr"))
				.expectNext(charBuffer("stuvwxyz"))
				.expectComplete()
				.verify());
	}

	@Test
	void decodeNewlinesAcrossBuffers() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("\r"),
				stringBuffer("\n"),
				stringBuffer("xyz")
		);

		testDecode(input, CharBuffer.class, step -> step
				.expectNext(charBuffer(""))
				.expectNext(charBuffer("xyz"))
				.expectComplete()
				.verify());
	}

	@Test
	void maxInMemoryLimit() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("abc\n"), stringBuffer("defg\n"),
				stringBuffer("hi"), stringBuffer("jkl"), stringBuffer("mnop"));

		this.decoder.setMaxInMemorySize(5);

		testDecode(input, CharBuffer.class, step -> step
				.expectNext(charBuffer("abc"))
				.expectNext(charBuffer("defg"))
				.verifyError(DataBufferLimitException.class));
	}

	@Test
	void maxInMemoryLimitDoesNotApplyToParsedItemsThatDontRequireBuffering() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("TOO MUCH DATA\nanother line\n\nand another\n"));

		this.decoder.setMaxInMemorySize(5);

		testDecode(input, CharBuffer.class, step -> step
				.expectNext(charBuffer("TOO MUCH DATA"))
				.expectNext(charBuffer("another line"))
				.expectNext(charBuffer(""))
				.expectNext(charBuffer("and another"))
				.expectComplete()
				.verify());
	}

	@Test
		// gh-24339
	void maxInMemoryLimitReleaseUnprocessedLinesWhenUnlimited() {
		Flux<DataBuffer> input = Flux.just(stringBuffer("Line 1\nLine 2\nLine 3\n"));

		this.decoder.setMaxInMemorySize(-1);
		testDecodeCancel(input, ResolvableType.forClass(String.class), null, Collections.emptyMap());
	}

	@Test
	void decodeNewLineIncludeDelimiters() {
		this.decoder = CharBufferDecoder.allMimeTypes(CharBufferDecoder.DEFAULT_DELIMITERS, false);

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

		testDecode(input, CharBuffer.class, step -> step
				.expectNext(charBuffer("\r\n"))
				.expectNext(charBuffer("abc\n"))
				.expectNext(charBuffer("defghi\r\n"))
				.expectNext(charBuffer("\n"))
				.expectNext(charBuffer("jklmno\n"))
				.expectNext(charBuffer("pqr\n"))
				.expectNext(charBuffer("stuvwxyz"))
				.expectComplete()
				.verify());
	}

	@Test
	void decodeEmptyFlux() {
		Flux<DataBuffer> input = Flux.empty();

		testDecode(input, String.class, step -> step
				.expectComplete()
				.verify());
	}

	@Test
	void decodeEmptyDataBuffer() {
		Flux<DataBuffer> input = Flux.just(stringBuffer(""));
		Flux<CharBuffer> output = this.decoder.decode(input,
				TYPE, null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(charBuffer(""))
				.expectComplete().verify();
	}

	@Override
	@Test
	protected void decodeToMono() {
		Flux<DataBuffer> input = Flux.just(
				stringBuffer("foo"),
				stringBuffer("bar"),
				stringBuffer("baz"));

		testDecodeToMonoAll(input, CharBuffer.class, step -> step
				.expectNext(charBuffer("foobarbaz"))
				.expectComplete()
				.verify());
	}

	@Test
	void decodeToMonoWithEmptyFlux() {
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

	private CharBuffer charBuffer(String value) {
		return CharBuffer
				.allocate(value.length())
				.put(value)
				.flip();
	}

}
