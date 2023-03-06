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

package org.springframework.context.aot;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AotApplicationContextInitializer}.
 *
 * @author Stephane Nicoll
 */
class AotApplicationContextInitializerTests {

	@Test
	void initializeInvokesApplicationContextInitializer() {
		GenericApplicationContext context = new GenericApplicationContext();
		AotApplicationContextInitializer.forInitializerClasses(
						TestApplicationContextInitializer.class.getName())
				.initialize(context);
		assertThat(context.getBeanDefinitionNames()).containsExactly("test");
	}

	@Test
	void initializeInvokesApplicationContextInitializersInOrder() {
		GenericApplicationContext context = new GenericApplicationContext();
		AotApplicationContextInitializer.forInitializerClasses(
						AnotherApplicationContextInitializer.class.getName(),
						TestApplicationContextInitializer.class.getName())
				.initialize(context);
		assertThat(context.getBeanDefinitionNames()).containsExactly("another", "test");
	}

	@Test
	void initializeWhenClassIsNotApplicationContextInitializerThrowsException() {
		GenericApplicationContext context = new GenericApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AotApplicationContextInitializer.forInitializerClasses("java.lang.String")
						.initialize(context))
				.withMessageContaining("not assignable")
				.withMessageContaining("ApplicationContextInitializer")
				.withMessageContaining("java.lang.String");
	}

	@Test
	void initializeWhenInitializerHasNoDefaultConstructorThrowsException() {
		GenericApplicationContext context = new GenericApplicationContext();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AotApplicationContextInitializer.forInitializerClasses(
						ConfigurableApplicationContextInitializer.class.getName()).initialize(context))
				.withMessageContaining("Failed to instantiate ApplicationContextInitializer: ")
				.withMessageContaining(ConfigurableApplicationContextInitializer.class.getName());
	}


	static class TestApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			applicationContext.registerBeanDefinition("test", new RootBeanDefinition());
		}

	}

	static class AnotherApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			applicationContext.registerBeanDefinition("another", new RootBeanDefinition());
		}

	}

	static class ConfigurableApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@SuppressWarnings("unused")
		public ConfigurableApplicationContextInitializer(ClassLoader classLoader) {
		}

		@Override
		public void initialize(GenericApplicationContext applicationContext) {

		}
	}

}
