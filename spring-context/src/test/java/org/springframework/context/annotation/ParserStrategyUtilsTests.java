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

package org.springframework.context.annotation;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

/**
 * Tests for {@link ParserStrategyUtils}.
 *
 * @author Phillip Webb
 */
class ParserStrategyUtilsTests {

	@Mock
	private Environment environment;

	@Mock(extraInterfaces = BeanFactory.class)
	private BeanDefinitionRegistry registry;

	@Mock
	private ClassLoader beanClassLoader;

	@Mock
	private ResourceLoader resourceLoader;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		given(this.resourceLoader.getClassLoader()).willReturn(this.beanClassLoader);
	}

	@Test
	void instantiateClassWhenHasNoArgsConstructorCallsAware() {
		NoArgsConstructor instance = instantiateClass(NoArgsConstructor.class);
		assertThat(instance.setEnvironment).isSameAs(this.environment);
		assertThat(instance.setBeanFactory).isSameAs(this.registry);
		assertThat(instance.setBeanClassLoader).isSameAs(this.beanClassLoader);
		assertThat(instance.setResourceLoader).isSameAs(this.resourceLoader);
	}

	@Test
	void instantiateClassWhenHasSingleConstructorInjectsParams() {
		ArgsConstructor instance = instantiateClass(ArgsConstructor.class);
		assertThat(instance.environment).isSameAs(this.environment);
		assertThat(instance.beanFactory).isSameAs(this.registry);
		assertThat(instance.beanClassLoader).isSameAs(this.beanClassLoader);
		assertThat(instance.resourceLoader).isSameAs(this.resourceLoader);
	}

	@Test
	void instantiateClassWhenHasSingleConstructorAndAwareInjectsParamsAndCallsAware() {
		ArgsConstructorAndAware instance = instantiateClass(ArgsConstructorAndAware.class);
		assertThat(instance.environment).isSameAs(this.environment);
		assertThat(instance.setEnvironment).isSameAs(this.environment);
		assertThat(instance.beanFactory).isSameAs(this.registry);
		assertThat(instance.setBeanFactory).isSameAs(this.registry);
		assertThat(instance.beanClassLoader).isSameAs(this.beanClassLoader);
		assertThat(instance.setBeanClassLoader).isSameAs(this.beanClassLoader);
		assertThat(instance.resourceLoader).isSameAs(this.resourceLoader);
		assertThat(instance.setResourceLoader).isSameAs(this.resourceLoader);
	}

	@Test
	void instantiateClassWhenHasMultipleConstructorsUsesNoArgsConstructor() {
		// Remain back-compatible by using the default constructor if there's more than one
		MultipleConstructors instance = instantiateClass(MultipleConstructors.class);
		assertThat(instance.usedDefaultConstructor).isTrue();
	}

	@Test
	void instantiateClassWhenHasMultipleConstructorsAndNotDefaultThrowsException() {
		assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
				instantiateClass(MultipleConstructorsWithNoDefault.class));
	}

	@Test
	void instantiateClassWhenHasUnsupportedParameterThrowsException() {
		assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
				instantiateClass(InvalidConstructorParameterType.class))
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("No suitable constructor found");
	}

	@Test
	void instantiateClassHasSubclassParameterThrowsException() {
		// To keep the algorithm simple we don't support subtypes
		assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
				instantiateClass(InvalidConstructorParameterSubType.class))
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("No suitable constructor found");
	}

	@Test
	void instantiateClassWhenHasNoBeanClassLoaderInjectsNull() {
		reset(this.resourceLoader);
		ArgsConstructor instance = instantiateClass(ArgsConstructor.class);
		assertThat(instance.beanClassLoader).isNull();
	}

	@Test
	void instantiateClassWhenHasNoBeanClassLoaderDoesNotCallAware() {
		reset(this.resourceLoader);
		NoArgsConstructor instance = instantiateClass(NoArgsConstructor.class);
		assertThat(instance.setBeanClassLoader).isNull();
		assertThat(instance.setBeanClassLoaderCalled).isFalse();
	}

	private <T> T instantiateClass(Class<T> clazz) {
		return ParserStrategyUtils.instantiateClass(clazz, clazz, this.environment,
				this.resourceLoader, this.registry);
	}

	static class NoArgsConstructor implements BeanClassLoaderAware,
			BeanFactoryAware, EnvironmentAware, ResourceLoaderAware {

		Environment setEnvironment;

		BeanFactory setBeanFactory;

		ClassLoader setBeanClassLoader;

		boolean setBeanClassLoaderCalled;

		ResourceLoader setResourceLoader;

		@Override
		public void setEnvironment(Environment environment) {
			this.setEnvironment = environment;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.setBeanFactory = beanFactory;
		}

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.setBeanClassLoader = classLoader;
			this.setBeanClassLoaderCalled = true;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.setResourceLoader = resourceLoader;
		}

	}

	static class ArgsConstructor {

		final Environment environment;

		final BeanFactory beanFactory;

		final ClassLoader beanClassLoader;

		final ResourceLoader resourceLoader;

		ArgsConstructor(Environment environment, BeanFactory beanFactory,
				ClassLoader beanClassLoader, ResourceLoader resourceLoader) {
			this.environment = environment;
			this.beanFactory = beanFactory;
			this.beanClassLoader = beanClassLoader;
			this.resourceLoader = resourceLoader;
		}

	}

	static class ArgsConstructorAndAware extends NoArgsConstructor {

		final Environment environment;

		final BeanFactory beanFactory;

		final ClassLoader beanClassLoader;

		final ResourceLoader resourceLoader;

		ArgsConstructorAndAware(Environment environment, BeanFactory beanFactory,
				ClassLoader beanClassLoader, ResourceLoader resourceLoader) {
			this.environment = environment;
			this.beanFactory = beanFactory;
			this.beanClassLoader = beanClassLoader;
			this.resourceLoader = resourceLoader;
		}

	}

	static class MultipleConstructors {

		final boolean usedDefaultConstructor;

		MultipleConstructors() {
			this.usedDefaultConstructor = true;
		}

		MultipleConstructors(Environment environment) {
			this.usedDefaultConstructor = false;
		}

	}

	static class MultipleConstructorsWithNoDefault {

		MultipleConstructorsWithNoDefault(Environment environment, BeanFactory beanFactory) {
		}

		MultipleConstructorsWithNoDefault(Environment environment) {
		}

	}

	static class InvalidConstructorParameterType {

		InvalidConstructorParameterType(Environment environment, InputStream inputStream) {
		}

	}

	static class InvalidConstructorParameterSubType {

		InvalidConstructorParameterSubType(ConfigurableEnvironment environment) {
		}

	}


}
