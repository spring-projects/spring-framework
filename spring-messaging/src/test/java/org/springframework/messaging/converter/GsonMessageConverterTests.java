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

package org.springframework.messaging.converter;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * Test fixture for {@link GsonMessageConverter}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class GsonMessageConverterTests {

	@Test
	void defaultConstructor() {
		GsonMessageConverter converter = new GsonMessageConverter();
		assertThat(converter.getSupportedMimeTypes()).contains(new MimeType("application", "json"));
	}

	@Test
	void fromMessage() {
		GsonMessageConverter converter = new GsonMessageConverter();
		String payload = "{\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		MyBean actual = (MyBean) converter.fromMessage(message, MyBean.class);

		assertThat(actual.getString()).isEqualTo("Foo");
		assertThat(actual.getNumber()).isEqualTo(42);
		assertThat(actual.getFraction()).isCloseTo(42F, within(0F));
		assertThat(actual.getArray()).isEqualTo(new String[]{"Foo", "Bar"});
		assertThat(actual.isBool()).isTrue();
	}

	@Test
	void fromMessageUntyped() {
		GsonMessageConverter converter = new GsonMessageConverter();
		String payload = "{\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		@SuppressWarnings("unchecked")
		HashMap<String, Object> actual = (HashMap<String, Object>) converter.fromMessage(message, HashMap.class);

		assertThat(actual.get("string")).isEqualTo("Foo");
		assertThat(actual.get("number")).isEqualTo(42.0);
		assertThat((Double) actual.get("fraction")).isCloseTo(42D, within(0D));
		assertThat(actual.get("array")).isEqualTo(Arrays.asList("Foo", "Bar"));
		assertThat(actual.get("bool")).isEqualTo(Boolean.TRUE);
	}

	@Test
	void fromMessageMatchingInstance() {
		MyBean myBean = new MyBean();
		GsonMessageConverter converter = new GsonMessageConverter();
		Message<?> message = MessageBuilder.withPayload(myBean).build();
		assertThat(converter.fromMessage(message, MyBean.class)).isSameAs(myBean);
	}

	@Test
	void fromMessageInvalidJson() {
		GsonMessageConverter converter = new GsonMessageConverter();
		String payload = "FooBar";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				converter.fromMessage(message, MyBean.class));
	}

	@Test
	void fromMessageValidJsonWithUnknownProperty() {
		GsonMessageConverter converter = new GsonMessageConverter();
		String payload = "{\"string\":\"string\",\"unknownProperty\":\"value\"}";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		MyBean myBean = (MyBean)converter.fromMessage(message, MyBean.class);
		assertThat(myBean.getString()).isEqualTo("string");
	}

	@Test
	void fromMessageToList() throws Exception {
		GsonMessageConverter converter = new GsonMessageConverter();
		String payload = "[1, 2, 3, 4, 5, 6, 7, 8, 9]";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();

		Method method = getClass().getDeclaredMethod("handleList", List.class);
		MethodParameter param = new MethodParameter(method, 0);
		Object actual = converter.fromMessage(message, List.class, param);

		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));
	}

	@Test
	void fromMessageToMessageWithPojo() throws Exception {
		GsonMessageConverter converter = new GsonMessageConverter();
		String payload = "{\"string\":\"foo\"}";
		Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();

		Method method = getClass().getDeclaredMethod("handleMessage", Message.class);
		MethodParameter param = new MethodParameter(method, 0);
		Object actual = converter.fromMessage(message, MyBean.class, param);

		assertThat(actual).isInstanceOf(MyBean.class);
		assertThat(((MyBean) actual).getString()).isEqualTo("foo");
	}

	@Test
	void toMessage() {
		GsonMessageConverter converter = new GsonMessageConverter();
		MyBean payload = new MyBean();
		payload.setString("Foo");
		payload.setNumber(42);
		payload.setFraction(42F);
		payload.setArray(new String[]{"Foo", "Bar"});
		payload.setBool(true);

		Message<?> message = converter.toMessage(payload, null);
		String actual = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);

		assertThat(actual).contains("\"string\":\"Foo\"");
		assertThat(actual).contains("\"number\":42");
		assertThat(actual).contains("fraction\":42.0");
		assertThat(actual).contains("\"array\":[\"Foo\",\"Bar\"]");
		assertThat(actual).contains("\"bool\":true");
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE, MimeType.class)).as("Invalid content-type").isEqualTo(new MimeType("application", "json"));
	}

	@Test
	void toMessageUtf16() {
		GsonMessageConverter converter = new GsonMessageConverter();
		MimeType contentType = new MimeType("application", "json", StandardCharsets.UTF_16BE);
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, contentType);
		MessageHeaders headers = new MessageHeaders(map);
		String payload = "H\u00e9llo W\u00f6rld";
		Message<?> message = converter.toMessage(payload, headers);

		assertThat(new String((byte[]) message.getPayload(), StandardCharsets.UTF_16BE)).isEqualTo("\"" + payload + "\"");
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(contentType);
	}

	@Test
	void toMessageUtf16String() {
		GsonMessageConverter converter = new GsonMessageConverter();
		converter.setSerializedPayloadClass(String.class);

		MimeType contentType = new MimeType("application", "json", StandardCharsets.UTF_16BE);
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, contentType);
		MessageHeaders headers = new MessageHeaders(map);
		String payload = "H\u00e9llo W\u00f6rld";
		Message<?> message = converter.toMessage(payload, headers);

		assertThat(message.getPayload()).isEqualTo("\"" + payload + "\"");
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(contentType);
	}


	void handleList(List<Long> payload) {
	}

	void handleMessage(Message<MyBean> message) {
	}


	public static class MyBean {

		private String string;

		private int number;

		private float fraction;

		private String[] array;

		private boolean bool;

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
