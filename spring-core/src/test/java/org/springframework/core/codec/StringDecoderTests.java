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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
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
public class StringDecoderTests extends AbstractDecoderTestCase<StringDecoder> {

	private static final ResolvableType TYPE = ResolvableType.forClass(String.class);

	public StringDecoderTests() {
		super(StringDecoder.allMimeTypes());
	}

	@Override
	@Test
	public void canDecode() {
		assertTrue(this.decoder.canDecode(
				TYPE, MimeTypeUtils.TEXT_PLAIN));

		assertTrue(this.decoder.canDecode(
				TYPE, MimeTypeUtils.TEXT_HTML));

		assertTrue(this.decoder.canDecode(
				TYPE, MimeTypeUtils.APPLICATION_JSON));

		assertTrue(this.decoder.canDecode(
				TYPE, MimeTypeUtils.parseMimeType("text/plain;charset=utf-8")));


		assertFalse(this.decoder.canDecode(
				ResolvableType.forClass(Integer.class), MimeTypeUtils.TEXT_PLAIN));

		assertFalse(this.decoder.canDecode(
				ResolvableType.forClass(Object.class), MimeTypeUtils.APPLICATION_JSON));
	}

	@Override
	@Test
	public void decode() {
		String u = "ü";
		String e = "é";
		String o = "ø";
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> input = toDataBuffers(s, 1, UTF_8);

		testDecodeAll(input, ResolvableType.forClass(String.class), step -> step
				.expectNext(u, e, o)
				.verifyComplete(), null, null);
	}

	@Override
	protected void testDecodeError(Publisher<DataBuffer> input, ResolvableType outputType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		input = Flux.concat(
				Flux.from(input).take(1),
				Flux.error(new InputException()));

		Flux<String> result = this.decoder.decode(input, outputType, mimeType, hints);

		StepVerifier.create(result)
				.expectError(InputException.class)
				.verify();
	}

	@Test
	public void decodeMultibyteCharacterUtf16() {
		String u = "ü";
		String e = "é";
		String o = "ø";
		String s = String.format("%s\n%s\n%s", u, e, o);
		Flux<DataBuffer> source = toDataBuffers(s, 2, UTF_16BE);
		MimeType mimeType = MimeTypeUtils.parseMimeType("text/plain;charset=utf-16be");

		testDecode(source, TYPE, step -> step
				.expectNext(u, e, o)
				.verifyComplete(), mimeType, null);
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
	public void decodeToMonoWithEmptyFlux() throws InterruptedException {
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
