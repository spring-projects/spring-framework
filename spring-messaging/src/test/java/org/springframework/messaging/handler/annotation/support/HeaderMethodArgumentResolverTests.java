/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link HeaderMethodArgumentResolver} tests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HeaderMethodArgumentResolverTests {

	private HeaderMethodArgumentResolver resolver;

	private MethodParameter paramRequired;
	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramNotAnnotated;
	private MethodParameter paramNativeHeader;


	@Before
	public void setup() throws Exception {
		@SuppressWarnings("resource")
		GenericApplicationContext cxt = new GenericApplicationContext();
		cxt.refresh();
		this.resolver = new HeaderMethodArgumentResolver(new DefaultConversionService(), cxt.getBeanFactory());

		Method method = getClass().getDeclaredMethod("handleMessage",
				String.class, String.class, String.class, String.class, String.class);
		this.paramRequired = new MethodParameter(method, 0);
		this.paramNamedDefaultValueStringHeader = new MethodParameter(method, 1);
		this.paramSystemProperty = new MethodParameter(method, 2);
		this.paramNotAnnotated = new MethodParameter(method, 3);
		this.paramNativeHeader = new MethodParameter(method, 4);

		this.paramRequired.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
		GenericTypeResolver.resolveParameterType(this.paramRequired, HeaderMethodArgumentResolver.class);
	}

	@Test
	public void supportsParameter() {
		assertTrue(resolver.supportsParameter(paramNamedDefaultValueStringHeader));
		assertFalse(resolver.supportsParameter(paramNotAnnotated));
	}

	@Test
	public void resolveArgument() throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeader("param1", "foo").build();
		Object result = this.resolver.resolveArgument(this.paramRequired, message);

		assertEquals("foo", result);
	}

	// SPR-11326

	@Test
	public void resolveArgumentNativeHeader() throws Exception {

		TestMessageHeaderAccessor headers = new TestMessageHeaderAccessor();
		headers.setNativeHeader("param1", "foo");
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		assertEquals("foo", this.resolver.resolveArgument(this.paramRequired, message));
	}

	@Test
	public void resolveArgumentNativeHeaderAmbiguity() throws Exception {

		TestMessageHeaderAccessor headers = new TestMessageHeaderAccessor();
		headers.setHeader("param1", "foo");
		headers.setNativeHeader("param1", "native-foo");
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		assertEquals("foo", this.resolver.resolveArgument(this.paramRequired, message));
		assertEquals("native-foo", this.resolver.resolveArgument(this.paramNativeHeader, message));
	}

	@Test(expected = MessageHandlingException.class)
	public void resolveArgumentNotFound() throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
		this.resolver.resolveArgument(this.paramRequired, message);
	}

	@Test
	public void resolveArgumentDefaultValue() throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
		Object result = this.resolver.resolveArgument(this.paramNamedDefaultValueStringHeader, message);

		assertEquals("bar", result);
	}

	@Test
	public void resolveDefaultValueSystemProperty() throws Exception {
		System.setProperty("systemProperty", "sysbar");
		try {
			Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
			Object result = resolver.resolveArgument(paramSystemProperty, message);
			assertEquals("sysbar", result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}


	@SuppressWarnings("unused")
	private void handleMessage(
			@Header String param1,
			@Header(value = "name", defaultValue = "bar") String param2,
			@Header(value = "name", defaultValue="#{systemProperties.systemProperty}") String param3,
			String param4,
			@Header("nativeHeaders.param1") String nativeHeaderParam1) {
	}


	public static class TestMessageHeaderAccessor extends NativeMessageHeaderAccessor {

		protected TestMessageHeaderAccessor() {
			super((Message<?>) null);
		}
	}

}
