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
public class Gh23206Tests {

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
