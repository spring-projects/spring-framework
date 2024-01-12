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

package org.springframework.aop.support;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ControlFlowPointcut}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class ControlFlowPointcutTests {

	@Test
	void matchesExactMethodName() {
		MyComponent component = new MyComponent();
		TestBean target = new TestBean("Jane", 27);
		ControlFlowPointcut cflow = pointcut("getAge");
		NopInterceptor nop = new NopInterceptor();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));
		ITestBean proxy = (ITestBean) pf.getProxy();

		// Will not be advised: not under MyComponent
		assertThat(proxy.getAge()).isEqualTo(target.getAge());
		assertThat(cflow.getEvaluations()).isEqualTo(1);
		assertThat(nop.getCount()).isEqualTo(0);

		// Will be advised due to "getAge" pattern: the proxy is invoked under MyComponent#getAge
		assertThat(component.getAge(proxy)).isEqualTo(target.getAge());
		assertThat(cflow.getEvaluations()).isEqualTo(2);
		assertThat(nop.getCount()).isEqualTo(1);

		// Will not be advised: the proxy is invoked under MyComponent, but there is no match for "nomatch"
		assertThat(component.nomatch(proxy)).isEqualTo(target.getAge());
		assertThat(cflow.getEvaluations()).isEqualTo(3);
		assertThat(nop.getCount()).isEqualTo(1);
	}

	@Test
	void matchesMethodNamePatterns() {
		ControlFlowPointcut cflow = pointcut("set", "getAge");
		assertMatchesSetAndGetAge(cflow);

		cflow = pointcut("foo", "get*", "bar", "*se*", "baz");
		assertMatchesSetAndGetAge(cflow);
	}

	@Test
	void regExControlFlowPointcut() {
		ControlFlowPointcut cflow = new RegExControlFlowPointcut(MyComponent.class, "(set.*?|getAge)");
		assertMatchesSetAndGetAge(cflow);

		cflow = new RegExControlFlowPointcut(MyComponent.class, "set", "^getAge$");
		assertMatchesSetAndGetAge(cflow);
	}

	@Test
	void controlFlowPointcutIsExtensible() {
		CustomControlFlowPointcut cflow = new CustomControlFlowPointcut(MyComponent.class, "set*", "getAge", "set*", "set*");
		assertMatchesSetAndGetAge(cflow, 2);
		assertThat(cflow.trackedClass()).isEqualTo(MyComponent.class);
		assertThat(cflow.trackedMethodNamePatterns()).containsExactly("set*", "getAge");
	}

	/**
	 * Check that we can use a cflow pointcut in conjunction with
	 * a static pointcut: e.g. all setter methods that are invoked under
	 * a particular class. This greatly reduces the number of calls
	 * to the cflow pointcut, meaning that it's not so prohibitively
	 * expensive.
	 */
	@Test
	void controlFlowPointcutCanBeCombinedWithStaticPointcut() {
		MyComponent component = new MyComponent();
		TestBean target = new TestBean("Jane", 27);
		ControlFlowPointcut cflow = pointcut();
		Pointcut settersUnderMyComponent = Pointcuts.intersection(Pointcuts.SETTERS, cflow);
		NopInterceptor nop = new NopInterceptor();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(new DefaultPointcutAdvisor(settersUnderMyComponent, nop));
		ITestBean proxy = (ITestBean) pf.getProxy();

		// Will not be advised: not under MyComponent
		target.setAge(16);
		assertThat(cflow.getEvaluations()).isEqualTo(0);
		assertThat(nop.getCount()).isEqualTo(0);

		// Will not be advised: under MyComponent but not a setter
		assertThat(component.getAge(proxy)).isEqualTo(16);
		assertThat(cflow.getEvaluations()).isEqualTo(0);
		assertThat(nop.getCount()).isEqualTo(0);

		// Will be advised due to Pointcuts.SETTERS: the proxy is invoked under MyComponent#set
		component.set(proxy);
		assertThat(proxy.getAge()).isEqualTo(5);
		assertThat(nop.getCount()).isEqualTo(1);

		// We saved most evaluations
		assertThat(cflow.getEvaluations()).isEqualTo(1);
	}

	@Test
	void equalsAndHashCode() {
		assertThat(pointcut()).isEqualTo(pointcut());
		assertThat(pointcut()).hasSameHashCodeAs(pointcut());

		assertThat(pointcut("getAge")).isEqualTo(pointcut("getAge"));
		assertThat(pointcut("getAge")).hasSameHashCodeAs(pointcut("getAge"));

		assertThat(pointcut("getAge")).isNotEqualTo(pointcut());
		assertThat(pointcut("getAge")).doesNotHaveSameHashCodeAs(pointcut());

		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut("get*", "set*"));
		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut("get*", "set*", "set*", "get*"));
		assertThat(pointcut("get*", "set*")).hasSameHashCodeAs(pointcut("get*", "get*", "set*"));

		assertThat(pointcut("get*", "set*")).isNotEqualTo(pointcut("set*", "get*"));
		assertThat(pointcut("get*", "set*")).doesNotHaveSameHashCodeAs(pointcut("set*", "get*"));

		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut(List.of("get*", "set*")));
		assertThat(pointcut("get*", "set*")).isEqualTo(pointcut(List.of("get*", "set*", "set*", "get*")));
		assertThat(pointcut("get*", "set*")).hasSameHashCodeAs(pointcut(List.of("get*", "get*", "set*")));
	}

	@Test
	void testToString() {
		String pointcutType = ControlFlowPointcut.class.getName();
		String componentType = MyComponent.class.getName();

		assertThat(pointcut()).asString()
				.startsWith(pointcutType)
				.contains(componentType)
				.endsWith("[]");

		assertThat(pointcut("getAge")).asString()
				.startsWith(pointcutType)
				.contains(componentType)
				.endsWith("[getAge]");

		assertThat(pointcut("get*", "set*", "get*")).asString()
				.startsWith(pointcutType)
				.contains(componentType)
				.endsWith("[get*, set*]");
	}


	private static ControlFlowPointcut pointcut() {
		return new ControlFlowPointcut(MyComponent.class);
	}

	private static ControlFlowPointcut pointcut(String methodNamePattern) {
		return new ControlFlowPointcut(MyComponent.class, methodNamePattern);
	}

	private static ControlFlowPointcut pointcut(String... methodNamePatterns) {
		return new ControlFlowPointcut(MyComponent.class, methodNamePatterns);
	}

	private static ControlFlowPointcut pointcut(List<String> methodNamePatterns) {
		return new ControlFlowPointcut(MyComponent.class, methodNamePatterns);
	}

	private static void assertMatchesSetAndGetAge(ControlFlowPointcut cflow) {
		assertMatchesSetAndGetAge(cflow, 1);
	}

	private static void assertMatchesSetAndGetAge(ControlFlowPointcut cflow, int evaluationFactor) {
		MyComponent component = new MyComponent();
		TestBean target = new TestBean("Jane", 27);
		NopInterceptor nop = new NopInterceptor();
		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvisor(new DefaultPointcutAdvisor(cflow, nop));
		ITestBean proxy = (ITestBean) pf.getProxy();

		// Will not be advised: not under MyComponent
		assertThat(proxy.getAge()).isEqualTo(target.getAge());
		assertThat(cflow.getEvaluations()).isEqualTo(evaluationFactor);
		assertThat(nop.getCount()).isEqualTo(0);

		// Will be advised: the proxy is invoked under MyComponent#getAge
		assertThat(component.getAge(proxy)).isEqualTo(target.getAge());
		assertThat(cflow.getEvaluations()).isEqualTo(2 * evaluationFactor);
		assertThat(nop.getCount()).isEqualTo(1);

		// Will be advised: the proxy is invoked under MyComponent#set
		component.set(proxy);
		assertThat(cflow.getEvaluations()).isEqualTo(3 * evaluationFactor);
		assertThat(proxy.getAge()).isEqualTo(5);
		assertThat(cflow.getEvaluations()).isEqualTo(4 * evaluationFactor);
		assertThat(nop.getCount()).isEqualTo(2);

		// Will not be advised: the proxy is invoked under MyComponent, but there is no match for "nomatch"
		assertThat(component.nomatch(proxy)).isEqualTo(target.getAge());
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(cflow.getEvaluations()).isEqualTo(5 * evaluationFactor);
	}


	private static class MyComponent {
		int getAge(ITestBean proxy) {
			return proxy.getAge();
		}
		int nomatch(ITestBean proxy) {
			return proxy.getAge();
		}
		void set(ITestBean proxy) {
			proxy.setAge(5);
		}
	}

	@SuppressWarnings("serial")
	private static class CustomControlFlowPointcut extends ControlFlowPointcut {

		CustomControlFlowPointcut(Class<?> clazz, String... methodNamePatterns) {
			super(clazz, methodNamePatterns);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			super.incrementEvaluationCount();
			return super.matches(method, targetClass, args);
		}

		Class<?> trackedClass() {
			return super.clazz;
		}

		List<String> trackedMethodNamePatterns() {
			return super.methodNamePatterns;
		}
	}

	@SuppressWarnings("serial")
	private static class RegExControlFlowPointcut extends ControlFlowPointcut {

		private final List<Pattern> compiledPatterns;

		RegExControlFlowPointcut(Class<?> clazz, String... methodNamePatterns) {
			super(clazz, methodNamePatterns);
			this.compiledPatterns = super.methodNamePatterns.stream().map(Pattern::compile).toList();
		}

		@Override
		protected boolean isMatch(String methodName, int patternIndex) {
			return this.compiledPatterns.get(patternIndex).matcher(methodName).matches();
		}
	}

}
