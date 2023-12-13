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

package org.springframework.context.annotation;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ResourceElementResolver} with methods.
 *
 * @author Stephane Nicoll
 */
class ResourceElementResolverMethodTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@Test
	void resolveWhenMethodIsMissingThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		ResourceElementResolver resolver = ResourceElementResolver.forMethod("missing", InputStream.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.resolve(registeredBean))
				.withMessage("Method 'missing' with parameter type 'java.io.InputStream' declared on %s could not be found.",
						TestBean.class.getName());
	}

	@Test
	void resolveReturnsValue() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		ResourceElementResolver resolver = ResourceElementResolver.forMethod("setOne", String.class);
		Object resolved = resolver.resolve(registeredBean);
		assertThat(resolved).isEqualTo("1");
	}

	@Test
	void resolveWhenResourceNameAndMatchReturnsValue() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = ResourceElementResolver.forMethod("setTest", String.class, "two").resolve(registeredBean);
		assertThat(resolved).isEqualTo("2");
	}

	@Test
	void resolveWheNoMatchFallbackOnType() {
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = ResourceElementResolver.forMethod("setOne", String.class).resolve(registeredBean);
		assertThat(resolved).isEqualTo("2");
	}

	@Test
	void resolveWhenMultipleCandidatesWithNoNameMatchThrowsException() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatThrownBy(() -> ResourceElementResolver.forMethod("setTest", String.class).resolve(registeredBean))
				.isInstanceOf(NoUniqueBeanDefinitionException.class)
				.hasMessageContaining(String.class.getName())
				.hasMessageContaining("one").hasMessageContaining("two");
	}

	@Test
	void resolveWhenNoCandidateMatchingTypeThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatThrownBy(() -> ResourceElementResolver.forMethod("setTest", String.class).resolve(registeredBean))
				.isInstanceOf(NoSuchBeanDefinitionException.class)
				.hasMessageContaining(String.class.getName());
	}

	@Test
	void resolveWhenInvalidMatchingTypeThrowsException() {
		this.beanFactory.registerSingleton("count", "counter");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatThrownBy(() -> ResourceElementResolver.forMethod("setCount", Integer.class).resolve(registeredBean))
				.isInstanceOf(BeanNotOfRequiredTypeException.class)
				.hasMessageContaining(Integer.class.getName())
				.hasMessageContaining(String.class.getName());
	}

	@Test
	void resolveAndInvokeInvokesMethod() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		TestBean testBean = new TestBean();
		ResourceElementResolver.forMethod("setOne", String.class).resolveAndSet(registeredBean, testBean);
		assertThat(testBean.one).isEqualTo("1");
	}

	@Test
	void resolveRegistersDependantBeans() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		ResourceElementResolver.forMethod("setOne", String.class).resolve(registeredBean);
		assertThat(this.beanFactory.getDependentBeans("one")).containsExactly("testBean");
	}

	private RegisteredBean registerTestBean(DefaultListableBeanFactory beanFactory) {
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class));
		return RegisteredBean.of(beanFactory, "testBean");
	}


	static class TestBean {

		private String one;

		private String test;

		private Integer count;

		public void setOne(String one) {
			this.one = one;
		}

		public void setTest(String test) {
			this.test = test;
		}

		public void setCount(Integer count) {
			this.count = count;
		}
	}

}
