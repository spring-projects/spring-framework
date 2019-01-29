/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.invocation.ResolvableMethod;
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

	private final HeadersMethodArgumentResolver resolver = new HeadersMethodArgumentResolver();

	private Message<byte[]> message =
			MessageBuilder.withPayload(new byte[0]).copyHeaders(Collections.singletonMap("foo", "bar")).build();

	private final ResolvableMethod resolvable = ResolvableMethod.on(getClass()).named("handleMessage").build();


	@Test
	public void supportsParameter() {

		assertTrue(this.resolver.supportsParameter(
				this.resolvable.annotPresent(Headers.class).arg(Map.class, String.class, Object.class)));

		assertTrue(this.resolver.supportsParameter(this.resolvable.arg(MessageHeaders.class)));
		assertTrue(this.resolver.supportsParameter(this.resolvable.arg(MessageHeaderAccessor.class)));
		assertTrue(this.resolver.supportsParameter(this.resolvable.arg(TestMessageHeaderAccessor.class)));

		assertFalse(this.resolver.supportsParameter(this.resolvable.annotPresent(Headers.class).arg(String.class)));
	}

	@Test
	public void resolveArgumentAnnotated() throws Exception {
		MethodParameter param = this.resolvable.annotPresent(Headers.class).arg(Map.class, String.class, Object.class);
		Object resolved = this.resolver.resolveArgument(param, this.message);

		assertTrue(resolved instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) resolved;
		assertEquals("bar", headers.get("foo"));
	}

	@Test(expected = IllegalStateException.class)
	public void resolveArgumentAnnotatedNotMap() throws Exception {
		this.resolver.resolveArgument(this.resolvable.annotPresent(Headers.class).arg(String.class), this.message);
	}

	@Test
	public void resolveArgumentMessageHeaders() throws Exception {
		Object resolved = this.resolver.resolveArgument(this.resolvable.arg(MessageHeaders.class), this.message);

		assertTrue(resolved instanceof MessageHeaders);
		MessageHeaders headers = (MessageHeaders) resolved;
		assertEquals("bar", headers.get("foo"));
	}

	@Test
	public void resolveArgumentMessageHeaderAccessor() throws Exception {
		MethodParameter param = this.resolvable.arg(MessageHeaderAccessor.class);
		Object resolved = this.resolver.resolveArgument(param, this.message);

		assertTrue(resolved instanceof MessageHeaderAccessor);
		MessageHeaderAccessor headers = (MessageHeaderAccessor) resolved;
		assertEquals("bar", headers.getHeader("foo"));
	}

	@Test
	public void resolveArgumentMessageHeaderAccessorSubclass() throws Exception {
		MethodParameter param = this.resolvable.arg(TestMessageHeaderAccessor.class);
		Object resolved = this.resolver.resolveArgument(param, this.message);

		assertTrue(resolved instanceof TestMessageHeaderAccessor);
		TestMessageHeaderAccessor headers = (TestMessageHeaderAccessor) resolved;
		assertEquals("bar", headers.getHeader("foo"));
	}


	@SuppressWarnings("unused")
	private void handleMessage(
			@Headers Map<String, Object> param1,
			@Headers String param2,
			MessageHeaders param3,
			MessageHeaderAccessor param4,
			TestMessageHeaderAccessor param5) {
	}


	public static class TestMessageHeaderAccessor extends NativeMessageHeaderAccessor {

		TestMessageHeaderAccessor(Message<?> message) {
			super(message);
		}

		public static TestMessageHeaderAccessor wrap(Message<?> message) {
			return new TestMessageHeaderAccessor(message);
		}
	}

}
