/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 */
class Spr16217Tests {

	@Test
	void baseConfigurationIsIncludedWhenFirstSuperclassReferenceIsSkippedInRegisterBeanPhase() {
		try (AnnotationConfigApplicationContext context =
					new AnnotationConfigApplicationContext(RegisterBeanPhaseImportingConfiguration.class)) {
			context.getBean("someBean");
		}
	}

	@Test
	void baseConfigurationIsIncludedWhenFirstSuperclassReferenceIsSkippedInParseConfigurationPhase() {
		try (AnnotationConfigApplicationContext context =
					new AnnotationConfigApplicationContext(ParseConfigurationPhaseImportingConfiguration.class)) {
			context.getBean("someBean");
		}
	}

	@Test
	void baseConfigurationIsIncludedOnceWhenBothConfigurationClassesAreActive() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		try (context) {
			context.setAllowBeanDefinitionOverriding(false);
			context.register(UnconditionalImportingConfiguration.class);
			context.refresh();
			context.getBean("someBean");
		}
	}


	public static class RegisterBeanPhaseCondition implements ConfigurationCondition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}
	}


	public static class ParseConfigurationPhaseCondition implements ConfigurationCondition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}
	}


	@Import({RegisterBeanPhaseConditionConfiguration.class, BarConfiguration.class})
	public static class RegisterBeanPhaseImportingConfiguration {
	}


	@Import({ParseConfigurationPhaseConditionConfiguration.class, BarConfiguration.class})
	public static class ParseConfigurationPhaseImportingConfiguration {
	}


	@Import({UnconditionalConfiguration.class, BarConfiguration.class})
	public static class UnconditionalImportingConfiguration {
	}


	public static class BaseConfiguration {

		@Bean
		public String someBean() {
			return "foo";
		}
	}


	@Conditional(RegisterBeanPhaseCondition.class)
	public static class RegisterBeanPhaseConditionConfiguration extends BaseConfiguration {
	}


	@Conditional(ParseConfigurationPhaseCondition.class)
	public static class ParseConfigurationPhaseConditionConfiguration extends BaseConfiguration {
	}


	public static class UnconditionalConfiguration extends BaseConfiguration {
	}


	public static class BarConfiguration extends BaseConfiguration {
	}

}
