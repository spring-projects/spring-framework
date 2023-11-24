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

package org.springframework.aop.config;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Integration tests for advice invocation order for advice configured via the
 * AOP namespace.
 *
 * @author Sam Brannen
 * @since 5.2.7
 * @see org.springframework.aop.framework.autoproxy.AspectJAutoProxyAdviceOrderIntegrationTests
 */
class AopNamespaceHandlerAdviceOrderIntegrationTests {

	@Nested
	@SpringJUnitConfig(locations = "AopNamespaceHandlerAdviceOrderIntegrationTests-afterFirst.xml")
	@DirtiesContext
	class AfterAdviceFirstTests {

		@Test
		void afterAdviceIsInvokedFirst(@Autowired Echo echo, @Autowired InvocationTrackingAspect aspect) throws Exception {
			assertThat(aspect.invocations).isEmpty();
			assertThat(echo.echo(42)).isEqualTo(42);
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after", "after returning");

			aspect.invocations.clear();
			assertThatException().isThrownBy(() -> echo.echo(new Exception()));
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after", "after throwing");
		}
	}

	@Nested
	@SpringJUnitConfig(locations = "AopNamespaceHandlerAdviceOrderIntegrationTests-afterLast.xml")
	@DirtiesContext
	class AfterAdviceLastTests {

		@Test
		void afterAdviceIsInvokedLast(@Autowired Echo echo, @Autowired InvocationTrackingAspect aspect) throws Exception {
			assertThat(aspect.invocations).isEmpty();
			assertThat(echo.echo(42)).isEqualTo(42);
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after returning", "after");

			aspect.invocations.clear();
			assertThatException().isThrownBy(() -> echo.echo(new Exception()));
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after throwing", "after");
		}
	}


	static class Echo {

		Object echo(Object obj) throws Exception {
			if (obj instanceof Exception) {
				throw (Exception) obj;
			}
			return obj;
		}
	}

	static class InvocationTrackingAspect {

		List<String> invocations = new ArrayList<>();

		Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			invocations.add("around - start");
			try {
				return joinPoint.proceed();
			}
			finally {
				invocations.add("around - end");
			}
		}

		void before() {
			invocations.add("before");
		}

		void afterReturning() {
			invocations.add("after returning");
		}

		void afterThrowing() {
			invocations.add("after throwing");
		}

		void after() {
			invocations.add("after");
		}
	}

}
