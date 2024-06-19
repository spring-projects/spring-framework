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

package org.springframework.beans.factory;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 6.2
 */
public class CustomObjectProviderTests {

	@Test
	void getObject() {
		TestBean tb1 = new TestBean("tb1");

		ObjectProvider<TestBean> provider = new ObjectProvider<>() {
			@Override
			public TestBean getObject() throws BeansException {
				return tb1;
			}
		};

		assertThat(provider.getObject()).isSameAs(tb1);
		assertThat(provider.getIfAvailable()).isSameAs(tb1);
		assertThat(provider.getIfUnique()).isSameAs(tb1);
	}

	@Test
	void noObject() {
		ObjectProvider<TestBean> provider = new ObjectProvider<>() {
			@Override
			public TestBean getObject() throws BeansException {
				throw new NoSuchBeanDefinitionException(Object.class);
			}
		};

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(provider::getObject);
		assertThat(provider.getIfAvailable()).isNull();
		assertThat(provider.getIfUnique()).isNull();
	}

	@Test
	void noUniqueObject() {
		ObjectProvider<TestBean> provider = new ObjectProvider<>() {
			@Override
			public TestBean getObject() throws BeansException {
				throw new NoUniqueBeanDefinitionException(Object.class);
			}
		};

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getObject);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getIfAvailable);
		assertThat(provider.getIfUnique()).isNull();
	}

	@Test
	void emptyStream() {
		ObjectProvider<TestBean> provider = new ObjectProvider<>() {
			@Override
			public Stream<TestBean> stream() {
				return Stream.empty();
			}
		};

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(provider::getObject);
		assertThat(provider.getIfAvailable()).isNull();
		assertThat(provider.getIfUnique()).isNull();
	}

	@Test
	void streamWithOneObject() {
		TestBean tb1 = new TestBean("tb1");

		ObjectProvider<TestBean> provider = new ObjectProvider<>() {
			@Override
			public Stream<TestBean> stream() {
				return Stream.of(tb1);
			}
		};

		assertThat(provider.getObject()).isSameAs(tb1);
		assertThat(provider.getIfAvailable()).isSameAs(tb1);
		assertThat(provider.getIfUnique()).isSameAs(tb1);
	}

	@Test
	void streamWithTwoObjects() {
		TestBean tb1 = new TestBean("tb1");
		TestBean tb2 = new TestBean("tb2");

		ObjectProvider<TestBean> provider = new ObjectProvider<>() {
			@Override
			public Stream<TestBean> stream() {
				return Stream.of(tb1, tb2);
			}
		};

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getObject);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getIfAvailable);
		assertThat(provider.getIfUnique()).isNull();
	}

}
