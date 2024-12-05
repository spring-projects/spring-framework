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

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.RootClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DefaultAdvisorAutoProxyCreator}.
 *
 * @author Sam Brannen
 * @since 6.2.1
 */
class DefaultAdvisorAutoProxyCreatorTests {

	/**
	 * Indirectly tests behavior of {@link org.springframework.aop.framework.AdvisedSupport.MethodCacheKey}.
	 * @see StaticMethodMatcherPointcut#matches(Method, Class)
	 */
	@Test  // gh-33915
	void staticMethodMatcherPointcutMatchesMethodIsNotInvokedAgainForActualMethodInvocation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DemoBean.class, DemoPointcutAdvisor.class, DefaultAdvisorAutoProxyCreator.class);
		DemoPointcutAdvisor demoPointcutAdvisor = context.getBean(DemoPointcutAdvisor.class);
		DemoBean demoBean = context.getBean(DemoBean.class);

		assertThat(demoPointcutAdvisor.matchesInvocationCount).as("matches() invocations before").isEqualTo(2);
		// Invoke multiple times to ensure additional invocations don't affect the outcome.
		assertThat(demoBean.sayHello()).isEqualTo("Advised: Hello!");
		assertThat(demoBean.sayHello()).isEqualTo("Advised: Hello!");
		assertThat(demoBean.sayHello()).isEqualTo("Advised: Hello!");
		assertThat(demoPointcutAdvisor.matchesInvocationCount).as("matches() invocations after").isEqualTo(2);

		context.close();
	}


	static class DemoBean {

		public String sayHello() {
			return "Hello!";
		}
	}

	@SuppressWarnings("serial")
	static class DemoPointcutAdvisor extends AbstractPointcutAdvisor {

		int matchesInvocationCount = 0;

		@Override
		public Pointcut getPointcut() {
			StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {

				@Override
				public boolean matches(Method method, Class<?> targetClass) {
					if (method.getName().equals("sayHello")) {
						matchesInvocationCount++;
						return true;
					}
					return false;
				}
			};
			pointcut.setClassFilter(new RootClassFilter(DemoBean.class));
			return pointcut;
		}

		@Override
		public Advice getAdvice() {
			return (MethodInterceptor) invocation -> "Advised: " + invocation.proceed();
		}
	}

}
