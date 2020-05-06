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

package org.springframework.web.servlet.mvc.method;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMethodMappingNamingStrategy}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoHandlerMethodMappingNamingStrategyTests {

	@Test
	public void getNameExplicit() {

		Method method = ClassUtils.getMethod(TestController.class, "handle");
		HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

		RequestMappingInfo rmi = new RequestMappingInfo("foo", null, null, null, null, null, null, null);

		HandlerMethodMappingNamingStrategy<RequestMappingInfo> strategy = new RequestMappingInfoHandlerMethodMappingNamingStrategy();

		assertThat(strategy.getName(handlerMethod, rmi)).isEqualTo("foo");
	}

	@Test
	public void getNameConvention() {

		Method method = ClassUtils.getMethod(TestController.class, "handle");
		HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

		RequestMappingInfo rmi = new RequestMappingInfo(null, null, null, null, null, null, null, null);

		HandlerMethodMappingNamingStrategy<RequestMappingInfo> strategy = new RequestMappingInfoHandlerMethodMappingNamingStrategy();

		assertThat(strategy.getName(handlerMethod, rmi)).isEqualTo("TC#handle");
	}


	private static class TestController {

		@RequestMapping
		public void handle() {
		}
	}

}
