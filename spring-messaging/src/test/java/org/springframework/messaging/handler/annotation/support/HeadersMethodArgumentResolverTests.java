/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link HeadersMethodArgumentResolver} tests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HeadersMethodArgumentResolverTests {

	private HeadersMethodArgumentResolver resolver;

	private MethodParameter paramAnnotated;
	private MethodParameter paramAnnotatedNotMap;
	private MethodParameter paramMessageHeaders;
	private MethodParameter paramMessageHeaderAccessor;
	private MethodParameter paramMessageHeaderAccessorSubclass;

	private Message<byte[]> message;


	@Before
	public void setup() throws Exception {
		this.resolver = new HeadersMethodArgumentResolver();

		Method method = getClass().getDeclaredMethod("handleMessage", Map.class, String.class,
				MessageHeaders.class, MessageHeaderAccessor.class, TestMessageHeaderAccessor.class);

		this.paramAnnotated = new MethodParameter(method, 0);
		this.paramAnnotatedNotMap = new MethodParameter(method, 1);
		this.paramMessageHeaders = new MethodParameter(method, 2);
		this.paramMessageHeaderAccessor = new MethodParameter(method, 3);
		this.paramMessageHeaderAccessorSubclass = new MethodParameter(method, 4);

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "bar");
		this.message = MessageBuilder.withPayload(new byte[0]).copyHeaders(headers).build();
	}

	@Test
	public void supportsParameter() {
		assertTrue(this.resolver.supportsParameter(this.paramAnnotated));
		assertFalse(this.resolver.supportsParameter(this.paramAnnotatedNotMap));
		assertTrue(this.resolver.supportsParameter(this.paramMessageHeaders));
		assertTrue(this.resolver.supportsParameter(this.paramMessageHeaderAccessor));
		assertTrue(this.resolver.supportsParameter(this.paramMessageHeaderAccessorSubclass));
	}

	@Test
	public void resolveArgumentAnnotated() throws Exception {
		Object resolved = this.resolver.resolveArgument(this.paramAnnotated, this.message);

		assertTrue(resolved instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) resolved;
		assertEquals("bar", headers.get("foo"));
	}

	@Test(expected=IllegalStateException.class)
	public void resolveArgumentAnnotatedNotMap() throws Exception {
		this.resolver.resolveArgument(this.paramAnnotatedNotMap, this.message);
	}

	@Test
	public void resolveArgumentMessageHeaders() throws Exception {
		Object resolved = this.resolver.resolveArgument(this.paramMessageHeaders, this.message);

		assertTrue(resolved instanceof MessageHeaders);
		MessageHeaders headers = (MessageHeaders) resolved;
		assertEquals("bar", headers.get("foo"));
	}

	@Test
	public void resolveArgumentMessageHeaderAccessor() throws Exception {
		Object resolved = this.resolver.resolveArgument(this.paramMessageHeaderAccessor, this.message);

		assertTrue(resolved instanceof MessageHeaderAccessor);
		MessageHeaderAccessor headers = (MessageHeaderAccessor) resolved;
		assertEquals("bar", headers.getHeader("foo"));
	}

	@Test
	public void resolveArgumentMessageHeaderAccessorSubclass() throws Exception {
		Object resolved = this.resolver.resolveArgument(this.paramMessageHeaderAccessorSubclass, this.message);

		assertTrue(resolved instanceof TestMessageHeaderAccessor);
		TestMessageHeaderAccessor headers = (TestMessageHeaderAccessor) resolved;
		assertEquals("bar", headers.getHeader("foo"));
	}


	@SuppressWarnings("unused")
	private void handleMessage(
			@Headers Map<String, ?> param1,
			@Headers String param2,
			MessageHeaders param3,
			MessageHeaderAccessor param4,
			TestMessageHeaderAccessor param5) {
	}


	public static class TestMessageHeaderAccessor extends NativeMessageHeaderAccessor {

		protected TestMessageHeaderAccessor(Message<?> message) {
			super(message);
		}

		public static TestMessageHeaderAccessor wrap(Message<?> message) {
			return new TestMessageHeaderAccessor(message);
		}
	}

}
