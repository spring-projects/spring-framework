/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CglibAopProxy#implementsOnlyConfigurationCallbackInterfaces}.
 *
 * <p>Verifies that final methods inherited from Spring's configuration callback
 * interfaces (InitializingBean, DisposableBean, Aware sub-interfaces,
 * Closeable/AutoCloseable) are recognised so that the CGLIB validation warning
 * can be suppressed for those container-driven methods (gh-35365).
 */
class CglibAopProxyConfigurationCallbackTests {

	@Test
	void finalAfterPropertiesSetIsRecognisedAsCallback() throws NoSuchMethodException {
		Method method = WithFinalAfterPropertiesSet.class.getDeclaredMethod("afterPropertiesSet");
		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalAfterPropertiesSet.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalDestroyIsRecognisedAsCallback() throws NoSuchMethodException {
		Method method = WithFinalDestroy.class.getDeclaredMethod("destroy");
		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalDestroy.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalAwareCallbackIsRecognisedAsCallback() throws NoSuchMethodException {
		Method method = WithFinalBeanFactoryAware.class.getDeclaredMethod("setBeanFactory",
				org.springframework.beans.factory.BeanFactory.class);
		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalBeanFactoryAware.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalCloseIsRecognisedAsCallback() throws NoSuchMethodException {
		Method method = WithFinalClose.class.getDeclaredMethod("close");
		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalClose.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalUserInterfaceMethodIsNotSuppressed() throws NoSuchMethodException {
		Method method = WithFinalUserApi.class.getDeclaredMethod("execute");
		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalUserApi.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isFalse();
	}

	@Test
	void methodSharedBetweenCallbackAndUserInterfaceIsNotSuppressed() throws NoSuchMethodException {
		Method method = WithSharedSignature.class.getDeclaredMethod("afterPropertiesSet");
		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithSharedSignature.class);

		// Even though InitializingBean declares afterPropertiesSet(), a user
		// interface (CustomLifecycle) declares the same signature, so the
		// warning must still fire.
		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isFalse();
	}

	@Test
	void finalMethodWithoutInterfaceMatchIsNotSuppressed() throws NoSuchMethodException {
		Method method = WithStandaloneFinal.class.getDeclaredMethod("doSomething");
		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithStandaloneFinal.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isFalse();
	}


	static class WithFinalAfterPropertiesSet implements InitializingBean {

		@Override
		public final void afterPropertiesSet() {
		}
	}

	static class WithFinalDestroy implements DisposableBean {

		@Override
		public final void destroy() {
		}
	}

	static class WithFinalBeanFactoryAware implements BeanFactoryAware {

		@Override
		public final void setBeanFactory(org.springframework.beans.factory.BeanFactory beanFactory) {
		}
	}

	static class WithFinalClose implements Closeable {

		@Override
		public final void close() {
		}
	}

	interface UserApi {

		void execute();
	}

	static class WithFinalUserApi implements UserApi {

		@Override
		public final void execute() {
		}
	}

	interface CustomLifecycle {

		void afterPropertiesSet();
	}

	static class WithSharedSignature implements InitializingBean, CustomLifecycle {

		@Override
		public final void afterPropertiesSet() {
		}
	}

	static class WithStandaloneFinal {

		public final void doSomething() {
		}
	}

}
