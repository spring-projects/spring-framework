/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.core;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * Tests for StandardReflectionParameterNameDiscoverer
 *
 * @author Rob Winch
 */
public class StandardReflectionParameterNameDiscoverTests {
	private ParameterNameDiscoverer parameterNameDiscoverer;

	@Before
	public void setup() {
		parameterNameDiscoverer = new StandardReflectionParameterNameDiscoverer();
	}

	@Test
	public void getParameterNamesOnInterface() {
		Method method = ReflectionUtils.findMethod(MessageService.class,"sendMessage", String.class);
		String[] actualParams = parameterNameDiscoverer.getParameterNames(method);
		assertThat(actualParams, is(new String[]{"message"}));
	}

	public interface MessageService {
		void sendMessage(String message);
	}
}
