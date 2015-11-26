/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.Colour;
import org.springframework.tests.sample.beans.TestBean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				AutowiredConfigurationTests.class.getSimpleName() + ".xml", AutowiredConfigurationTests.class);

		assertThat(context.getBean("colour", Colour.class), equalTo(Colour.RED));
		assertThat(context.getBean("testBean", TestBean.class).getName(), equalTo(Colour.RED.toString()));
	}

	@Test
	public void testAutowiredConfigurationMethodDependencies() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				AutowiredMethodConfig.class, ColorConfig.class);

		assertThat(context.getBean(Colour.class), equalTo(Colour.RED));
		assertThat(context.getBean(TestBean.class).getName(), equalTo("RED-RED"));
	}

	@Test
	public void testAutowiredConfigurationMethodDependenciesWithOptionalAndAvailable() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				OptionalAutowiredMethodConfig.class, ColorConfig.class);

		assertThat(context.getBean(Colour.class), equalTo(Colour.RED));
		assertThat(context.getBean(TestBean.class).getName(), equalTo("RED-RED"));
	}

	@Test
	public void testAutowiredConfigurationMethodDependenciesWithOptionalAndNotAvailable() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				OptionalAutowiredMethodConfig.class);

		assertTrue(context.getBeansOfType(Colour.class).isEmpty());
		assertThat(context.getBean(TestBean.class).getName(), equalTo(""));
	}

	@Test
	public void testAutowiredConfigurationConstructorsAreSupported() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
				new ClassPathResource("annotation-config.xml", AutowiredConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(AutowiredConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh();
		assertSame(ctx.getBean(AutowiredConstructorConfig.class).colour,
				ctx.getBean(Colour.class));
	}

	@Test
	public void testValueInjection() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ValueInjectionTests.xml", AutowiredConfigurationTests.class);
		doTestValueInjection(context);
	}

	@Test
	public void testValueInjectionWithProviderFields() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderFields.class);
		doTestValueInjection(context);
	}

	@Test
	public void testValueInjectionWithProviderConstructorArguments() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderConstructorArguments.class);
		doTestValueInjection(context);
	}

	@Test
	public void testValueInjectionWithProviderMethodArguments() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderMethodArguments.class);
		doTestValueInjection(context);
	}

	private void doTestValueInjection(BeanFactory context) {
		System.clearProperty("myProp");

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertNull(testBean.getName());

		testBean = context.getBean("testBean2", TestBean.class);
		assertNull(testBean.getName());

		System.setProperty("myProp", "foo");

		testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("foo"));

		testBean = context.getBean("testBean2", TestBean.class);
		assertThat(testBean.getName(), equalTo("foo"));

		System.clearProperty("myProp");

		testBean = context.getBean("testBean", TestBean.class);
		assertNull(testBean.getName());

		testBean = context.getBean("testBean2", TestBean.class);
		assertNull(testBean.getName());
	}

	@Test
	public void testCustomPropertiesWithClassPathContext() throws IOException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class);

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("localhost"));
		assertThat(testBean.getAge(), equalTo((int) new ClassPathResource("log4j.properties").contentLength()));
	}

	@Test
	public void testCustomPropertiesWithGenericContext() throws IOException {
		GenericApplicationContext context = new GenericApplicationContext();
		// context.setResourceLoader(new FileSystemResourceLoader());
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(
				new ClassPathResource("AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class));
		context.refresh();

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("localhost"));
		assertThat(testBean.getAge(), equalTo((int) new ClassPathResource("log4j.properties").contentLength()));
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
	static class AutowiredMethodConfig {

		@Bean
		public TestBean testBean(Colour colour, List<Colour> colours) {
			return new TestBean(colour.toString() + "-" + colours.get(0).toString());
		}
	}


	@Configuration
	static class OptionalAutowiredMethodConfig {

		@Bean
		public TestBean testBean(Optional<Colour> colour, Optional<List<Colour>> colours) {
			if (!colour.isPresent() && !colours.isPresent()) {
				return new TestBean("");
			}
			else {
				return new TestBean(colour.get().toString() + "-" + colours.get().get(0).toString());
			}
		}
	}


	@Configuration
	static class AutowiredConstructorConfig {

		Colour colour;

		@Autowired
		AutowiredConstructorConfig(Colour colour) {
			this.colour = colour;
		}
	}


	@Configuration
	static class ColorConfig {

		@Bean
		public Colour colour() {
			return Colour.RED;
		}
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


	@Configuration
	static class ValueConfigWithProviderFields {

		@Value("#{systemProperties[myProp]}")
		private Provider<String> name;

		private Provider<String> name2;

		@Value("#{systemProperties[myProp]}")
		public void setName2(Provider<String> name) {
			this.name2 = name;
		}

		@Bean @Scope("prototype")
		public TestBean testBean() {
			return new TestBean(name.get());
		}

		@Bean @Scope("prototype")
		public TestBean testBean2() {
			return new TestBean(name2.get());
		}
	}


	static class ValueConfigWithProviderConstructorArguments {

		private final Provider<String> name;

		private final Provider<String> name2;

		@Autowired
		public ValueConfigWithProviderConstructorArguments(@Value("#{systemProperties[myProp]}") Provider<String> name,
				@Value("#{systemProperties[myProp]}") Provider<String> name2) {
			this.name = name;
			this.name2 = name2;
		}

		@Bean @Scope("prototype")
		public TestBean testBean() {
			return new TestBean(name.get());
		}

		@Bean @Scope("prototype")
		public TestBean testBean2() {
			return new TestBean(name2.get());
		}
	}


	@Configuration
	static class ValueConfigWithProviderMethodArguments {

		@Bean @Scope("prototype")
		public TestBean testBean(@Value("#{systemProperties[myProp]}") Provider<String> name) {
			return new TestBean(name.get());
		}

		@Bean @Scope("prototype")
		public TestBean testBean2(@Value("#{systemProperties[myProp]}") Provider<String> name2) {
			return new TestBean(name2.get());
		}
	}


	@Configuration
	static class PropertiesConfig {

		private String hostname;

		private Resource resource;

		@Value("#{myProps.hostname}")
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		@Value("log4j.properties")
		public void setResource(Resource resource) {
			this.resource = resource;
		}

		@Bean
		public TestBean testBean() throws IOException {
			return new TestBean(hostname, (int) resource.contentLength());
		}
	}

}
