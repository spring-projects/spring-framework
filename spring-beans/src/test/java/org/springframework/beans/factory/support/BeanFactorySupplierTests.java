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

package org.springframework.beans.factory.support;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.function.ThrowingSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractAutowireCapableBeanFactory} instance supplier support.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class BeanFactorySupplierTests {

	@Test
	void getBeanWhenUsingRegularSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(() -> "I am supplied");
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test")).isEqualTo("I am supplied");
	}

	@Test
	void getBeanWithInnerBeanUsingRegularSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(() -> "I am supplied");
		RootBeanDefinition outerBean = new RootBeanDefinition(String.class);
		outerBean.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition);
		beanFactory.registerBeanDefinition("test", outerBean);
		assertThat(beanFactory.getBean("test")).asString().startsWith("I am supplied");
	}

	@Test
	void getBeanWhenUsingInstanceSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean ->
				"I am bean " + registeredBean.getBeanName() + " of " + registeredBean.getBeanClass()));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test")).isEqualTo("I am bean test of class java.lang.String");
	}

	@Test
	void getBeanWithInnerBeanUsingInstanceSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean ->
				"I am bean " + registeredBean.getBeanName() + " of " + registeredBean.getBeanClass()));
		RootBeanDefinition outerBean = new RootBeanDefinition(String.class);
		outerBean.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition);
		beanFactory.registerBeanDefinition("test", outerBean);
		assertThat(beanFactory.getBean("test")).asString()
				.startsWith("I am bean (inner bean)")
				.endsWith(" of class java.lang.String");
	}

	@Test
	void getBeanWhenUsingThrowableSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> "I am supplied"));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test")).isEqualTo("I am supplied");
	}

	@Test
	void getBeanWithInnerBeanUsingThrowableSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> "I am supplied"));
		RootBeanDefinition outerBean = new RootBeanDefinition(String.class);
		outerBean.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition);
		beanFactory.registerBeanDefinition("test", outerBean);
		assertThat(beanFactory.getBean("test")).asString().startsWith("I am supplied");
	}

	@Test
	void getBeanWhenUsingThrowableSupplierThatThrowsCheckedException() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> {
			throw new IOException("fail");
		}));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> beanFactory.getBean("test"))
				.withCauseInstanceOf(IOException.class);
	}

	@Test
	void getBeanWhenUsingThrowableSupplierThatThrowsRuntimeException() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> {
			throw new IllegalStateException("fail");
		}));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> beanFactory.getBean("test"))
				.withCauseInstanceOf(IllegalStateException.class);
	}

}
