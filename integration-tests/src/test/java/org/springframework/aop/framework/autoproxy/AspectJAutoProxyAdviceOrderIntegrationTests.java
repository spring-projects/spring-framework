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
import org.springframework.core.annotation.Order;
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
			assertThat(aspect.invocations).containsExactly("before", "around - start", "after returning", "after", "around - end");

			aspect.invocations.clear();
			assertThatException().isThrownBy(() -> echo.echo(new Exception()));
			assertThat(aspect.invocations).containsExactly("before", "around - start", "after throwing", "after", "around - end");

			aspect.invocations.clear();
			assertThat(echo.echo(42)).isEqualTo(42);
		}
	}

	/**
	 * {@link Aspect} method level {@link Order}
	 */
	@Nested
	@SpringJUnitConfig(AfterAdviceFirstAndLastConfig.class)
	@DirtiesContext
	class AfterAdviceFirstAndLastTests {

		@Test
		void afterAdviceIsInvokedLast(@Autowired Echo echo) throws Exception {
			assertThat(GLOBAL_INVOCATIONS).isEmpty();
			assertThat(echo.echo(42)).isEqualTo(42);
			assertThat(GLOBAL_INVOCATIONS).containsExactly("before2", "around1 - start1", "before1", "around2 - start2",
					"after returning2", "after2", "around2 - end2", "after returning1", "after1", "around1 - end1");

			GLOBAL_INVOCATIONS.clear();
			assertThatException().isThrownBy(() -> echo.echo(new Exception()));
			assertThat(GLOBAL_INVOCATIONS).containsExactly("before2", "around1 - start1", "before1", "around2 - start2",
					"after throwing2", "after2", "around2 - end2", "after throwing1", "after1", "around1 - end1");

			GLOBAL_INVOCATIONS.clear();
			assertThat(echo.echo(42)).isEqualTo(42);
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

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class AfterAdviceFirstAndLastConfig {

		@Bean
		AfterAdviceFirstAspect echoFirstAspect() {
			return new AfterAdviceFirstAspect();
		}

		@Bean
		AfterAdviceLastAspect echoLastAspect() {
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

	private static final List<String> GLOBAL_INVOCATIONS = new ArrayList<>(12);

	/**
	 * {@link After @After} advice declared as first <em>after</em> method in source code.
	 */
	@Aspect
	@Order(98)
	static class AfterAdviceFirstAspect {

		List<String> invocations = new ArrayList<>();

		@Pointcut("execution(* echo(*))")
		void echo() {
		}

		@After("echo()")
		void after() {
			invocations.add("after");
			GLOBAL_INVOCATIONS.add("after1");
		}

		@AfterReturning("echo()")
		void afterReturning() {
			invocations.add("after returning");
			GLOBAL_INVOCATIONS.add("after returning1");
		}

		@AfterThrowing("echo()")
		void afterThrowing() {
			invocations.add("after throwing");
			GLOBAL_INVOCATIONS.add("after throwing1");
		}

		@Before("echo()")
		void before() {
			invocations.add("before");
			GLOBAL_INVOCATIONS.add("before1");
		}

		@Around("echo()")
		Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			invocations.add("around - start");
			GLOBAL_INVOCATIONS.add("around1 - start1");
			try {
				return joinPoint.proceed();
			}
			finally {
				invocations.add("around - end");
				GLOBAL_INVOCATIONS.add("around1 - end1");
			}
		}
	}

	/**
	 * {@link After @After} advice declared as last <em>after</em> method in source code.
	 */
	@Aspect
	@Order(99)
	static class AfterAdviceLastAspect {

		List<String> invocations = new ArrayList<>();

		@Pointcut("execution(* echo(*))")
		void echo() {
		}

		@Around("echo()")
		Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			invocations.add("around - start");
			GLOBAL_INVOCATIONS.add("around2 - start2");
			try {
				return joinPoint.proceed();
			}
			finally {
				invocations.add("around - end");
				GLOBAL_INVOCATIONS.add("around2 - end2");
			}
		}

		@Order(97)
		@Before("echo()")
		void before() {
			invocations.add("before");
			GLOBAL_INVOCATIONS.add("before2");
		}

		@AfterReturning("echo()")
		void afterReturning() {
			invocations.add("after returning");
			GLOBAL_INVOCATIONS.add("after returning2");
		}

		@AfterThrowing("echo()")
		void afterThrowing() {
			invocations.add("after throwing");
			GLOBAL_INVOCATIONS.add("after throwing2");
		}

		@After("echo()")
		void after() {
			invocations.add("after");
			GLOBAL_INVOCATIONS.add("after2");
		}
	}

}
