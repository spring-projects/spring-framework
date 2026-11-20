/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import java.lang.reflect.Method;
import java.util.List;

import org.jspecify.annotations.Nullable;
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
	void pointcutCanNeverApply() {
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
	void pointcutAlwaysApplies() {
		assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), Object.class)).isTrue();
		assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), TestBean.class)).isTrue();
	}

	@Test
	void pointcutAppliesToOneMethodOnObject() {
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
	void canonicalFrameworkClassesStillCanonicalOnDeserialization() throws Exception {
		assertThat(SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE)).isSameAs(MethodMatcher.TRUE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(ClassFilter.TRUE)).isSameAs(ClassFilter.TRUE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcut.TRUE)).isSameAs(Pointcut.TRUE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(EmptyTargetSource.INSTANCE)).isSameAs(EmptyTargetSource.INSTANCE);
		assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcuts.SETTERS)).isSameAs(Pointcuts.SETTERS);
		assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcuts.GETTERS)).isSameAs(Pointcuts.GETTERS);
		assertThat(SerializationTestUtils.serializeAndDeserialize(ExposeInvocationInterceptor.INSTANCE)).isSameAs(ExposeInvocationInterceptor.INSTANCE);
	}

	@Test
	void invokeJoinpointUsingReflection() throws Throwable {
		String name = "foo";
		TestBean testBean = new TestBean(name);
		Method method = ReflectionUtils.findMethod(TestBean.class, "getName");
		Object result = AopUtils.invokeJoinpointUsingReflection(testBean, method, new Object[0]);
		assertThat(result).isEqualTo(name);
	}

	@Test  // gh-32365
	void mostSpecificMethodBetweenJdkProxyAndTarget() throws Exception {
		Class<?> proxyClass = new ProxyFactory(new WithInterface()).getProxyClass(getClass().getClassLoader());
		Method specificMethod = AopUtils.getMostSpecificMethod(proxyClass.getMethod("handle", List.class), WithInterface.class);
		assertThat(ResolvableType.forMethodParameter(specificMethod, 0).getGeneric().toClass()).isEqualTo(String.class);
	}

	@Test  // gh-32365
	void mostSpecificMethodBetweenCglibProxyAndTarget() throws Exception {
		Class<?> proxyClass = new ProxyFactory(new WithoutInterface()).getProxyClass(getClass().getClassLoader());
		Method specificMethod = AopUtils.getMostSpecificMethod(proxyClass.getMethod("handle", List.class), WithoutInterface.class);
		assertThat(ResolvableType.forMethodParameter(specificMethod, 0).getGeneric().toClass()).isEqualTo(String.class);
	}


	interface ProxyInterface {

		void handle(List<String> list);
	}

	static class WithInterface implements ProxyInterface {

		@Override
		public void handle(List<String> list) {
		}
	}

	static class WithoutInterface {

		public void handle(List<String> list) {
		}
	}

}
