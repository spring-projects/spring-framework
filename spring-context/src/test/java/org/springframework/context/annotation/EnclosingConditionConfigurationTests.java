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

package org.springframework.context.annotation;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@code @Conditional} annotations declared on a {@code @Configuration}
 * class also gate registration of any nested static {@code @Configuration} classes
 * when those nested classes are discovered via {@link ComponentScan} or direct
 * registration — not only via the {@code processMemberClasses} recursion path
 * or via {@link Import}.
 *
 * <p>Prior to this behavior, a nested {@code @Configuration} discovered through
 * an independent path (component scan or direct registration) was processed using
 * only its own metadata, silently bypassing any condition declared on its lexical
 * enclosing class.
 */
class EnclosingConditionConfigurationTests {

	@Test
	void scanDiscoveredInnerSkipsWhenEnclosingParseConditionFails() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ScannerForParseDisabledOuter.class)) {
			assertThat(context.containsBean("innerBean")).isFalse();
		}
	}

	@Test
	void scanDiscoveredInnerSkipsWhenEnclosingRegisterBeanConditionFails() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ScannerForRegisterBeanDisabledOuter.class)) {
			assertThat(context.containsBean("innerBean")).isFalse();
		}
	}

	@Test
	void scanDiscoveredInnerIsRegisteredWhenEnclosingConditionMatches() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ScannerForEnabledOuter.class)) {
			assertThat(context.containsBean("innerBean")).isTrue();
		}
	}

	@Test
	void directlyRegisteredInnerSkipsWhenEnclosingParseConditionFails() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(ParseDisabledOuter.Inner.class);
			context.refresh();
			assertThat(context.containsBean("innerBean")).isFalse();
		}
	}

	@Test
	void deeplyNestedInnerSkipsWhenOutermostParseConditionFails() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ScannerForDeeplyNestedDisabledOuter.class)) {
			assertThat(context.containsBean("deepInnerBean")).isFalse();
		}
	}

	@Test
	void scanDiscoveredInnerSkipsWhenEnclosingInterfaceConditionFails() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ScannerForInterfaceEnclosingDisabled.class)) {
			assertThat(context.containsBean("innerBean")).isFalse();
		}
	}

	@Test
	void scanDiscoveredInnerSkipsWhenEnclosingFailsEvenIfInnerOwnConditionMatches() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ScannerForMixedPhaseOuterFails.class)) {
			assertThat(context.containsBean("innerBean")).isFalse();
		}
	}


	@Configuration(proxyBeanMethods = false)
	@ComponentScan(
			basePackageClasses = ParseDisabledOuter.class,
			useDefaultFilters = false,
			includeFilters = @Filter(Configuration.class),
			resourcePattern = "EnclosingConditionConfigurationTests$ParseDisabledOuter*.class")
	static class ScannerForParseDisabledOuter {
	}


	@Configuration(proxyBeanMethods = false)
	@ComponentScan(
			basePackageClasses = RegisterBeanDisabledOuter.class,
			useDefaultFilters = false,
			includeFilters = @Filter(Configuration.class),
			resourcePattern = "EnclosingConditionConfigurationTests$RegisterBeanDisabledOuter*.class")
	static class ScannerForRegisterBeanDisabledOuter {
	}


	@Configuration(proxyBeanMethods = false)
	@ComponentScan(
			basePackageClasses = EnabledOuter.class,
			useDefaultFilters = false,
			includeFilters = @Filter(Configuration.class),
			resourcePattern = "EnclosingConditionConfigurationTests$EnabledOuter*.class")
	static class ScannerForEnabledOuter {
	}


	@Configuration(proxyBeanMethods = false)
	@ComponentScan(
			basePackageClasses = DeeplyNestedDisabledOuter.class,
			useDefaultFilters = false,
			includeFilters = @Filter(Configuration.class),
			resourcePattern = "EnclosingConditionConfigurationTests$DeeplyNestedDisabledOuter*.class")
	static class ScannerForDeeplyNestedDisabledOuter {
	}


	@Configuration(proxyBeanMethods = false)
	@ComponentScan(
			basePackageClasses = InterfaceEnclosingDisabled.class,
			useDefaultFilters = false,
			includeFilters = @Filter(Configuration.class),
			resourcePattern = "EnclosingConditionConfigurationTests$InterfaceEnclosingDisabled*.class")
	static class ScannerForInterfaceEnclosingDisabled {
	}


	@Configuration(proxyBeanMethods = false)
	@ComponentScan(
			basePackageClasses = MixedPhaseOuterFails.class,
			useDefaultFilters = false,
			includeFilters = @Filter(Configuration.class),
			resourcePattern = "EnclosingConditionConfigurationTests$MixedPhaseOuterFails*.class")
	static class ScannerForMixedPhaseOuterFails {
	}


	@Configuration(proxyBeanMethods = false)
	@Conditional(NeverMatchParseCondition.class)
	static class ParseDisabledOuter {

		@Configuration(proxyBeanMethods = false)
		static class Inner {

			@Bean
			String innerBean() {
				return "inner";
			}
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Conditional(NeverMatchRegisterBeanCondition.class)
	static class RegisterBeanDisabledOuter {

		@Configuration(proxyBeanMethods = false)
		static class Inner {

			@Bean
			String innerBean() {
				return "inner";
			}
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Conditional(AlwaysMatchCondition.class)
	static class EnabledOuter {

		@Configuration(proxyBeanMethods = false)
		static class Inner {

			@Bean
			String innerBean() {
				return "inner";
			}
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Conditional(NeverMatchParseCondition.class)
	static class DeeplyNestedDisabledOuter {

		@Configuration(proxyBeanMethods = false)
		static class Middle {

			@Configuration(proxyBeanMethods = false)
			static class DeepInner {

				@Bean
				String deepInnerBean() {
					return "deep";
				}
			}
		}
	}


	@Conditional(NeverMatchParseCondition.class)
	interface InterfaceEnclosingDisabled {

		@Configuration(proxyBeanMethods = false)
		class Inner {

			@Bean
			String innerBean() {
				return "inner";
			}
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Conditional(NeverMatchRegisterBeanCondition.class)
	static class MixedPhaseOuterFails {

		@Configuration(proxyBeanMethods = false)
		@Conditional(AlwaysMatchCondition.class)
		static class Inner {

			@Bean
			String innerBean() {
				return "inner";
			}
		}
	}


	static class NeverMatchParseCondition implements ConfigurationCondition {

		@Override
		public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public @NonNull ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}
	}


	static class NeverMatchRegisterBeanCondition implements ConfigurationCondition {

		@Override
		public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public @NonNull ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}
	}


	static class AlwaysMatchCondition implements ConfigurationCondition {

		@Override
		public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
			return true;
		}

		@Override
		public @NonNull ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}
	}

}
