/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
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
 */
class ImportWithConditionTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	void conditionalThenUnconditional() {
		this.context.register(ConditionalThenUnconditional.class);
		this.context.refresh();
		assertThat(this.context.containsBean("beanTwo")).isFalse();
		assertThat(this.context.containsBean("beanOne")).isTrue();
	}

	@Test
	void unconditionalThenConditional() {
		this.context.register(UnconditionalThenConditional.class);
		this.context.refresh();
		assertThat(this.context.containsBean("beanTwo")).isFalse();
		assertThat(this.context.containsBean("beanOne")).isTrue();
	}


	@Configuration
	@Import({ConditionalConfiguration.class, UnconditionalConfiguration.class})
	protected static class ConditionalThenUnconditional {

		@Autowired
		@SuppressWarnings("unused")
		private BeanOne beanOne;
	}


	@Configuration
	@Import({UnconditionalConfiguration.class, ConditionalConfiguration.class})
	protected static class UnconditionalThenConditional {

		@Autowired
		@SuppressWarnings("unused")
		private BeanOne beanOne;
	}


	@Configuration
	@Import(BeanProvidingConfiguration.class)
	protected static class UnconditionalConfiguration {
	}


	@Configuration
	@Conditional(NeverMatchingCondition.class)
	@Import(BeanProvidingConfiguration.class)
	protected static class ConditionalConfiguration {
	}


	@Configuration
	protected static class BeanProvidingConfiguration {

		@Bean
		BeanOne beanOne() {
			return new BeanOne();
		}
	}


	private static final class BeanOne {
	}


	private static final class NeverMatchingCondition implements ConfigurationCondition {

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
