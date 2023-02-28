/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jms.listener.adapter;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Integration tests for {@link MessagingMessageListenerAdapter}.
 *
 * <p>These tests are similar to those in {@link MessagingMessageListenerAdapterTests},
 * except that these tests have a different scope and do not use mocks.
 *
 * @author Sam Brannen
 * @since 5.3.26
 * @see MessagingMessageListenerAdapterTests
 */
class MessagingMessageListenerAdapterIntegrationTests {

	@ParameterizedTest
	@MethodSource("subscriptionNames")
	void defaultSubscriptionName(Method method, String subscriptionName) {
		MessagingMessageListenerAdapter messageListenerAdaptor = new MessagingMessageListenerAdapter();
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(new CustomListener(), method);
		messageListenerAdaptor.setHandlerMethod(handlerMethod);

		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer();
		assertThat(listenerContainer.getSubscriptionName()).isNull();

		listenerContainer.setMessageListener(messageListenerAdaptor);
		assertThat(listenerContainer.getSubscriptionName()).isEqualTo(subscriptionName);
	}


	private static Stream<Arguments> subscriptionNames() {
		String method1 = "toUpperCase";
		String method2 = "toUpperCase(java.lang.String)";
		String method3 = "toUpperCase(java.lang.String,int)";
		String method4 = "toUpperCase(java.lang.String[])";
		String expectedName = CustomListener.class.getName() + ".toUpperCase";
		return Stream.of(
				arguments(named(method1, findMethod()), expectedName),
				arguments(named(method2, findMethod(String.class)), expectedName),
				arguments(named(method3, findMethod(String.class, String.class)), expectedName),
				arguments(named(method4, findMethod(byte[].class)), expectedName));
	}

	private static Method findMethod(Class<?>... paramTypes) {
		return ReflectionUtils.findMethod(CustomListener.class, "toUpperCase", paramTypes);
	}


	@SuppressWarnings("unused")
	private static class CustomListener {

		// @JmsListener(...)
		String toUpperCase() {
			return "ENIGMA";
		}

		// @JmsListener(...)
		String toUpperCase(String input) {
			return "ENIGMA";
		}

		// @JmsListener(...)
		String toUpperCase(String input, String customHeader) {
			return "ENIGMA";
		}

		// @JmsListener(...)
		String toUpperCase(byte[] input) {
			return "ENIGMA";
		}

	}

}
