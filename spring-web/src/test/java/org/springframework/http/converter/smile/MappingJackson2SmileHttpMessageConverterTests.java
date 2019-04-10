/*
 * Copyright 2002-2016 the original author or authors.
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
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

/**
 * Jackson 2.x Smile converter tests.
 *
 * @author Sebastien Deleuze
 */
public class MappingJackson2SmileHttpMessageConverterTests {

	private final MappingJackson2SmileHttpMessageConverter converter = new MappingJackson2SmileHttpMessageConverter();
	private final ObjectMapper mapper = new ObjectMapper(new SmileFactory());

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void canRead() {
		assertTrue(converter.canRead(MyBean.class, new MediaType("application", "x-jackson-smile")));
		assertFalse(converter.canRead(MyBean.class, new MediaType("application", "json")));
		assertFalse(converter.canRead(MyBean.class, new MediaType("application", "xml")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(MyBean.class, new MediaType("application", "x-jackson-smile")));
		assertFalse(converter.canWrite(MyBean.class, new MediaType("application", "json")));
		assertFalse(converter.canWrite(MyBean.class, new MediaType("application", "xml")));
	}

	@Test
	public void read() throws IOException {
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
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}

	@Test
	public void write() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean body = new MyBean();
		body.setString("Foo");
		body.setNumber(42);
		body.setFraction(42F);
		body.setArray(new String[]{"Foo", "Bar"});
		body.setBool(true);
		body.setBytes(new byte[]{0x1, 0x2});
		converter.write(body, null, outputMessage);
		assertArrayEquals(mapper.writeValueAsBytes(body), outputMessage.getBodyAsBytes());
		assertEquals("Invalid content-type", new MediaType("application", "x-jackson-smile", StandardCharsets.UTF_8),
				outputMessage.getHeaders().getContentType());
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
