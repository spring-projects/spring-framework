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

package org.springframework.context.annotation;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import example.scannable.FooService;
import example.scannable.ServiceInvocationCounter;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
class SimpleConfigTests {

	@Test
	void testFooService() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getConfigLocations(), getClass());

		FooService fooService = ctx.getBean("fooServiceImpl", FooService.class);
		ServiceInvocationCounter serviceInvocationCounter = ctx.getBean("serviceInvocationCounter", ServiceInvocationCounter.class);

		String value = fooService.foo(1);
		assertThat(value).isEqualTo("bar");

		Future<?> future = fooService.asyncFoo(1);
		boolean condition = future instanceof FutureTask;
		assertThat(condition).isTrue();
		assertThat(future.get()).isEqualTo("bar");

		assertThat(serviceInvocationCounter.getCount()).isEqualTo(2);

		fooService.foo(1);
		assertThat(serviceInvocationCounter.getCount()).isEqualTo(3);
	}

	public String[] getConfigLocations() {
		return new String[] {"simpleConfigTests.xml"};
	}

}
