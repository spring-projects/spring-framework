/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.invocation.ResolvableMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.messaging.handler.annotation.MessagingPredicates.header;
import static org.springframework.messaging.handler.annotation.MessagingPredicates.headerPlain;

/**
 * Test fixture for {@link HeaderMethodArgumentResolver} tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class HeaderMethodArgumentResolverTests {

	private HeaderMethodArgumentResolver resolver;

	private final ResolvableMethod resolvable = ResolvableMethod.on(getClass()).named("handleMessage").build();


	@BeforeEach
	@SuppressWarnings("resource")
	public void setup() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.refresh();
		this.resolver = new HeaderMethodArgumentResolver(new DefaultConversionService(), context.getBeanFactory());
	}


	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.resolvable.annot(headerPlain()).arg())).isTrue();
		assertThat(this.resolver.supportsParameter(this.resolvable.annotNotPresent(Header.class).arg())).isFalse();
	}

	@Test
	public void resolveArgument() throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeader("param1", "foo").build();
		Object result = this.resolver.resolveArgument(this.resolvable.annot(headerPlain()).arg(), message);
		assertThat(result).isEqualTo("foo");
	}

	@Test  // SPR-11326
	public void resolveArgumentNativeHeader() throws Exception {
		TestMessageHeaderAccessor headers = new TestMessageHeaderAccessor();
		headers.setNativeHeader("param1", "foo");
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		assertThat(this.resolver.resolveArgument(this.resolvable.annot(headerPlain()).arg(), message)).isEqualTo("foo");
	}

	@Test
	public void resolveArgumentNativeHeaderAmbiguity() throws Exception {
		TestMessageHeaderAccessor headers = new TestMessageHeaderAccessor();
		headers.setHeader("param1", "foo");
		headers.setNativeHeader("param1", "native-foo");
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		assertThat(this.resolver.resolveArgument(
				this.resolvable.annot(headerPlain()).arg(), message)).isEqualTo("foo");

		assertThat(this.resolver.resolveArgument(
				this.resolvable.annot(header("nativeHeaders.param1")).arg(), message)).isEqualTo("native-foo");
	}

	@Test
	public void resolveArgumentNotFound() throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
		assertThatExceptionOfType(MessageHandlingException.class).isThrownBy(() ->
				this.resolver.resolveArgument(this.resolvable.annot(headerPlain()).arg(), message));
	}

	@Test
	public void resolveArgumentDefaultValue() throws Exception {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
		Object result = this.resolver.resolveArgument(this.resolvable.annot(header("name", "bar")).arg(), message);
		assertThat(result).isEqualTo("bar");
	}

	@Test
	public void resolveDefaultValueSystemProperty() throws Exception {
		System.setProperty("systemProperty", "sysbar");
		try {
			Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
			MethodParameter param = this.resolvable.annot(header("name", "#{systemProperties.systemProperty}")).arg();
			Object result = resolver.resolveArgument(param, message);
			assertThat(result).isEqualTo("sysbar");
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
			MethodParameter param = this.resolvable.annot(header("#{systemProperties.systemProperty}")).arg();
			Object result = resolver.resolveArgument(param, message);
			assertThat(result).isEqualTo("foo");
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveOptionalHeaderWithValue() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("foo", "bar").build();
		MethodParameter param = this.resolvable.annot(header("foo")).arg(Optional.class, String.class);
		Object result = resolver.resolveArgument(param, message);
		assertThat(result).isEqualTo(Optional.of("bar"));
	}

	@Test
	public void resolveOptionalHeaderAsEmpty() throws Exception {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		MethodParameter param = this.resolvable.annot(header("foo")).arg(Optional.class, String.class);
		Object result = resolver.resolveArgument(param, message);
		assertThat(result).isEqualTo(Optional.empty());
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	public void handleMessage(
			@Header String param1,
			@Header(name = "name", defaultValue = "bar") String param2,
			@Header(name = "name", defaultValue = "#{systemProperties.systemProperty}") String param3,
			@Header(name = "#{systemProperties.systemProperty}") String param4,
			String param5,
			@Header("foo") Optional<String> param6,
			@Header("nativeHeaders.param1") String nativeHeaderParam1) {
	}


	public static class TestMessageHeaderAccessor extends NativeMessageHeaderAccessor {

		TestMessageHeaderAccessor() {
			super((Map<String, List<String>>) null);
		}
	}

}
