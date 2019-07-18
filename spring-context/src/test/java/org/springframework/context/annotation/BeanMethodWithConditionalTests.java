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

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Tobias Singhania
 *
 */
public class BeanMethodWithConditionalTests {

	/**
	 * Tests that a bean is not created if the bean factory method is annotated with a conditions that
	 * evaluates to false.
	 */
	@Test
	public void excludesBeanOnFalseCondition() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ExcludeBeanConfig.class);
		assertThat(context.containsBean("myBean"))
				.as("should exclude bean on false condition")
				.isTrue();
		context.close();
	}

	/**
	 * If a configuration class contains two bean factory methods with the same name, where the first declaration is
	 * annotated with a condition that evaluates to false, the bean should be created using the second factory method
	 * See issue #23307
	 */
	@Test
	public void includesBeanIfFirstBeanDeclarationIsDisabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DeclareExcludedBeanFirstConfig.class);
		MyBean bean = (MyBean) context.getBean("myBean");
		assertThat(bean.identifier).as("should provide bean which has true condition")
				.isEqualTo("Custom bean");
		context.close();
	}

	/**
	 * If a configuration class contains two bean factory methods with the same name, where the second declaration is
	 * annotated with a condition that evaluates to false, the bean should be created using the first factory method
	 * See issue #23307
	 */
	@Test
	public void includesBeanIfSecondBeanDeclarationIsDisabled() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DeclareIncludedBeanFirstConfig.class);
		MyBean bean = (MyBean) context.getBean("myBean");
		assertThat(bean.identifier).as("should provide bean which has true condition")
				.isEqualTo("Custom bean");
		context.close();
	}


	/**
	 * If a bean factory method with the same name is provided in two config classes, where the first declaration is
	 * annotated with a condition that evaluates to false, the bean should be created using the factory method from
	 * the second config
	 */
	@Test
	public void createsBeansWithSameNameInSeparateConfigs() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ExcludeBeanConfig.class, DeclareBeanConfig.class);
		MyBean bean = (MyBean) context.getBean("myBean");
		assertThat(bean.identifier).as("should provide bean which has true condition")
				.isEqualTo("Custom bean");
		context.close();
	}


	@Configuration
	static class DeclareBeanConfig {
		@Bean
		public MyBean myBean() {
			return new MyBean("Custom bean");
		}
	}

	@Configuration
	static class ExcludeBeanConfig {

		@Bean
		@Conditional(NeverCondition.class)
		public MyBean myBean() {
			return new MyBean();
		}
	}


	@Configuration
	static class DeclareExcludedBeanFirstConfig {

		@Bean
		@Conditional(NeverCondition.class)
		public MyBean myBean() {
			return new MyBean();
		}

		@Bean
		public MyBean myBean(ApplicationContext ctx) {
			return new MyBean("Custom bean");
		}
	}


	@Configuration
	static class DeclareIncludedBeanFirstConfig {

		@Bean
		public MyBean myBean() {
			return new MyBean("Custom bean");
		}

		@Bean
		@Conditional(NeverCondition.class)
		public MyBean myBean(ApplicationContext ctx) {
			return new MyBean();
		}
	}


	static class MyBean {
		String identifier = "Default Definition";

		MyBean() {
		}

		MyBean(String identifier) {
			this.identifier = identifier;
		}
	}


	static class NeverCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}
	}

}
