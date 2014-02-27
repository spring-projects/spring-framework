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

package org.springframework.messaging.converter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test fixture for {@link org.springframework.messaging.converter.AbstractMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
public class AbstractMessageConverterTests {

	private TestMessageConverter converter;


	@Before
	public void setup() {
		this.converter = new TestMessageConverter();
		this.converter.setContentTypeResolver(new DefaultContentTypeResolver());
	}

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
		this.converter.setContentTypeResolver(new DefaultContentTypeResolver());

		assertEquals("success-from", this.converter.fromMessage(message, String.class));
	}

	@Test
	public void toMessageHeadersCopied() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("foo", "bar");
		MessageHeaders headers = new MessageHeaders(map );
		Message<?> message = this.converter.toMessage("ABC", headers);

		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageContentTypeHeader() {
		Message<?> message = this.converter.toMessage("ABC", null);
		assertEquals(MimeTypeUtils.TEXT_PLAIN, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void noContentTypeResolverWhenTypeResolutionRequired() {
		Message<String> message = MessageBuilder.withPayload("ABC").build();
		this.converter = new TestMessageConverter();
		this.converter.setContentTypeResolutionRequired(true);
		assertNull(this.converter.fromMessage(message, String.class));
	}

	@Test
	public void noMimeTypeDefinedWhenTypeResolutionRequired() {
		Message<String> message = MessageBuilder.withPayload("ABC").build();
		this.converter = new TestMessageConverter(Collections.<MimeType>emptyList());
		this.converter.setContentTypeResolver(new DefaultContentTypeResolver());
		this.converter.setContentTypeResolutionRequired(true);
		assertNull(this.converter.fromMessage(message, String.class));
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
		public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
			return "success-from";
		}

		@Override
		public Object convertToInternal(Object payload, MessageHeaders headers) {
			return "success-to";
		}
	}

}
