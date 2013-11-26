/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.support.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link MappingJackson2MessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
public class MappingJackson2MessageConverterTests {

	private static Charset UTF_8 = Charset.forName("UTF-8");

	private MappingJackson2MessageConverter converter;


	@Before
	public void setup() {
		this.converter = new MappingJackson2MessageConverter();
		this.converter.setContentTypeResolver(new DefaultContentTypeResolver());
	}

	@Test
	public void fromMessage() throws Exception {
		String payload = "{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],"
				+ "\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(UTF_8)).build();
		MyBean actual = (MyBean) this.converter.fromMessage(message, MyBean.class);

		assertEquals("Foo", actual.getString());
		assertEquals(42, actual.getNumber());
		assertEquals(42F, actual.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, actual.getArray());
		assertTrue(actual.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, actual.getBytes());
	}

	@Test
	public void fromMessageUntyped() throws Exception {
		String payload = "{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],"
				+ "\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(UTF_8)).build();
		@SuppressWarnings("unchecked")
		HashMap<String, Object> actual = (HashMap<String, Object>) this.converter.fromMessage(message, HashMap.class);

		assertEquals("Foo", actual.get("string"));
		assertEquals(42, actual.get("number"));
		assertEquals(42D, (Double) actual.get("fraction"), 0D);
		assertEquals(Arrays.asList("Foo", "Bar"), actual.get("array"));
		assertEquals(Boolean.TRUE, actual.get("bool"));
		assertEquals("AQI=", actual.get("bytes"));
	}

	@Test(expected = MessageConversionException.class)
	public void fromMessageInvalidJson() throws Exception {
		String payload = "FooBar";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(UTF_8)).build();
		this.converter.fromMessage(message, MyBean.class);
	}

	@Test(expected = MessageConversionException.class)
	public void fromMessageValidJsonWithUnknownProperty() throws IOException {
		String payload = "{\"string\":\"string\",\"unknownProperty\":\"value\"}";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(UTF_8)).build();
		this.converter.fromMessage(message, MyBean.class);
	}

	@Test
	public void toMessage() throws Exception {
		MyBean payload = new MyBean();
		payload.setString("Foo");
		payload.setNumber(42);
		payload.setFraction(42F);
		payload.setArray(new String[]{"Foo", "Bar"});
		payload.setBool(true);
		payload.setBytes(new byte[]{0x1, 0x2});

		Message<?> message = this.converter.toMessage(payload, null);
		String actual = new String((byte[]) message.getPayload(), UTF_8);

		assertTrue(actual.contains("\"string\":\"Foo\""));
		assertTrue(actual.contains("\"number\":42"));
		assertTrue(actual.contains("fraction\":42.0"));
		assertTrue(actual.contains("\"array\":[\"Foo\",\"Bar\"]"));
		assertTrue(actual.contains("\"bool\":true"));
		assertTrue(actual.contains("\"bytes\":\"AQI=\""));
		assertEquals("Invalid content-type", new MimeType("application", "json", UTF_8),
				message.getHeaders().get(MessageHeaders.CONTENT_TYPE, MimeType.class));
	}

	@Test
	public void toMessageUtf16() {
		Charset utf16 = Charset.forName("UTF-16BE");
		MimeType contentType = new MimeType("application", "json", utf16);
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, contentType);
		MessageHeaders headers = new MessageHeaders(map);
		String payload = "H\u00e9llo W\u00f6rld";
		Message<?> message = this.converter.toMessage(payload, headers);

		assertEquals("\"" + payload + "\"", new String((byte[]) message.getPayload(), utf16));
		assertEquals(contentType, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void toMessageUtf16String() {
		this.converter.setSerializedPayloadClass(String.class);

		Charset utf16 = Charset.forName("UTF-16BE");
		MimeType contentType = new MimeType("application", "json", utf16);
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, contentType);
		MessageHeaders headers = new MessageHeaders(map);
		String payload = "H\u00e9llo W\u00f6rld";
		Message<?> message = this.converter.toMessage(payload, headers);

		assertEquals("\"" + payload + "\"", message.getPayload());
		assertEquals(contentType, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
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
