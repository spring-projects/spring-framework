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

import org.junit.jupiter.api.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 */
class AopUtilsTests {

	@Test
	void testPointcutCanNeverApply() {
		class TestPointcut extends StaticMethodMatcherPointcut {
			@Override
			public boolean matches(Method method, @Nullable Class<?> clazzy) {
				return false;
			}
		}

		Pointcut no = new TestPointcut();
		assertThat(AopUtils.canApply(no, Object.class)).isFalse();
	}

	@Test
	void testPointcutAlwaysApplies() {
		assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), Object.class)).isTrue();
		assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), TestBean.class)).isTrue();
	}

	@Test
	void testPointcutAppliesToOneMethodOnObject() {
		class TestPointcut extends StaticMethodMatcherPointcut {
			@Override
			public boolean matches(Method method, @Nullable Class<?> clazz) {
				return method.getName().equals("hashCode");
			}
		}

		Pointcut pc = new TestPointcut();

		// will return true if we're not proxying interfaces
		assertThat(AopUtils.canApply(pc, Object.class)).isTrue();
	}

	/**
	 * Test that when we serialize and deserialize various canonical instances
	 * of AOP classes, they return the same instance, not a new instance
	 * that's subverted the singleton construction limitation.
	 */
	@Test
	void testCanonicalFrameworkClassesStillCanonicalOnDeserialization() throws Exception {
		assertThat(SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE)).isSameAs(MethodMatcher.TRUE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(ClassFilter.TRUE)).isSameAs(ClassFilter.TRUE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcut.TRUE)).isSameAs(Pointcut.TRUE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(EmptyTargetSource.INSTANCE)).isSameAs(EmptyTargetSource.INSTANCE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcuts.SETTERS)).isSameAs(Pointcuts.SETTERS);
		assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcuts.GETTERS)).isSameAs(Pointcuts.GETTERS);
		assertThat(SerializationTestUtils.serializeAndDeserialize(ExposeInvocationInterceptor.INSTANCE)).isSameAs(ExposeInvocationInterceptor.INSTANCE);
	}

	@Test
	void testInvokeJoinpointUsingReflection() throws Throwable {
		String name = "foo";
		TestBean testBean = new TestBean(name);
		Method method = ReflectionUtils.findMethod(TestBean.class, "getName");
		Object result = AopUtils.invokeJoinpointUsingReflection(testBean, method, new Object[0]);
		assertThat(result).isEqualTo(name);
	}

	@Test  // gh-32365
	void mostSpecificMethodBetweenJdkProxyAndTarget() throws Exception {
		Class<?> proxyClass = new ProxyFactory(new WithInterface()).getProxy(getClass().getClassLoader()).getClass();
		Method specificMethod = AopUtils.getMostSpecificMethod(proxyClass.getMethod("handle", List.class), WithInterface.class);
		assertThat(ResolvableType.forMethodParameter(specificMethod, 0).getGeneric().toClass()).isEqualTo(String.class);
	}

	@Test  // gh-32365
	void mostSpecificMethodBetweenCglibProxyAndTarget() throws Exception {
		Class<?> proxyClass = new ProxyFactory(new WithoutInterface()).getProxy(getClass().getClassLoader()).getClass();
		Method specificMethod = AopUtils.getMostSpecificMethod(proxyClass.getMethod("handle", List.class), WithoutInterface.class);
		assertThat(ResolvableType.forMethodParameter(specificMethod, 0).getGeneric().toClass()).isEqualTo(String.class);
	}


	interface ProxyInterface {

		void handle(List<String> list);
	}

	static class WithInterface implements ProxyInterface {

		public void handle(List<String> list) {
		}
	}

	static class WithoutInterface {

		public void handle(List<String> list) {
		}
	}

}
