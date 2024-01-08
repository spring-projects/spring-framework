/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MessageMethodArgumentResolver}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class MessageMethodArgumentResolverTests {

	private MessageConverter converter = mock();

	private MessageMethodArgumentResolver resolver = new MessageMethodArgumentResolver(this.converter);

	private Method method;


	@BeforeEach
	void setup() throws Exception {
		this.method = getClass().getDeclaredMethod("handle",
				Message.class, Message.class, Message.class, Message.class, ErrorMessage.class, Message.class);
	}


	@Test
	void resolveWithPayloadTypeAsWildcard() throws Exception {
		Message<String> message = MessageBuilder.withPayload("test").build();
		MethodParameter parameter = new MethodParameter(this.method, 0);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThat(this.resolver.resolveArgument(parameter, message)).isSameAs(message);
	}

	@Test
	void resolveWithMatchingPayloadType() throws Exception {
		Message<Integer> message = MessageBuilder.withPayload(123).build();
		MethodParameter parameter = new MethodParameter(this.method, 1);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThat(this.resolver.resolveArgument(parameter, message)).isSameAs(message);
	}

	@Test
	void resolveWithPayloadTypeSubclass() throws Exception {
		Message<Integer> message = MessageBuilder.withPayload(123).build();
		MethodParameter parameter = new MethodParameter(this.method, 2);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThat(this.resolver.resolveArgument(parameter, message)).isSameAs(message);
	}

	@Test
	void resolveWithConversion() throws Exception {
		Message<String> message = MessageBuilder.withPayload("test").build();
		MethodParameter parameter = new MethodParameter(this.method, 1);

		given(this.converter.fromMessage(message, Integer.class)).willReturn(4);

		@SuppressWarnings("unchecked")
		Message<Integer> actual = (Message<Integer>) this.resolver.resolveArgument(parameter, message);

		assertThat(actual).isNotNull();
		assertThat(actual.getHeaders()).isSameAs(message.getHeaders());
		assertThat(actual.getPayload()).isEqualTo(4);
	}

	@Test
	void resolveWithConversionNoMatchingConverter() {
		Message<String> message = MessageBuilder.withPayload("test").build();
		MethodParameter parameter = new MethodParameter(this.method, 1);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.resolver.resolveArgument(parameter, message))
			.withMessageContaining(Integer.class.getName())
			.withMessageContaining(String.class.getName());
	}

	@Test
	void resolveWithConversionEmptyPayload() {
		Message<String> message = MessageBuilder.withPayload("").build();
		MethodParameter parameter = new MethodParameter(this.method, 1);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.resolver.resolveArgument(parameter, message))
			.withMessageContaining("payload is empty")
			.withMessageContaining(Integer.class.getName())
			.withMessageContaining(String.class.getName());
	}

	@Test
	void resolveWithPayloadTypeUpperBound() throws Exception {
		Message<Integer> message = MessageBuilder.withPayload(123).build();
		MethodParameter parameter = new MethodParameter(this.method, 3);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThat(this.resolver.resolveArgument(parameter, message)).isSameAs(message);
	}

	@Test
	void resolveWithPayloadTypeOutOfBound() {
		Message<Locale> message = MessageBuilder.withPayload(Locale.getDefault()).build();
		MethodParameter parameter = new MethodParameter(this.method, 3);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.resolver.resolveArgument(parameter, message))
			.withMessageContaining(Number.class.getName())
			.withMessageContaining(Locale.class.getName());
	}

	@Test
	void resolveMessageSubclassMatch() throws Exception {
		ErrorMessage message = new ErrorMessage(new UnsupportedOperationException());
		MethodParameter parameter = new MethodParameter(this.method, 4);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThat(this.resolver.resolveArgument(parameter, message)).isSameAs(message);
	}

	@Test
	void resolveWithMessageSubclassAndPayloadWildcard() throws Exception {
		ErrorMessage message = new ErrorMessage(new UnsupportedOperationException());
		MethodParameter parameter = new MethodParameter(this.method, 0);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThat(this.resolver.resolveArgument(parameter, message)).isSameAs(message);
	}

	@Test
	void resolveWithWrongMessageType() {
		UnsupportedOperationException ex = new UnsupportedOperationException();
		Message<? extends Throwable> message = new GenericMessage<Throwable>(ex);
		MethodParameter parameter = new MethodParameter(this.method, 4);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThatExceptionOfType(MethodArgumentTypeMismatchException.class).isThrownBy(() ->
				this.resolver.resolveArgument(parameter, message))
			.withMessageContaining(ErrorMessage.class.getName())
			.withMessageContaining(GenericMessage.class.getName());
	}

	@Test
	void resolveWithPayloadTypeAsWildcardAndNoConverter() throws Exception {
		this.resolver = new MessageMethodArgumentResolver();

		Message<String> message = MessageBuilder.withPayload("test").build();
		MethodParameter parameter = new MethodParameter(this.method, 0);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThat(this.resolver.resolveArgument(parameter, message)).isSameAs(message);
	}

	@Test
	void resolveWithConversionNeededButNoConverter() {
		this.resolver = new MessageMethodArgumentResolver();

		Message<String> message = MessageBuilder.withPayload("test").build();
		MethodParameter parameter = new MethodParameter(this.method, 1);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.resolver.resolveArgument(parameter, message))
			.withMessageContaining(Integer.class.getName())
			.withMessageContaining(String.class.getName());
	}

	@Test
	void resolveWithConversionEmptyPayloadButNoConverter() {
		this.resolver = new MessageMethodArgumentResolver();

		Message<String> message = MessageBuilder.withPayload("").build();
		MethodParameter parameter = new MethodParameter(this.method, 1);

		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.resolver.resolveArgument(parameter, message))
			.withMessageContaining("payload is empty")
			.withMessageContaining(Integer.class.getName())
			.withMessageContaining(String.class.getName());
	}

	@Test // SPR-16486
	public void resolveWithJacksonConverter() throws Exception {
		Message<String> inMessage = MessageBuilder.withPayload("{\"foo\":\"bar\"}").build();
		MethodParameter parameter = new MethodParameter(this.method, 5);

		this.resolver = new MessageMethodArgumentResolver(new MappingJackson2MessageConverter());
		Object actual = this.resolver.resolveArgument(parameter, inMessage);

		boolean condition1 = actual instanceof Message;
		assertThat(condition1).isTrue();
		Message<?> outMessage = (Message<?>) actual;
		boolean condition = outMessage.getPayload() instanceof Foo;
		assertThat(condition).isTrue();
		assertThat(((Foo) outMessage.getPayload()).getFoo()).isEqualTo("bar");
	}


	@SuppressWarnings("unused")
	private void handle(
			Message<?> wildcardPayload,
			Message<Integer> integerPayload,
			Message<Number> numberPayload,
			Message<? extends Number> anyNumberPayload,
			ErrorMessage subClass,
			Message<Foo> fooPayload) {
	}


	static class Foo {

		private String foo;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

}
