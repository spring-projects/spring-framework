/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Gh23206Tests.ConditionalConfiguration.NestedConfiguration;
import org.springframework.context.annotation.componentscan.simple.SimpleComponent;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for gh-23206.
 *
 * @author Stephane Nicoll
 */
class Gh23206Tests {

	@Test
	void componentScanShouldFailWithRegisterBeanCondition() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ConditionalComponentScanConfiguration.class);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(context::refresh)
				.withMessageContaining(ConditionalComponentScanConfiguration.class.getName())
				.havingCause().isInstanceOf(ApplicationContextException.class)
				.withMessageStartingWith("Component scan for configuration class [")
				.withMessageContaining(ConditionalComponentScanConfiguration.class.getName())
				.withMessageContaining("could not be used with conditions in REGISTER_BEAN phase");
	}

	@Test
	void componentScanShouldFailWithRegisterBeanConditionOnClasThatImportedIt() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ConditionalConfiguration.class);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(context::refresh)
				.withMessageContaining(ConditionalConfiguration.class.getName())
				.havingCause().isInstanceOf(ApplicationContextException.class)
				.withMessageStartingWith("Component scan for configuration class [")
				.withMessageContaining(NestedConfiguration.class.getName())
				.withMessageContaining("could not be used with conditions in REGISTER_BEAN phase");
	}


	@Configuration(proxyBeanMethods = false)
	@Conditional(NeverRegisterBeanCondition.class)
	@ComponentScan(basePackageClasses = SimpleComponent.class)
	static class ConditionalComponentScanConfiguration {

	}


	@Configuration(proxyBeanMethods = false)
	@Conditional(NeverRegisterBeanCondition.class)
	static class ConditionalConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ComponentScan(basePackageClasses = SimpleComponent.class)
		static class NestedConfiguration {
		}

	}


	static class NeverRegisterBeanCondition implements ConfigurationCondition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

	}
}
