/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StandardReflectionParameterNameDiscoverer
 *
 * @author Rob Winch
 */
class StandardReflectionParameterNameDiscoverTests {

	private ParameterNameDiscoverer parameterNameDiscoverer;

	@BeforeEach
	void setup() {
		parameterNameDiscoverer = new StandardReflectionParameterNameDiscoverer();
	}

	@Test
	void getParameterNamesOnInterface() {
		Method method = ReflectionUtils.findMethod(MessageService.class,"sendMessage", String.class);
		String[] actualParams = parameterNameDiscoverer.getParameterNames(method);
		assertThat(actualParams).isEqualTo(new String[]{"message"});
	}

	public interface MessageService {
		void sendMessage(String message);
	}

}
