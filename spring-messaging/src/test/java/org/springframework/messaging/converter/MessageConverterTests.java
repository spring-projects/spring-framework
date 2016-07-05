/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.converter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;

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

		assertEquals("success-from", this.converter.fromMessage(message, String.class));
		assertNull(this.converter.fromMessage(message, Integer.class));
	}

	@Test
	public void supportsMimeType() {
		Message<String> message = MessageBuilder.withPayload(
				"ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();

		assertEquals("success-from", this.converter.fromMessage(message, String.class));
	}

	@Test
	public void supportsMimeTypeNotSupported() {
		Message<String> message = MessageBuilder.withPayload(
				"ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build();

		assertNull(this.converter.fromMessage(message, String.class));
	}

	@Test
	public void supportsMimeTypeNotSpecified() {
		Message<String> message = MessageBuilder.withPayload("ABC").build();
		assertEquals("success-from", this.converter.fromMessage(message, String.class));
	}

	@Test
	public void supportsMimeTypeNoneConfigured() {
		Message<String> message = MessageBuilder.withPayload(
				"ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build();
		this.converter = new TestMessageConverter(Collections.<MimeType>emptyList());

		assertEquals("success-from", this.converter.fromMessage(message, String.class));
	}

	@Test
	public void canConvertFromStrictContentTypeMatch() {
		this.converter = new TestMessageConverter(Arrays.asList(MimeTypeUtils.TEXT_PLAIN));
		this.converter.setStrictContentTypeMatch(true);

		Message<String> message = MessageBuilder.withPayload("ABC").build();
		assertFalse(this.converter.canConvertFrom(message, String.class));

		message = MessageBuilder.withPayload("ABC")
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN).build();
		assertTrue(this.converter.canConvertFrom(message, String.class));

	}

	@Test(expected = IllegalArgumentException.class)
	public void setStrictContentTypeMatchWithNoSupportedMimeTypes() {
		this.converter = new TestMessageConverter(Collections.<MimeType>emptyList());
		this.converter.setStrictContentTypeMatch(true);
	}

	@Test
	public void toMessageWithHeaders() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("foo", "bar");
		MessageHeaders headers = new MessageHeaders(map);
		Message<?> message = this.converter.toMessage("ABC", headers);

		assertNotNull(message.getHeaders().getId());
		assertNotNull(message.getHeaders().getTimestamp());
		assertEquals(MimeTypeUtils.TEXT_PLAIN, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithMutableMessageHeaders() {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setHeader("foo", "bar");
		accessor.setNativeHeader("fooNative", "barNative");
		accessor.setLeaveMutable(true);

		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = this.converter.toMessage("ABC", headers);

		assertSame(headers, message.getHeaders());
		assertNull(message.getHeaders().getId());
		assertNull(message.getHeaders().getTimestamp());
		assertEquals(MimeTypeUtils.TEXT_PLAIN, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void toMessageContentTypeHeader() {
		Message<?> message = this.converter.toMessage("ABC", null);
		assertEquals(MimeTypeUtils.TEXT_PLAIN, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}


	private static class TestMessageConverter extends AbstractMessageConverter {

		public TestMessageConverter() {
			super(MimeTypeUtils.TEXT_PLAIN);
		}

		public TestMessageConverter(Collection<MimeType> supportedMimeTypes) {
			super(supportedMimeTypes);
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return String.class.equals(clazz);
		}

		@Override
		protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
			return "success-from";
		}

		@Override
		protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
			return "success-to";
		}
	}

}
