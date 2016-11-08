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

package org.springframework.http.codec;

import java.nio.ByteBuffer;
import java.util.Collections;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link EncoderHttpMessageWriter}.
 *
 * @author Marcin Kamionowski
 * @author Rossen Stoyanchev
 */
public class EncoderHttpMessageWriterTests {

	private MockServerHttpResponse response = new MockServerHttpResponse();


	@Test
	public void writableMediaTypes() throws Exception {
		EncoderHttpMessageWriter<ByteBuffer> writer = createWriter(new ByteBufferEncoder());
		assertThat(writer.getWritableMediaTypes(), containsInAnyOrder(MimeTypeUtils.ALL));
	}

	@Test
	public void supportedMediaTypes() throws Exception {
		EncoderHttpMessageWriter<ByteBuffer> writer = createWriter(new ByteBufferEncoder());
		assertTrue(writer.canWrite(ResolvableType.forClass(ByteBuffer.class), MediaType.ALL));
		assertTrue(writer.canWrite(ResolvableType.forClass(ByteBuffer.class), MediaType.TEXT_PLAIN));
	}

	@Test
	public void encodeByteBuffer(){
		String payload = "Buffer payload";
		Mono<ByteBuffer> source = Mono.just(ByteBuffer.wrap(payload.getBytes(UTF_8)));

		EncoderHttpMessageWriter<ByteBuffer> writer = createWriter(new ByteBufferEncoder());
		writer.write(source, ResolvableType.forClass(ByteBuffer.class),
				MediaType.APPLICATION_OCTET_STREAM, this.response, Collections.emptyMap());

		assertThat(this.response.getHeaders().getContentType(), is(MediaType.APPLICATION_OCTET_STREAM));
		StepVerifier.create(this.response.getBodyAsString())
				.expectNext(payload)
				.expectComplete()
				.verify();
	}

	private <T> EncoderHttpMessageWriter<T> createWriter(Encoder<T> encoder) {
		return new EncoderHttpMessageWriter<>(encoder);
	}

}