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

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link InjectedFieldResolver}.
 *
 * @author Stephane Nicoll
 */
class InjectedFieldResolverTests {

	@Test
	void resolveDependency() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("one", "1");
		InjectedElementAttributes attributes = createResolver(TestBean.class, "string",
				String.class).resolve(beanFactory, true);
		assertThat(attributes.isResolved()).isTrue();
		assertThat((String) attributes.get(0)).isEqualTo("1");
	}

	@Test
	void resolveRequiredDependencyNotPresentThrowsUnsatisfiedDependencyException() {
		Field field = ReflectionUtils.findField(TestBean.class, "string", String.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		assertThatThrownBy(() -> createResolver(TestBean.class, "string", String.class).resolve(beanFactory))
				.isInstanceOfSatisfying(UnsatisfiedDependencyException.class, ex -> {
					assertThat(ex.getBeanName()).isEqualTo("test");
					assertThat(ex.getInjectionPoint()).isNotNull();
					assertThat(ex.getInjectionPoint().getField()).isEqualTo(field);
				});
	}

	@Test
	void resolveNonRequiredDependency() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		InjectedElementAttributes attributes = createResolver(TestBean.class, "string", String.class).resolve(beanFactory, false);
		assertThat(attributes.isResolved()).isFalse();
	}

	private InjectedFieldResolver createResolver(Class<?> beanType, String fieldName, Class<?> fieldType) {
		Field field = ReflectionUtils.findField(beanType, fieldName, fieldType);
		assertThat(field).isNotNull();
		return new InjectedFieldResolver(field, "test");
	}

	static class TestBean {

		private String string;

	}

}
