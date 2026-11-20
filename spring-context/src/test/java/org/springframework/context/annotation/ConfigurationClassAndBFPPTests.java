/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests semantics of declaring {@link BeanFactoryPostProcessor}-returning @Bean
 * methods, specifically as regards static @Bean methods and the avoidance of
 * container lifecycle issues when BFPPs are in the mix.
 *
 * @author Chris Beams
 * @since 3.1
 */
class ConfigurationClassAndBFPPTests {

	@Test
	void autowiringFailsWithBFPPAsInstanceMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(TestBeanConfig.class, AutowiredConfigWithBFPPAsInstanceMethod.class);
		ctx.refresh();
		// instance method BFPP interferes with lifecycle -> autowiring fails!
		// WARN-level logging should have been issued about returning BFPP from non-static @Bean method
		assertThat(ctx.getBean(AutowiredConfigWithBFPPAsInstanceMethod.class).autowiredTestBean).isNull();
		ctx.close();
	}

	@Test
	void autowiringSucceedsWithBFPPAsStaticMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(TestBeanConfig.class, AutowiredConfigWithBFPPAsStaticMethod.class);
		ctx.refresh();
		// static method BFPP does not interfere with lifecycle -> autowiring succeeds
		assertThat(ctx.getBean(AutowiredConfigWithBFPPAsStaticMethod.class).autowiredTestBean).isNotNull();
		ctx.close();
	}

	@Test
	void staticBeanMethodsDoNotRespectScoping() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithStaticBeanMethod.class);
		assertThat(ConfigWithStaticBeanMethod.testBean()).isNotSameAs(ConfigWithStaticBeanMethod.testBean());
		ctx.close();
	}


	@Configuration
	static class TestBeanConfig {
		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}

	@Configuration
	static class AutowiredConfigWithBFPPAsInstanceMethod {
		@Autowired TestBean autowiredTestBean;

		@Bean
		public BeanFactoryPostProcessor bfpp() {
			return beanFactory -> {
				// no-op
			};
		}
	}

	@Configuration
	static class AutowiredConfigWithBFPPAsStaticMethod {
		@Autowired TestBean autowiredTestBean;

		@Bean
		public static BeanFactoryPostProcessor bfpp() {
			return beanFactory -> {
				// no-op
			};
		}
	}

	@Configuration
	static class ConfigWithStaticBeanMethod {
		@Bean
		public static TestBean testBean() {
			return new TestBean("foo");
		}
	}

}
