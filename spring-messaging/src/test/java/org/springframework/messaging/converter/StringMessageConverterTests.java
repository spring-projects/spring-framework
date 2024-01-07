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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link org.springframework.messaging.converter.StringMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
class StringMessageConverterTests {

	private final StringMessageConverter converter = new StringMessageConverter();


	@Test
	void fromByteArrayMessage() {
		Message<byte[]> message = MessageBuilder.withPayload(
				"ABC".getBytes()).setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();
		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("ABC");
	}

	@Test
	void fromStringMessage() {
		Message<String> message = MessageBuilder.withPayload(
				"ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();
		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("ABC");
	}

	@Test
	void fromMessageNoContentTypeHeader() {
		Message<byte[]> message = MessageBuilder.withPayload("ABC".getBytes()).build();
		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("ABC");
	}

	@Test
	void fromMessageCharset() {
		String payload = "H\u00e9llo W\u00f6rld";
		Message<byte[]> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.ISO_8859_1))
				.setHeader(MessageHeaders.CONTENT_TYPE, new MimeType("text", "plain", StandardCharsets.ISO_8859_1)).build();
		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo(payload);
	}

	@Test
	void fromMessageDefaultCharset() {
		String payload = "H\u00e9llo W\u00f6rld";
		Message<byte[]> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo(payload);
	}

	@Test
	void fromMessageTargetClassNotSupported() {
		Message<byte[]> message = MessageBuilder.withPayload("ABC".getBytes()).build();
		assertThat(this.converter.fromMessage(message, Integer.class)).isNull();
	}

	@Test
	void fromMessageByteArray() {
		Message<byte[]> message = MessageBuilder.withPayload(
				"ABC".getBytes()).setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();
		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("ABC");
	}

	@Test
	void toMessage() {
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN);
		MessageHeaders headers = new MessageHeaders(map);
		Message<?> message = this.converter.toMessage("ABC", headers);

		assertThat(new String(((byte[]) message.getPayload()))).isEqualTo("ABC");
	}

}
