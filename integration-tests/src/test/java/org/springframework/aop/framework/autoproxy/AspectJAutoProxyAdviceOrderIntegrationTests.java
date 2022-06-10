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

package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Integration tests for advice invocation order for advice configured via
 * AspectJ auto-proxy support.
 *
 * @author Sam Brannen
 * @since 5.2.7
 * @see org.springframework.aop.config.AopNamespaceHandlerAdviceOrderIntegrationTests
 */
class AspectJAutoProxyAdviceOrderIntegrationTests {

	/**
	 * {@link After @After} advice declared as first <em>after</em> method in source code.
	 */
	@Nested
	@SpringJUnitConfig(AfterAdviceFirstConfig.class)
	@DirtiesContext
	class AfterAdviceFirstTests {

		@Test
		void afterAdviceIsInvokedLast(@Autowired Echo echo, @Autowired AfterAdviceFirstAspect aspect) throws Exception {
			assertThat(aspect.invocations).isEmpty();
			assertThat(echo.echo(42)).isEqualTo(42);
			assertThat(aspect.invocations).containsExactly("around - start", "before", "after returning", "after", "around - end");

			aspect.invocations.clear();
			assertThatException().isThrownBy(() -> echo.echo(new Exception()));
			assertThat(aspect.invocations).containsExactly("around - start", "before", "after throwing", "after", "around - end");
		}
	}


	/**
	 * This test class uses {@link AfterAdviceLastAspect} which declares its
	 * {@link After @After} advice as the last <em>after advice type</em> method
	 * in its source code.
	 *
	 * <p>On Java versions prior to JDK 7, we would have expected the {@code @After}
	 * advice method to be invoked before {@code @AfterThrowing} and
	 * {@code @AfterReturning} advice methods due to the AspectJ precedence
	 * rules implemented in
	 * {@link org.springframework.aop.aspectj.autoproxy.AspectJPrecedenceComparator}.
	 */
	@Nested
	@SpringJUnitConfig(AfterAdviceLastConfig.class)
	@DirtiesContext
	class AfterAdviceLastTests {

		@Test
		void afterAdviceIsInvokedLast(@Autowired Echo echo, @Autowired AfterAdviceLastAspect aspect) throws Exception {
			assertThat(aspect.invocations).isEmpty();
			assertThat(echo.echo(42)).isEqualTo(42);
			assertThat(aspect.invocations).containsExactly("around - start", "before", "after returning", "after", "around - end");

			aspect.invocations.clear();
			assertThatException().isThrownBy(() -> echo.echo(new Exception()));
			assertThat(aspect.invocations).containsExactly("around - start", "before", "after throwing", "after", "around - end");
		}
	}


	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class AfterAdviceFirstConfig {

		@Bean
		AfterAdviceFirstAspect echoAspect() {
			return new AfterAdviceFirstAspect();
		}

		@Bean
		Echo echo() {
			return new Echo();
		}
	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class AfterAdviceLastConfig {

		@Bean
		AfterAdviceLastAspect echoAspect() {
			return new AfterAdviceLastAspect();
		}

		@Bean
		Echo echo() {
			return new Echo();
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

	/**
	 * {@link After @After} advice declared as first <em>after</em> method in source code.
	 */
	@Aspect
	static class AfterAdviceFirstAspect {

		List<String> invocations = new ArrayList<>();

		@Pointcut("execution(* echo(*))")
		void echo() {
		}

		@After("echo()")
		void after() {
			invocations.add("after");
		}

		@AfterReturning("echo()")
		void afterReturning() {
			invocations.add("after returning");
		}

		@AfterThrowing("echo()")
		void afterThrowing() {
			invocations.add("after throwing");
		}

		@Before("echo()")
		void before() {
			invocations.add("before");
		}

		@Around("echo()")
		Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			invocations.add("around - start");
			try {
				return joinPoint.proceed();
			}
			finally {
				invocations.add("around - end");
			}
		}
	}

	/**
	 * {@link After @After} advice declared as last <em>after</em> method in source code.
	 */
	@Aspect
	static class AfterAdviceLastAspect {

		List<String> invocations = new ArrayList<>();

		@Pointcut("execution(* echo(*))")
		void echo() {
		}

		@Around("echo()")
		Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			invocations.add("around - start");
			try {
				return joinPoint.proceed();
			}
			finally {
				invocations.add("around - end");
			}
		}

		@Before("echo()")
		void before() {
			invocations.add("before");
		}

		@AfterReturning("echo()")
		void afterReturning() {
			invocations.add("after returning");
		}

		@AfterThrowing("echo()")
		void afterThrowing() {
			invocations.add("after throwing");
		}

		@After("echo()")
		void after() {
			invocations.add("after");
		}
	}

}
