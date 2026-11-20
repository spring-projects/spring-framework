/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
