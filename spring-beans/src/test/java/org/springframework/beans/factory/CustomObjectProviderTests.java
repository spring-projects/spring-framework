/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class CustomObjectProviderTests {

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
