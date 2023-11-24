/*
 * Copyright 2002-2022 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.util.MimeType;

import static org.springframework.http.MediaType.APPLICATION_NDJSON;

/**
 * Unit tests for a customized {@link Jackson2JsonEncoder}.
 *
 * @author Jason Laber
 */
class CustomizedJackson2JsonEncoderTests extends AbstractEncoderTests<Jackson2JsonEncoder> {

	CustomizedJackson2JsonEncoderTests() {
		super(new Jackson2JsonEncoderWithCustomization());
	}


	@Override
	public void canEncode() throws Exception {
		// Not Testing, covered under Jackson2JsonEncoderTests
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


	private static class Jackson2JsonEncoderWithCustomization extends Jackson2JsonEncoder {

		@Override
		protected ObjectWriter customizeWriter(
				ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {

			return writer.with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		}
	}

}
