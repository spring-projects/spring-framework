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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.ReflectionUtils;

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
	private MethodParameter paramSystemPropertyDefaultValue;
	private MethodParameter paramSystemPropertyName;
	private MethodParameter paramNotAnnotated;
	private MethodParameter paramNativeHeader;


	@Before
	public void setup() throws Exception {
		@SuppressWarnings("resource")
		GenericApplicationContext cxt = new GenericApplicationContext();
		cxt.refresh();
		this.resolver = new HeaderMethodArgumentResolver(new DefaultConversionService(), cxt.getBeanFactory());

		Method method = ReflectionUtils.findMethod(getClass(), "handleMessage", (Class<?>[]) null);
		this.paramRequired = new SynthesizingMethodParameter(method, 0);
		this.paramNamedDefaultValueStringHeader = new SynthesizingMethodParameter(method, 1);
		this.paramSystemPropertyDefaultValue = new SynthesizingMethodParameter(method, 2);
		this.paramSystemPropertyName = new SynthesizingMethodParameter(method, 3);
		this.paramNotAnnotated = new SynthesizingMethodParameter(method, 4);
		this.paramNativeHeader = new SynthesizingMethodParameter(method, 5);

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

	@Test  // SPR-11326
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
			Object result = resolver.resolveArgument(paramSystemPropertyDefaultValue, message);
			assertEquals("sysbar", result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemProperty() throws Exception {
		System.setProperty("systemProperty", "sysbar");
		try {
			Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeader("sysbar", "foo").build();
			Object result = resolver.resolveArgument(paramSystemPropertyName, message);
			assertEquals("foo", result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}


	public void handleMessage(
			@Header String param1,
			@Header(name = "name", defaultValue = "bar") String param2,
			@Header(name = "name", defaultValue = "#{systemProperties.systemProperty}") String param3,
			@Header(name = "#{systemProperties.systemProperty}") String param4,
			String param5,
			@Header("nativeHeaders.param1") String nativeHeaderParam1) {
	}


	public static class TestMessageHeaderAccessor extends NativeMessageHeaderAccessor {

		protected TestMessageHeaderAccessor() {
			super((Map<String, List<String>>) null);
		}
	}

}
