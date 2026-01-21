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

import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.EnumFeature;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.util.MimeType;

import static org.springframework.http.MediaType.APPLICATION_NDJSON;

/**
 * Tests for a customized {@link JacksonJsonEncoder}.
 *
 * @author Sebastien Deleuze
 */
class CustomizedJacksonJsonEncoderTests extends AbstractEncoderTests<JacksonJsonEncoder> {

	CustomizedJacksonJsonEncoderTests() {
		super(new JacksonJsonEncoderWithCustomization());
	}


	@Override
	public void canEncode() throws Exception {
		// Not Testing, covered under JacksonJsonEncoderTests
	}

	@Test
	@Override
	public void encode() throws Exception {
		Flux<MyCustomizedEncoderBean> input = Flux.just(
				new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL1),
				new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL2)
		);

		testEncodeAll(input, ResolvableType.forClass(MyCustomizedEncoderBean.class), APPLICATION_NDJSON, null, step -> step
				.consumeNextWith(expectString("{\"property\":\"Value1\"}\n"))
				.consumeNextWith(expectString("{\"property\":\"Value2\"}\n"))
				.verifyComplete()
		);
	}

	@Test
	void encodeNonStream() {
		Flux<MyCustomizedEncoderBean> input = Flux.just(
				new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL1),
				new MyCustomizedEncoderBean(MyCustomEncoderEnum.VAL2)
		);

		testEncode(input, MyCustomizedEncoderBean.class, step -> step
				.consumeNextWith(expectString("[{\"property\":\"Value1\"}").andThen(DataBufferUtils::release))
				.consumeNextWith(expectString(",{\"property\":\"Value2\"}").andThen(DataBufferUtils::release))
				.consumeNextWith(expectString("]").andThen(DataBufferUtils::release))
				.verifyComplete());
	}


	private static class MyCustomizedEncoderBean {

		private MyCustomEncoderEnum property;

		public MyCustomizedEncoderBean(MyCustomEncoderEnum property) {
			this.property = property;
		}

		@SuppressWarnings("unused")
		public MyCustomEncoderEnum getProperty() {
			return property;
		}

		@SuppressWarnings("unused")
		public void setProperty(MyCustomEncoderEnum property) {
			this.property = property;
		}
	}


	private enum MyCustomEncoderEnum {
		VAL1,
		VAL2;

		@Override
		public String toString() {
			return this == VAL1 ? "Value1" : "Value2";
		}
	}


	private static class JacksonJsonEncoderWithCustomization extends JacksonJsonEncoder {

		@Override
		protected ObjectWriter customizeWriter(
				ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {

			return writer.with(EnumFeature.WRITE_ENUMS_USING_TO_STRING);
		}
	}

}
