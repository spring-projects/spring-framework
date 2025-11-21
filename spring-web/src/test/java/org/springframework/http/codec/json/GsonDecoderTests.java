/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.codec.json;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_NDJSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Tests for {@link GsonDecoder}.
 */
class GsonDecoderTests extends AbstractDecoderTests<GsonDecoder> {


	public GsonDecoderTests() {
		super(new GsonDecoder());
	}

	@Test
	@Override
	protected void canDecode() throws Exception {
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_JSON)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_NDJSON)).isFalse();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), null)).isTrue();

		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), null)).isFalse();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();
	}

	@Test
	@Override
	protected void decode() throws Exception {
		Flux<DataBuffer> input = Flux.concat(
				stringBuffer("[{\"bar\":\"b1\",\"foo\":\"f1\"},"),
				stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}]"));
		assertThatThrownBy(() -> decoder.decode(input, ResolvableType.forClass(Pojo.class), APPLICATION_JSON, null))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	@Override
	protected void decodeToMono() throws Exception {
		Flux<DataBuffer> input = Flux.concat(
				stringBuffer("[{\"bar\":\"b1\",\"foo\":\"f1\"},"),
				stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);

		testDecodeToMonoAll(input, elementType, step -> step
				.expectNext(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")))
				.expectComplete()
				.verify(), null, null);
	}

	private Mono<DataBuffer> stringBuffer(String value) {
		return stringBuffer(value, StandardCharsets.UTF_8);
	}


	private Mono<DataBuffer> stringBuffer(String value, Charset charset) {
		return Mono.defer(() -> {
			byte[] bytes = value.getBytes(charset);
			DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
			buffer.write(bytes);
			return Mono.just(buffer);
		});
	}
}
