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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
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
 * Closeable/AutoCloseable) are recognized so that the CGLIB validation warning
 * can be suppressed for those container-driven methods.
 *
 * @since 7.1
 * @see <a href="https://github.com/spring-projects/spring-framework/pull/36935>gh-36935</a>
 */
class CglibAopProxyConfigurationCallbackTests {

	@Test
	void finalAfterPropertiesSetIsRecognisedAsCallback() {
		var method = ClassUtils.getMethod(WithFinalAfterPropertiesSet.class, "afterPropertiesSet");
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalAfterPropertiesSet.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalDestroyIsRecognisedAsCallback() {
		var method = ClassUtils.getMethod(WithFinalDestroy.class, "destroy");
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalDestroy.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalAwareCallbackIsRecognisedAsCallback() {
		var method = ClassUtils.getMethod(WithFinalBeanFactoryAware.class, "setBeanFactory", BeanFactory.class);
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalBeanFactoryAware.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalCloseableCloseIsRecognisedAsCallback() {
		var method = ClassUtils.getMethod(WithFinalCloseableClose.class, "close");
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalCloseableClose.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalAutoCloseableCloseIsRecognisedAsCallback() {
		var method = ClassUtils.getMethod(WithFinalAutoCloseableClose.class, "close");
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalAutoCloseableClose.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isTrue();
	}

	@Test
	void finalUserInterfaceMethodIsNotSuppressed() {
		var method = ClassUtils.getMethod(WithFinalUserApi.class, "execute");
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithFinalUserApi.class);

		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isFalse();
	}

	@Test
	void methodSharedBetweenCallbackAndUserInterfaceIsNotSuppressed() {
		var method = ClassUtils.getMethod(WithSharedSignature.class, "afterPropertiesSet");
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithSharedSignature.class);

		// Even though InitializingBean declares afterPropertiesSet(), a user
		// interface (CustomLifecycle) declares the same signature, so the
		// warning must still fire.
		assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isFalse();
	}

	@Test
	void finalMethodWithoutInterfaceMatchIsNotSuppressed() {
		var method = ClassUtils.getMethod(WithStandaloneFinal.class, "doSomething");
		var interfaces = ClassUtils.getAllInterfacesForClassAsSet(WithStandaloneFinal.class);

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
		public final void setBeanFactory(BeanFactory beanFactory) {
		}
	}

	static class WithFinalCloseableClose implements Closeable {

		@Override
		public final void close() {
		}
	}

	static class WithFinalAutoCloseableClose implements AutoCloseable {

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
