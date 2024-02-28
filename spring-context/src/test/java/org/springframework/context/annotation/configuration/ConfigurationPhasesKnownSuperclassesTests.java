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

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 * @since 6.2
 */
class ConfigurationPhasesKnownSuperclassesTests {

	@Test
	void superclassSkippedInParseConfigurationPhaseShouldNotPreventSubsequentProcessingOfSameSuperclass() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ParseConfigurationPhase.class)) {
			assertThat(context.getBean("subclassBean")).isEqualTo("bravo");
			assertThat(context.getBean("superclassBean")).isEqualTo("superclass");
			assertThat(context.getBean("baseBean")).isEqualTo("base");
		}
	}

	@Test
	void superclassSkippedInRegisterBeanPhaseShouldNotPreventSubsequentProcessingOfSameSuperclass() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RegisterBeanPhase.class)) {
			assertThat(context.getBean("subclassBean")).isEqualTo("bravo");
			assertThat(context.getBean("superclassBean")).isEqualTo("superclass");
			assertThat(context.getBean("baseBean")).isEqualTo("base");
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class Base {

		@Bean
		String baseBean() {
			return "base";
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Example extends Base {

		@Bean
		String superclassBean() {
			return "superclass";
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({RegisterBeanPhaseExample.class, BravoExample.class})
	static class RegisterBeanPhase {
	}

	@Conditional(NonMatchingRegisterBeanPhaseCondition.class)
	@Configuration(proxyBeanMethods = false)
	static class RegisterBeanPhaseExample extends Example {

		@Bean
		String subclassBean() {
			return "alpha";
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ParseConfigurationPhaseExample.class, BravoExample.class})
	static class ParseConfigurationPhase {
	}

	@Conditional(NonMatchingParseConfigurationPhaseCondition.class)
	@Configuration(proxyBeanMethods = false)
	static class ParseConfigurationPhaseExample extends Example {

		@Bean
		String subclassBean() {
			return "alpha";
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class BravoExample extends Example {

		@Bean
		String subclassBean() {
			return "bravo";
		}
	}

	static class NonMatchingRegisterBeanPhaseCondition implements ConfigurationCondition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}
	}

	static class NonMatchingParseConfigurationPhaseCondition implements ConfigurationCondition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}
	}

}
