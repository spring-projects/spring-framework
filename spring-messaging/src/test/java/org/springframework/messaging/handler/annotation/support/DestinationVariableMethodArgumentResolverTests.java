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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.invocation.ResolvableMethod;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.messaging.handler.annotation.MessagingPredicates.destinationVar;

/**
 * Test fixture for {@link DestinationVariableMethodArgumentResolver} tests.
 *
 * @author Brian Clozel
 */
class DestinationVariableMethodArgumentResolverTests {

	private final DestinationVariableMethodArgumentResolver resolver =
			new DestinationVariableMethodArgumentResolver(new DefaultConversionService());

	private final ResolvableMethod resolvable =
			ResolvableMethod.on(getClass()).named("handleMessage").build();


	@Test
	void supportsParameter() {
		assertThat(resolver.supportsParameter(this.resolvable.annot(destinationVar().noValue()).arg())).isTrue();
		assertThat(resolver.supportsParameter(this.resolvable.annotNotPresent(DestinationVariable.class).arg())).isFalse();
	}

	@Test
	void resolveArgument() throws Exception {

		Map<String, Object> vars = new HashMap<>();
		vars.put("foo", "bar");
		vars.put("name", "value");

		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeader(
			DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars).build();

		MethodParameter param = this.resolvable.annot(destinationVar().noValue()).arg();
		Object result = this.resolver.resolveArgument(param, message);
		assertThat(result).isEqualTo("bar");

		param = this.resolvable.annot(destinationVar("name")).arg();
		result = this.resolver.resolveArgument(param, message);
		assertThat(result).isEqualTo("value");
	}

	@Test
	void resolveArgumentNotFound() {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
		assertThatExceptionOfType(MessageHandlingException.class).isThrownBy(() ->
				this.resolver.resolveArgument(this.resolvable.annot(destinationVar().noValue()).arg(), message));
	}

	@SuppressWarnings("unused")
	private void handleMessage(
			@DestinationVariable String foo,
			@DestinationVariable(value = "name") String param1,
			String param3) {
	}

}
