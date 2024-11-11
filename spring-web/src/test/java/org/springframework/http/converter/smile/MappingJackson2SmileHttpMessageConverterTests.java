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

package org.springframework.http.converter.smile;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Jackson 2.x Smile converter tests.
 *
 * @author Sebastien Deleuze
 */
class MappingJackson2SmileHttpMessageConverterTests {

	private final MappingJackson2SmileHttpMessageConverter converter = new MappingJackson2SmileHttpMessageConverter();
	private final ObjectMapper mapper = new ObjectMapper(new SmileFactory());


	@Test
	void canRead() {
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "x-jackson-smile"))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json"))).isFalse();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "xml"))).isFalse();
	}

	@Test
	void canWrite() {
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "x-jackson-smile"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json"))).isFalse();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "xml"))).isFalse();
	}

	@Test
	void read() throws IOException {
		MyBean body = new MyBean();
		body.setString("Foo");
		body.setNumber(42);
		body.setFraction(42F);
		body.setArray(new String[]{"Foo", "Bar"});
		body.setBool(true);
		body.setBytes(new byte[]{0x1, 0x2});
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(mapper.writeValueAsBytes(body));
		inputMessage.getHeaders().setContentType(new MediaType("application", "x-jackson-smile"));
		MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
		assertThat(result.getString()).isEqualTo("Foo");
		assertThat(result.getNumber()).isEqualTo(42);
		assertThat(result.getFraction()).isCloseTo(42F, within(0F));

		assertThat(result.getArray()).isEqualTo(new String[]{"Foo", "Bar"});
		assertThat(result.isBool()).isTrue();
		assertThat(result.getBytes()).isEqualTo(new byte[]{0x1, 0x2});
	}

	@Test
	void write() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean body = new MyBean();
		body.setString("Foo");
		body.setNumber(42);
		body.setFraction(42F);
		body.setArray(new String[]{"Foo", "Bar"});
		body.setBool(true);
		body.setBytes(new byte[]{0x1, 0x2});
		converter.write(body, null, outputMessage);
		assertThat(outputMessage.getBodyAsBytes()).isEqualTo(mapper.writeValueAsBytes(body));
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(new MediaType("application", "x-jackson-smile"));
	}


	public static class MyBean {

		private String string;

		private int number;

		private float fraction;

		private String[] array;

		private boolean bool;

		private byte[] bytes;

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

		public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public float getFraction() {
			return fraction;
		}

		public void setFraction(float fraction) {
			this.fraction = fraction;
		}

		public String[] getArray() {
			return array;
		}

		public void setArray(String[] array) {
			this.array = array;
		}
	}

}
