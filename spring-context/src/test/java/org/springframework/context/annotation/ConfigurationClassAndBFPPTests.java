/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests semantics of declaring {@link BeanFactoryPostProcessor}-returning @Bean
 * methods, specifically as regards static @Bean methods and the avoidance of
 * container lifecycle issues when BFPPs are in the mix.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ConfigurationClassAndBFPPTests {

	@Test
	public void autowiringFailsWithBFPPAsInstanceMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(TestBeanConfig.class, AutowiredConfigWithBFPPAsInstanceMethod.class);
		ctx.refresh();
		// instance method BFPP interferes with lifecycle -> autowiring fails!
		// WARN-level logging should have been issued about returning BFPP from non-static @Bean method
		assertThat(ctx.getBean(AutowiredConfigWithBFPPAsInstanceMethod.class).autowiredTestBean).isNull();
	}

	@Test
	public void autowiringSucceedsWithBFPPAsStaticMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(TestBeanConfig.class, AutowiredConfigWithBFPPAsStaticMethod.class);
		ctx.refresh();
		// static method BFPP does not interfere with lifecycle -> autowiring succeeds
		assertThat(ctx.getBean(AutowiredConfigWithBFPPAsStaticMethod.class).autowiredTestBean).isNotNull();
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
			return new BeanFactoryPostProcessor() {
				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
					// no-op
				}
			};
		}
	}


	@Configuration
	static class AutowiredConfigWithBFPPAsStaticMethod {
		@Autowired TestBean autowiredTestBean;

		@Bean
		public static final BeanFactoryPostProcessor bfpp() {
			return new BeanFactoryPostProcessor() {
				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
					// no-op
				}
			};
		}
	}


	@Test
	public void staticBeanMethodsDoNotRespectScoping() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithStaticBeanMethod.class);
		ctx.refresh();
		assertThat(ConfigWithStaticBeanMethod.testBean()).isNotSameAs(ConfigWithStaticBeanMethod.testBean());
	}


	@Configuration
	static class ConfigWithStaticBeanMethod {
		@Bean
		public static TestBean testBean() {
			return new TestBean("foo");
		}
	}

}
