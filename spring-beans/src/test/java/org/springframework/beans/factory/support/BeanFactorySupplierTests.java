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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
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
	void getBeanWhenUsingInstanceSupplierUsesObtainInstanceFromSupplierExtensionPoint() {
		AtomicBoolean called = new AtomicBoolean();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory() {
			@Override
			protected Object obtainInstanceFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition mbd)
					throws Exception {

				called.set(true);
				return super.obtainInstanceFromSupplier(supplier, beanName, mbd);
			}
		};
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean -> "I am supplied"));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test")).isEqualTo("I am supplied");
		assertThat(called).isTrue();
	}

	@Test
	void getBeanWithExplicitArgumentsWhenUsingSupportingInstanceSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(Object.class);
		beanDefinition.setInstanceSupplier(new InstanceSupplier<>() {
			@Override
			public Object get(RegisteredBean registeredBean) {
				return "I am supplied";
			}
			@Override
			public Object get(RegisteredBean registeredBean, Object... args) {
				return args[0];
			}
			@Override
			public boolean supportsExplicitArguments(Object... args) {
				return true;
			}
		});
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test", "I am supplied with an argument"))
				.isEqualTo("I am supplied with an argument");
	}

	@Test
	void getBeanWithEmptyExplicitArgumentsWhenUsingSupportingInstanceSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(Object.class);
		beanDefinition.setInstanceSupplier(new InstanceSupplier<>() {
			@Override
			public Object get(RegisteredBean registeredBean) {
				return "I am supplied";
			}
			@Override
			public Object get(RegisteredBean registeredBean, Object... args) {
				return args.length;
			}
			@Override
			public boolean supportsExplicitArguments(Object... args) {
				return true;
			}
		});
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test", new Object[0])).isEqualTo(0);
	}

	@Test
	void getBeanWithEmptyExplicitArgumentsWhenUsingNonSupportingInstanceSupplier() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean -> "I am supplied"));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test", new Object[0])).isEqualTo("");
	}

	@Test
	void getBeanWithExplicitArgumentsWhenUsingUnsupportedBeanInstanceSupplierArgumentsUsesRegularInstantiation() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MultiConstructorBean.class);
		beanDefinition.setInstanceSupplier(BeanInstanceSupplier.forConstructor(String.class)
				.withGenerator((registeredBean, args) -> new MultiConstructorBean(args.get(0), null)));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		MultiConstructorBean bean = beanFactory.getBean(MultiConstructorBean.class, "test", 1);
		assertThat(bean.name).isEqualTo("test");
		assertThat(bean.counter).isEqualTo(1);
	}

	@Test
	void getBeanWithExplicitArgumentsWhenUsingUnsupportedBeanInstanceSupplierArgumentTypesUsesRegularInstantiation() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MultiConstructorBean.class);
		beanDefinition.setInstanceSupplier(BeanInstanceSupplier.forConstructor(String.class)
				.withGenerator((registeredBean, args) -> new MultiConstructorBean(args.get(0), null)));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		MultiConstructorBean bean = beanFactory.getBean(MultiConstructorBean.class, 1);
		assertThat(bean.name).isNull();
		assertThat(bean.counter).isEqualTo(1);
	}

	@Test
	void getBeanWithExplicitArgumentsWhenUsingAssignableBeanInstanceSupplierArgumentsUsesRegularInstantiation() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(OverloadedConstructorBean.class);
		beanDefinition.setInstanceSupplier(BeanInstanceSupplier.forConstructor(Object.class)
				.withGenerator((registeredBean, args) -> new OverloadedConstructorBean(args.get(0), "supplier")));
		beanFactory.registerBeanDefinition("test", beanDefinition);
		OverloadedConstructorBean bean = beanFactory.getBean(OverloadedConstructorBean.class, "test");
		assertThat(bean.argument).isEqualTo("test");
		assertThat(bean.constructor).isEqualTo("string");
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


	static class MultiConstructorBean {

		final String name;

		final Integer counter;

		MultiConstructorBean(String name) {
			this(name, null);
		}

		MultiConstructorBean(Integer counter) {
			this(null, counter);
		}

		MultiConstructorBean(String name, Integer counter) {
			this.name = name;
			this.counter = counter;
		}
	}


	static class OverloadedConstructorBean {

		final Object argument;

		final String constructor;

		OverloadedConstructorBean(Object argument) {
			this(argument, "object");
		}

		OverloadedConstructorBean(String argument) {
			this(argument, "string");
		}

		OverloadedConstructorBean(Object argument, String constructor) {
			this.argument = argument;
			this.constructor = constructor;
		}
	}

}
