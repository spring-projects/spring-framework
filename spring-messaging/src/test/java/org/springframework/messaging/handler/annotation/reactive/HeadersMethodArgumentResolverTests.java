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

package org.springframework.messaging.handler.annotation.reactive;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.invocation.ResolvableMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link HeadersMethodArgumentResolver} tests.
 * @author Rossen Stoyanchev
 */
public class HeadersMethodArgumentResolverTests {

	private final HeadersMethodArgumentResolver resolver = new HeadersMethodArgumentResolver();

	private Message<byte[]> message =
			MessageBuilder.withPayload(new byte[0]).copyHeaders(Collections.singletonMap("foo", "bar")).build();

	private final ResolvableMethod resolvable = ResolvableMethod.on(getClass()).named("handleMessage").build();


	@Test
	public void supportsParameter() {

		assertThat(this.resolver.supportsParameter(
				this.resolvable.annotPresent(Headers.class).arg(Map.class, String.class, Object.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.resolvable.arg(MessageHeaders.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(MessageHeaderAccessor.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(TestMessageHeaderAccessor.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.resolvable.annotPresent(Headers.class).arg(String.class))).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveArgumentAnnotated() {
		MethodParameter param = this.resolvable.annotPresent(Headers.class).arg(Map.class, String.class, Object.class);
		Map<String, Object> headers = resolveArgument(param);
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	public void resolveArgumentAnnotatedNotMap() {
		assertThatIllegalStateException().isThrownBy(() ->
				resolveArgument(this.resolvable.annotPresent(Headers.class).arg(String.class)));
	}

	@Test
	public void resolveArgumentMessageHeaders() {
		MessageHeaders headers = resolveArgument(this.resolvable.arg(MessageHeaders.class));
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	public void resolveArgumentMessageHeaderAccessor() {
		MessageHeaderAccessor headers = resolveArgument(this.resolvable.arg(MessageHeaderAccessor.class));
		assertThat(headers.getHeader("foo")).isEqualTo("bar");
	}

	@Test
	public void resolveArgumentMessageHeaderAccessorSubclass() {
		TestMessageHeaderAccessor headers = resolveArgument(this.resolvable.arg(TestMessageHeaderAccessor.class));
		assertThat(headers.getHeader("foo")).isEqualTo("bar");
	}

	@SuppressWarnings({"unchecked", "ConstantConditions"})
	private <T> T resolveArgument(MethodParameter param) {
		return (T) this.resolver.resolveArgument(param, this.message).block(Duration.ofSeconds(5));
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
