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

package org.springframework.beans.factory.generator.config;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link InjectedMethodResolver}.
 *
 * @author Stephane Nicoll
 */
class InjectedMethodResolverTests {

	@Test
	void resolveSingleDependency() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("test", "testValue");
		InjectedElementAttributes attributes = createResolver(TestBean.class, "injectString", String.class)
				.resolve(beanFactory, true);
		assertThat(attributes.isResolved()).isTrue();
		assertThat((String) attributes.get(0)).isEqualTo("testValue");
	}

	@Test
	void resolveRequiredDependencyNotPresentThrowsUnsatisfiedDependencyException() {
		Method method = ReflectionUtils.findMethod(TestBean.class, "injectString", String.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		assertThatThrownBy(() -> createResolver(TestBean.class, "injectString", String.class)
				.resolve(beanFactory)).isInstanceOfSatisfying(UnsatisfiedDependencyException.class, ex -> {
			assertThat(ex.getBeanName()).isEqualTo("test");
			assertThat(ex.getInjectionPoint()).isNotNull();
			assertThat(ex.getInjectionPoint().getMember()).isEqualTo(method);
		});
	}

	@Test
	void resolveNonRequiredDependency() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		InjectedElementAttributes attributes = createResolver(TestBean.class, "injectString", String.class)
				.resolve(beanFactory, false);
		assertThat(attributes.isResolved()).isFalse();
	}

	@Test
	void resolveDependencyAndEnvironment() {
		Environment environment = mock(Environment.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("environment", environment);
		beanFactory.registerSingleton("test", "testValue");
		InjectedElementAttributes attributes = createResolver(TestBean.class, "injectStringAndEnvironment",
				String.class, Environment.class).resolve(beanFactory, true);
		assertThat(attributes.isResolved()).isTrue();
		String string = attributes.get(0);
		assertThat(string).isEqualTo("testValue");
		assertThat((Environment) attributes.get(1)).isEqualTo(environment);
	}

	@Test
	@SuppressWarnings("unchecked")
	void createWithUnresolvedAttributesDoesNotInvokeCallback() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinitionRegistrar.ThrowableFunction<InjectedElementAttributes, ?> callback = mock(BeanDefinitionRegistrar.ThrowableFunction.class);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				createResolver(TestBean.class, "injectString", String.class).create(beanFactory, callback));
		verifyNoInteractions(callback);
	}

	@Test
	@SuppressWarnings("unchecked")
	void invokeWithUnresolvedAttributesDoesNotInvokeCallback() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinitionRegistrar.ThrowableConsumer<InjectedElementAttributes> callback = mock(BeanDefinitionRegistrar.ThrowableConsumer.class);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				createResolver(TestBean.class, "injectString", String.class).invoke(beanFactory, callback));
		verifyNoInteractions(callback);
	}

	private InjectedMethodResolver createResolver(Class<?> beanType, String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(beanType, methodName, parameterTypes);
		assertThat(method).isNotNull();
		return new InjectedMethodResolver(method, beanType, "test");
	}

	@SuppressWarnings("unused")
	static class TestBean {

		public void injectString(String string) {

		}

		public void injectStringAndEnvironment(String string, Environment environment) {

		}

	}

}
