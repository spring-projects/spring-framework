/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RootBeanDefinition}.
 *
 * @author Stephane Nicoll
 */
class RootBeanDefinitionTests {

	@Test
	void setInstanceSetResolvedFactoryMethod() {
		InstanceSupplier<?> instanceSupplier = mock();
		Method method = ReflectionUtils.findMethod(String.class, "toString");
		given(instanceSupplier.getFactoryMethod()).willReturn(method);
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(instanceSupplier);
		assertThat(beanDefinition.getResolvedFactoryMethod()).isEqualTo(method);
		verify(instanceSupplier).getFactoryMethod();
	}

	@Test
	void setInstanceDoesNotOverrideResolvedFactoryMethodWithNull() {
		InstanceSupplier<?> instanceSupplier = mock();
		given(instanceSupplier.getFactoryMethod()).willReturn(null);
		Method method = ReflectionUtils.findMethod(String.class, "toString");
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setResolvedFactoryMethod(method);
		beanDefinition.setInstanceSupplier(instanceSupplier);
		assertThat(beanDefinition.getResolvedFactoryMethod()).isEqualTo(method);
		verify(instanceSupplier).getFactoryMethod();
	}

	@Test
	void resolveDestroyMethodWithMatchingCandidateReplacedInferredVaue() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanWithCloseMethod.class);
		beanDefinition.setDestroyMethodName(AbstractBeanDefinition.INFER_METHOD);
		beanDefinition.resolveDestroyMethodIfNecessary();
		assertThat(beanDefinition.getDestroyMethodNames()).containsExactly("close");
	}

	@Test
	void resolveDestroyMethodWithNoCandidateSetDestroyMethodNameToNull() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanWithNoDestroyMethod.class);
		beanDefinition.setDestroyMethodName(AbstractBeanDefinition.INFER_METHOD);
		beanDefinition.resolveDestroyMethodIfNecessary();
		assertThat(beanDefinition.getDestroyMethodNames()).isNull();
	}

	@Test
	void resolveDestroyMethodWithNoResolvableType() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setDestroyMethodName(AbstractBeanDefinition.INFER_METHOD);
		beanDefinition.resolveDestroyMethodIfNecessary();
		assertThat(beanDefinition.getDestroyMethodNames()).isNull();
	}

	static class BeanWithCloseMethod {

		public void close() {
		}

	}

	static class BeanWithNoDestroyMethod {

	}

}
