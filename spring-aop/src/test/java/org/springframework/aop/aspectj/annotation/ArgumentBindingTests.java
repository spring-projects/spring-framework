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

package org.springframework.aop.aspectj.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.AspectJAdviceParameterNameDiscoverer;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class ArgumentBindingTests {

	@Test
	void annotationArgumentNameBinding() {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TransactionalBean());
		proxyFactory.addAspect(PointcutWithAnnotationArgument.class);
		ITransactionalBean proxiedTestBean = proxyFactory.getProxy();

		assertThatIllegalStateException()
				.isThrownBy(proxiedTestBean::doInTransaction)
				.withMessage("Invoked with @Transactional");
	}

	@Test
	void bindingInPointcutUsedByAdvice() {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
		proxyFactory.addAspect(NamedPointcutWithArgs.class);
		ITestBean proxiedTestBean = proxyFactory.getProxy();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> proxiedTestBean.setName("enigma"))
				.withMessage("enigma");
	}

	@Test
	void bindingWithDynamicAdvice() {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
		proxyFactory.addAspect(DynamicPointcutWithArgs.class);
		ITestBean proxiedTestBean = proxyFactory.getProxy();

		proxiedTestBean.applyName(1);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> proxiedTestBean.applyName("enigma"))
				.withMessage("enigma");
	}

	@Test
	void parameterNameDiscoverWithReferencePointcut() throws Exception {
		AspectJAdviceParameterNameDiscoverer discoverer =
				new AspectJAdviceParameterNameDiscoverer("somepc(formal) && set(* *)");
		discoverer.setRaiseExceptions(true);
		Method method = getClass().getDeclaredMethod("methodWithOneParam", String.class);
		assertThat(discoverer.getParameterNames(method)).containsExactly("formal");
	}


	@SuppressWarnings("unused")
	private void methodWithOneParam(String aParam) {
	}


	interface ITransactionalBean {

		@Transactional
		void doInTransaction();
	}


	static class TransactionalBean implements ITransactionalBean {

		@Override
		@Transactional
		public void doInTransaction() {
		}
	}


	/**
	 * Mimics Spring's @Transactional annotation without actually introducing the dependency.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface Transactional {
	}


	@Aspect
	static class PointcutWithAnnotationArgument {

		@Around("execution(* org.springframework..*.*(..)) && @annotation(transactional)")
		public Object around(ProceedingJoinPoint pjp, Transactional transactional) {
			throw new IllegalStateException("Invoked with @Transactional");
		}
	}


	@Aspect
	static class NamedPointcutWithArgs {

		@Pointcut("execution(* *(..)) && args(s,..)")
		public void pointcutWithArgs(String s) {}

		@Around("pointcutWithArgs(aString)")
		public Object doAround(ProceedingJoinPoint pjp, String aString) {
			throw new IllegalArgumentException(aString);
		}
	}


	@Aspect("pertarget(execution(* *(..)))")
	static class DynamicPointcutWithArgs {

		@Around("execution(* *(..)) && args(java.lang.String)")
		public Object doAround(ProceedingJoinPoint pjp) {
			throw new IllegalArgumentException(String.valueOf(pjp.getArgs()[0]));
		}
	}

}

