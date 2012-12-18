/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import test.beans.Colour;
import test.beans.TestBean;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * System tests covering use of {@link Autowired} and {@link Value} within
 * {@link Configuration} classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class AutowiredConfigurationTests {

	@Test
	public void testAutowiredConfigurationDependencies() {
		ClassPathXmlApplicationContext factory = new ClassPathXmlApplicationContext(
		        AutowiredConfigurationTests.class.getSimpleName() + ".xml", AutowiredConfigurationTests.class);

		assertThat(factory.getBean("colour", Colour.class), equalTo(Colour.RED));
		assertThat(factory.getBean("testBean", TestBean.class).getName(), equalTo(Colour.RED.toString()));
	}

	@Configuration
	static class AutowiredConfig {
		@Autowired
		private Colour colour;

		@Bean
		public TestBean testBean() {
			return new TestBean(colour.toString());
		}
	}

	@Configuration
	static class ColorConfig {

		@Bean
		public Colour colour() {
			return Colour.RED;
		}
	}


	/**
	 * {@link Autowired} constructors are not supported on {@link Configuration} classes
	 * due to CGLIB constraints
	 */
	@Test(expected=BeanCreationException.class)
	public void testAutowiredConfigurationConstructorsAreNotSupported() {
		XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("annotation-config.xml", AutowiredConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(AutowiredConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh(); // should throw
	}

	@Configuration
	static class AutowiredConstructorConfig {
		Colour colour;

		@Autowired
		AutowiredConstructorConfig(Colour colour) {
			this.colour = colour;
		}
	}


	@Test
	public void testValueInjection() {
		ClassPathXmlApplicationContext factory = new ClassPathXmlApplicationContext(
		        "ValueInjectionTests.xml", AutowiredConfigurationTests.class);

		System.clearProperty("myProp");

		TestBean testBean = factory.getBean("testBean", TestBean.class);
		assertNull(testBean.getName());

		testBean = factory.getBean("testBean2", TestBean.class);
		assertNull(testBean.getName());

		System.setProperty("myProp", "foo");

		testBean = factory.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("foo"));

		testBean = factory.getBean("testBean2", TestBean.class);
		assertThat(testBean.getName(), equalTo("foo"));

		System.clearProperty("myProp");

		testBean = factory.getBean("testBean", TestBean.class);
		assertNull(testBean.getName());

		testBean = factory.getBean("testBean2", TestBean.class);
		assertNull(testBean.getName());
	}

	@Configuration
	static class ValueConfig {

		@Value("#{systemProperties[myProp]}")
		private String name;

		private String name2;

		@Value("#{systemProperties[myProp]}")
		public void setName2(String name) {
			this.name2 = name;
		}

		@Bean @Scope("prototype")
		public TestBean testBean() {
			return new TestBean(name);
		}

		@Bean @Scope("prototype")
		public TestBean testBean2() {
			return new TestBean(name2);
		}
	}


	@Test
	public void testCustomProperties() {
		ClassPathXmlApplicationContext factory = new ClassPathXmlApplicationContext(
		        "AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class);

		TestBean testBean = factory.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("localhost"));
	}

	@Configuration
	static class PropertiesConfig {

		private String hostname;

		@Value("#{myProps.hostname}")
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		@Bean
		public TestBean testBean() {
			return new TestBean(hostname);
		}
	}

}
