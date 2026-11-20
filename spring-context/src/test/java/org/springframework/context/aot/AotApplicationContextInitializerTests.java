/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
