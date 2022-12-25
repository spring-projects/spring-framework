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

import java.nio.CharBuffer;
import java.nio.charset.Charset;
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
 * Unit tests for {@link CharBufferDecoder}.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Mark Paluch
 */
class CharBufferDecoderTests extends AbstractDecoderTests<CharBufferDecoder> {

	private static final ResolvableType TYPE = ResolvableType.forClass(CharBuffer.class);

	CharBufferDecoderTests() {
		super(CharBufferDecoder.allMimeTypes());
	}

	@Override
	@Test
	public void canDecode() {
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.TEXT_PLAIN)).isTrue();
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.TEXT_HTML)).isTrue();
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.APPLICATION_JSON)).isTrue();
		assertThat(this.decoder.canDecode(TYPE, MimeTypeUtils.parseMimeType("text/plain;charset=utf-8"))).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Integer.class), MimeTypeUtils.TEXT_PLAIN)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Object.class), MimeTypeUtils.APPLICATION_JSON)).isFalse();
	}

	@Override
	@Test
	public void decode() {
		CharBuffer u = charBuffer("ü");
		CharBuffer e = charBuffer("é");
		CharBuffer o = charBuffer("ø");
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> input = buffers(s, 1, UTF_8);

		// TODO: temporarily replace testDecodeAll with explicit decode/cancel/empty
		// see https://github.com/reactor/reactor-core/issues/2041

//		testDecode(input, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), null, null);
//		testDecodeCancel(input, TYPE, null, null);
//		testDecodeEmpty(TYPE, null, null);

		testDecodeAll(input, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), null, null);
	}

	@Test
	void decodeMultibyteCharacterUtf16() {
		CharBuffer u = charBuffer("ü");
		CharBuffer e = charBuffer("é");
		CharBuffer o = charBuffer("ø");
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> source = buffers(s, 2, UTF_16BE);
		MimeType mimeType = MimeTypeUtils.parseMimeType("text/plain;charset=utf-16be");

		testDecode(source, TYPE, step -> step.expectNext(u, e, o).verifyComplete(), mimeType, null);
	}

	private Flux<DataBuffer> buffers(String s, int length, Charset charset) {
		byte[] bytes = s.getBytes(charset);
		List<byte[]> chunks = new ArrayList<>();
		for (int i = 0; i < bytes.length; i += length) {
			chunks.add(Arrays.copyOfRange(bytes, i, i + length));
		}
		return Flux.fromIterable(chunks)
				.map(this::buffer);
	}

	@Test
	void decodeNewLine() {
		Flux<DataBuffer> input = buffers(
				"\r\nabc\n",
				"def",
				"ghi\r\n\n",
				"jkl",
				"mno\npqr\n",
				"stu",
				"vw",
				"xyz");

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
		Flux<DataBuffer> input = buffers(
				"\r",
				"\n",
				"xyz");

		testDecode(input, CharBuffer.class, step -> step
				.expectNext(charBuffer(""))
				.expectNext(charBuffer("xyz"))
				.expectComplete()
				.verify());
	}

	@Test
	void maxInMemoryLimit() {
		Flux<DataBuffer> input = buffers(
				"abc\n", "defg\n",
				"hi", "jkl", "mnop");

		this.decoder.setMaxInMemorySize(5);

		testDecode(input, CharBuffer.class, step -> step
				.expectNext(charBuffer("abc"))
				.expectNext(charBuffer("defg"))
				.verifyError(DataBufferLimitException.class));
	}

	@Test
	void maxInMemoryLimitDoesNotApplyToParsedItemsThatDontRequireBuffering() {
		Flux<DataBuffer> input = buffers(
				"TOO MUCH DATA\nanother line\n\nand another\n");

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
		Flux<DataBuffer> input = buffers("Line 1\nLine 2\nLine 3\n");

		this.decoder.setMaxInMemorySize(-1);

		testDecodeCancel(input, ResolvableType.forClass(String.class), null, Collections.emptyMap());
	}

	@Test
	void decodeNewLineIncludeDelimiters() {
		this.decoder = CharBufferDecoder.allMimeTypes(CharBufferDecoder.DEFAULT_DELIMITERS, false);

		Flux<DataBuffer> input = buffers(
				"\r\nabc\n",
				"def",
				"ghi\r\n\n",
				"jkl",
				"mno\npqr\n",
				"stu",
				"vw",
				"xyz");

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
		Flux<DataBuffer> input = buffers("");

		Flux<CharBuffer> output = this.decoder.decode(input, TYPE, null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(charBuffer(""))
				.expectComplete().verify();
	}

	@Override
	@Test
	public void decodeToMono() {
		Flux<DataBuffer> input = buffers(
				"foo",
				"bar",
				"baz");

		testDecodeToMonoAll(input, CharBuffer.class, step -> step
				.expectNext(charBuffer("foobarbaz"))
				.expectComplete()
				.verify());
	}

	@Test
	void decodeToMonoWithEmptyFlux() {
		Flux<DataBuffer> input = buffers();

		testDecodeToMono(input, String.class, step -> step
				.expectComplete()
				.verify());
	}

	private Flux<DataBuffer> buffers(String... value) {
		return Flux.just(value).map(this::buffer);
	}

	private DataBuffer buffer(String value) {
		return buffer(value.getBytes(UTF_8));
	}

	private DataBuffer buffer(byte[] value) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(value.length);
		buffer.write(value);
		return buffer;
	}

	private CharBuffer charBuffer(String value) {
		return CharBuffer
				.allocate(value.length())
				.put(value)
				.flip();
	}

}
