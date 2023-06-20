/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for
 * {@link org.springframework.messaging.converter.AbstractMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
public class MessageConverterTests {

	private TestMessageConverter converter = new TestMessageConverter();


	@Test
	public void supportsTargetClass() {
		Message<String> message = MessageBuilder.withPayload("ABC").build();

		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
		assertThat(this.converter.fromMessage(message, Integer.class)).isNull();
	}

	@Test
	public void supportsMimeType() {
		Message<String> message = MessageBuilder.withPayload(
				"ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();

		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
	}

	@Test
	public void supportsMimeTypeNotSupported() {
		Message<String> message = MessageBuilder.withPayload(
				"ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build();

		assertThat(this.converter.fromMessage(message, String.class)).isNull();
	}

	@Test
	public void supportsMimeTypeNotSpecified() {
		Message<String> message = MessageBuilder.withPayload("ABC").build();
		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
	}

	@Test
	public void supportsMimeTypeNoneConfigured() {
		Message<String> message = MessageBuilder.withPayload(
				"ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build();
		this.converter = new TestMessageConverter(new MimeType[0]);

		assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
	}

	@Test
	public void canConvertFromStrictContentTypeMatch() {
		this.converter = new TestMessageConverter(MimeTypeUtils.TEXT_PLAIN);
		this.converter.setStrictContentTypeMatch(true);

		Message<String> message = MessageBuilder.withPayload("ABC").build();
		assertThat(this.converter.canConvertFrom(message, String.class)).isFalse();

		message = MessageBuilder.withPayload("ABC")
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();
		assertThat(this.converter.canConvertFrom(message, String.class)).isTrue();

	}

	@Test
	public void setStrictContentTypeMatchWithNoSupportedMimeTypes() {
		this.converter = new TestMessageConverter(new MimeType[0]);
		assertThatIllegalArgumentException().isThrownBy(() -> this.converter.setStrictContentTypeMatch(true));
	}

	@Test
	public void toMessageWithHeaders() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		MessageHeaders headers = new MessageHeaders(map);
		Message<?> message = this.converter.toMessage("ABC", headers);

		assertThat(message.getHeaders().getId()).isNotNull();
		assertThat(message.getHeaders().getTimestamp()).isNotNull();
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.TEXT_PLAIN);
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void toMessageWithMutableMessageHeaders() {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setHeader("foo", "bar");
		accessor.setNativeHeader("fooNative", "barNative");
		accessor.setLeaveMutable(true);

		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = this.converter.toMessage("ABC", headers);

		assertThat(message.getHeaders()).isSameAs(headers);
		assertThat(message.getHeaders().getId()).isNull();
		assertThat(message.getHeaders().getTimestamp()).isNull();
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.TEXT_PLAIN);
	}

	@Test
	public void toMessageContentTypeHeader() {
		Message<?> message = this.converter.toMessage("ABC", null);
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.TEXT_PLAIN);
	}

	@Test // gh-29768
	public void toMessageDefaultContentType() {
		DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
		resolver.setDefaultMimeType(MimeTypeUtils.TEXT_PLAIN);

		TestMessageConverter converter = new TestMessageConverter();
		converter.setContentTypeResolver(resolver);
		converter.setStrictContentTypeMatch(true);

		Message<?> message = converter.toMessage("ABC", null);
		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.TEXT_PLAIN);
	}


	private static class TestMessageConverter extends AbstractMessageConverter {

		public TestMessageConverter() {
			super(MimeTypeUtils.TEXT_PLAIN);
		}

		public TestMessageConverter(MimeType... supportedMimeTypes) {
			super(supportedMimeTypes);
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return String.class.equals(clazz);
		}

		@Override
		protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object hint) {
			return "success-from";
		}

		@Override
		protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object hint) {
			return "success-to";
		}
	}

}
