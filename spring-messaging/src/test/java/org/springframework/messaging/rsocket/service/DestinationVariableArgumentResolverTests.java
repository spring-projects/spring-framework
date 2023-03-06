/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.rsocket.service;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.handler.annotation.DestinationVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DestinationVariableArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class DestinationVariableArgumentResolverTests extends RSocketServiceArgumentResolverTestSupport {

	@Override
	protected RSocketServiceArgumentResolver initResolver() {
		return new DestinationVariableArgumentResolver();
	}

	@Test
	void variable() {
		String value = "foo";
		boolean resolved = execute(value, initMethodParameter(Service.class, "execute", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getRouteVariables()).containsExactly(value);
	}

	@Test
	void variableList() {
		List<String> values = Arrays.asList("foo", "bar", "baz");
		boolean resolved = execute(values, initMethodParameter(Service.class, "execute", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getRouteVariables()).containsExactlyElementsOf(values);
	}

	@Test
	void variableArray() {
		String[] values = new String[] {"foo", "bar", "baz"};
		boolean resolved = execute(values, initMethodParameter(Service.class, "execute", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getRouteVariables()).containsExactlyElementsOf(Arrays.asList(values));
	}

	@Test
	void notRequestBody() {
		MethodParameter parameter = initMethodParameter(Service.class, "executeNotAnnotated", 0);
		boolean resolved = execute("value", parameter);

		assertThat(resolved).isFalse();
	}

	@Test
	void ignoreNull() {
		boolean resolved = execute(null, initMethodParameter(Service.class, "execute", 0));

		assertThat(resolved).isTrue();
		assertThat(getRequestValues().getPayloadValue()).isNull();
		assertThat(getRequestValues().getPayload()).isNull();
		assertThat(getRequestValues().getPayloadElementType()).isNull();
	}


	@SuppressWarnings("unused")
	private interface Service {

		void execute(@DestinationVariable String variable);

		void executeList(@DestinationVariable List<String> variables);

		void executeArray(@DestinationVariable String[] variables);

		void executeNotAnnotated(String variable);

	}

}
