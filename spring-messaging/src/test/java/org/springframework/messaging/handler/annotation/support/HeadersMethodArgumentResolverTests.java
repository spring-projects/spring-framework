/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.support;

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
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class HeadersMethodArgumentResolverTests {

	private final HeadersMethodArgumentResolver resolver = new HeadersMethodArgumentResolver();

	private Message<byte[]> message =
			MessageBuilder.withPayload(new byte[0]).copyHeaders(Collections.singletonMap("foo", "bar")).build();

	private final ResolvableMethod resolvable = ResolvableMethod.on(getClass()).named("handleMessage").build();


	@Test
	void supportsParameter() {

		assertThat(this.resolver.supportsParameter(
				this.resolvable.annotPresent(Headers.class).arg(Map.class, String.class, Object.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.resolvable.arg(MessageHeaders.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(MessageHeaderAccessor.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(TestMessageHeaderAccessor.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.resolvable.annotPresent(Headers.class).arg(String.class))).isFalse();
	}

	@Test
	void resolveArgumentAnnotated() throws Exception {
		MethodParameter param = this.resolvable.annotPresent(Headers.class).arg(Map.class, String.class, Object.class);
		Object resolved = this.resolver.resolveArgument(param, this.message);

		assertThat(resolved).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) resolved;
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	void resolveArgumentAnnotatedNotMap() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.resolveArgument(this.resolvable.annotPresent(Headers.class).arg(String.class), this.message));
	}

	@Test
	void resolveArgumentMessageHeaders() throws Exception {
		Object resolved = this.resolver.resolveArgument(this.resolvable.arg(MessageHeaders.class), this.message);

		assertThat(resolved).isInstanceOf(MessageHeaders.class);
		MessageHeaders headers = (MessageHeaders) resolved;
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	void resolveArgumentMessageHeaderAccessor() throws Exception {
		MethodParameter param = this.resolvable.arg(MessageHeaderAccessor.class);
		Object resolved = this.resolver.resolveArgument(param, this.message);

		assertThat(resolved).isInstanceOf(MessageHeaderAccessor.class);
		MessageHeaderAccessor headers = (MessageHeaderAccessor) resolved;
		assertThat(headers.getHeader("foo")).isEqualTo("bar");
	}

	@Test
	void resolveArgumentMessageHeaderAccessorSubclass() throws Exception {
		MethodParameter param = this.resolvable.arg(TestMessageHeaderAccessor.class);
		Object resolved = this.resolver.resolveArgument(param, this.message);

		assertThat(resolved).isInstanceOf(TestMessageHeaderAccessor.class);
		TestMessageHeaderAccessor headers = (TestMessageHeaderAccessor) resolved;
		assertThat(headers.getHeader("foo")).isEqualTo("bar");
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
