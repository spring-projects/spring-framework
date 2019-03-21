/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * A configuration class that registers a placeholder configurer @Bean method
 * cannot also have @Value fields.  Logically, the config class must be instantiated
 * in order to invoke the placeholder configurer bean method, and it is a
 * chicken-and-egg problem to process the @Value field.
 *
 * Therefore, placeholder configurers should be put in separate configuration classes
 * as has been done in the test below. Simply said, placeholder configurer @Bean methods
 * and @Value fields in the same configuration class are mutually exclusive.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ConfigurationClassWithPlaceholderConfigurerBeanTests {

	/**
	 * Intentionally ignored test proving that a property placeholder bean
	 * cannot be declared in the same configuration class that has a @Value
	 * field in need of placeholder replacement. It's an obvious chicken-and-egg issue.
	 * The solution is to do as {@link #valueFieldsAreProcessedWhenPlaceholderConfigurerIsSegregated()}
	 * does and segregate the two bean definitions across configuration classes.
	 */
	@Ignore @Test
	public void valueFieldsAreNotProcessedWhenPlaceholderConfigurerIsIntegrated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithValueFieldAndPlaceholderConfigurer.class);
		System.setProperty("test.name", "foo");
		ctx.refresh();
		System.clearProperty("test.name");

		TestBean testBean = ctx.getBean(TestBean.class);
		assertThat(testBean.getName(), nullValue());
	}

	@Test
	public void valueFieldsAreProcessedWhenPlaceholderConfigurerIsSegregated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithValueField.class);
		ctx.register(ConfigWithPlaceholderConfigurer.class);
		System.setProperty("test.name", "foo");
		ctx.refresh();
		System.clearProperty("test.name");

		TestBean testBean = ctx.getBean(TestBean.class);
		assertThat(testBean.getName(), equalTo("foo"));
	}

	@Test
	public void valueFieldsResolveToPlaceholderSpecifiedDefaultValue() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithValueField.class);
		ctx.register(ConfigWithPlaceholderConfigurer.class);
		ctx.refresh();

		TestBean testBean = ctx.getBean(TestBean.class);
		assertThat(testBean.getName(), equalTo("bar"));
	}


	@Configuration
	static class ConfigWithValueField {

		@Value("${test.name:bar}")
		private String name;

		@Bean
		public ITestBean testBean() {
			return new TestBean(this.name);
		}
	}


	@Configuration
	static class ConfigWithPlaceholderConfigurer {

		@Bean
		public PropertySourcesPlaceholderConfigurer ppc() {
			return new PropertySourcesPlaceholderConfigurer();
		}
	}


	@Configuration
	static class ConfigWithValueFieldAndPlaceholderConfigurer {

		@Value("${test.name}")
		private String name;

		@Bean
		public ITestBean testBean() {
			return new TestBean(this.name);
		}

		@Bean
		public PropertySourcesPlaceholderConfigurer ppc() {
			return new PropertySourcesPlaceholderConfigurer();
		}
	}

}
