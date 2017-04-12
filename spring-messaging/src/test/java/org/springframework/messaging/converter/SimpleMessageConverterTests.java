/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.junit.Assert.*;

/**
 * Unit tests for
 * {@link org.springframework.messaging.converter.SimpleMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
public class SimpleMessageConverterTests {

	private SimpleMessageConverter converter;

	@Before
	public void setup() {
		this.converter = new SimpleMessageConverter();
	}

	@Test
	public void toMessageWithNullPayload() {
		assertNull(this.converter.toMessage(null, null));
	}

	@Test
	public void toMessageWithPayloadAndHeaders() {
		MessageHeaders headers = new MessageHeaders(Collections.<String, Object>singletonMap("foo", "bar"));
		Message<?> message = this.converter.toMessage("payload", headers);

		assertEquals("payload", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithPayloadAndMutableHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();

		Message<?> message = this.converter.toMessage("payload", headers);

		assertEquals("payload", message.getPayload());
		assertSame(headers, message.getHeaders());
		assertEquals("bar", message.getHeaders().get("foo"));
	}
}
