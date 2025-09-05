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
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.EnumFeature;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;

/**
 * Tests for a customized {@link JacksonJsonDecoder}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
class CustomizedJacksonJsonDecoderTests extends AbstractDecoderTests<JacksonJsonDecoder> {

	CustomizedJacksonJsonDecoderTests() {
		super(new JacksonJsonDecoderWithCustomization());
	}


	@Override
	public void canDecode() throws Exception {
		// Not Testing, covered under JacksonJsonDecoderTests
	}

	@Test
	@Override
	public void decode() throws Exception {
		Flux<DataBuffer> input = Flux.concat(stringBuffer("{\"property\":\"Value1\"}"));

		testDecodeAll(input, MyCustomizedDecoderBean.class, step -> step
				.expectNextMatches(obj -> obj.getProperty() == MyCustomDecoderEnum.VAL1)
				.verifyComplete());
	}

	@Test
	@Override
	public void decodeToMono() throws Exception {
		Mono<DataBuffer> input = stringBuffer("{\"property\":\"Value2\"}");

		ResolvableType elementType = ResolvableType.forClass(MyCustomizedDecoderBean.class);

		testDecodeToMono(input, elementType, step -> step
				.expectNextMatches(obj -> ((MyCustomizedDecoderBean)obj).getProperty() == MyCustomDecoderEnum.VAL2)
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


	private static class MyCustomizedDecoderBean {

		private MyCustomDecoderEnum property;

		public MyCustomDecoderEnum getProperty() {
			return property;
		}

		@SuppressWarnings("unused")
		public void setProperty(MyCustomDecoderEnum property) {
			this.property = property;
		}
	}


	private enum MyCustomDecoderEnum {
		VAL1,
		VAL2;

		@Override
		public String toString() {
			return this == VAL1 ? "Value1" : "Value2";
		}
	}


	private static class JacksonJsonDecoderWithCustomization extends JacksonJsonDecoder {

		@Override
		protected ObjectReader customizeReader(
				ObjectReader reader, ResolvableType elementType, Map<String, Object> hints) {

			return reader.with(EnumFeature.READ_ENUMS_USING_TO_STRING);
		}
	}

}
