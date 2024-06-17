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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.Colour;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests covering use of {@link Autowired} and {@link Value} within
 * {@link Configuration} classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class AutowiredConfigurationTests {

	@Test
	void testAutowiredConfigurationDependencies() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				AutowiredConfigurationTests.class.getSimpleName() + ".xml", AutowiredConfigurationTests.class);

		assertThat(context.getBean("colour", Colour.class)).isEqualTo(Colour.RED);
		assertThat(context.getBean("testBean", TestBean.class).getName()).isEqualTo(Colour.RED.toString());
		context.close();
	}

	@Test
	void testAutowiredConfigurationMethodDependencies() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				AutowiredMethodConfig.class, ColorConfig.class);

		assertThat(context.getBean(Colour.class)).isEqualTo(Colour.RED);
		assertThat(context.getBean(TestBean.class).getName()).isEqualTo("RED-RED");
		context.close();
	}

	@Test
	void testAutowiredConfigurationMethodDependenciesWithOptionalAndAvailable() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				OptionalAutowiredMethodConfig.class, ColorConfig.class);

		assertThat(context.getBean(Colour.class)).isEqualTo(Colour.RED);
		assertThat(context.getBean(TestBean.class).getName()).isEqualTo("RED-RED");
		context.close();
	}

	@Test
	void testAutowiredConfigurationMethodDependenciesWithOptionalAndNotAvailable() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				OptionalAutowiredMethodConfig.class);

		assertThat(context.getBeansOfType(Colour.class)).isEmpty();
		assertThat(context.getBean(TestBean.class).getName()).isEmpty();
		context.close();
	}

	@Test
	void testAutowiredSingleConstructorSupported() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
				new ClassPathResource("annotation-config.xml", AutowiredConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(AutowiredConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh();
		assertThat(ctx.getBean(Colour.class)).isSameAs(ctx.getBean(AutowiredConstructorConfig.class).colour);
		ctx.close();
	}

	@Test
	void testObjectFactoryConstructorWithTypeVariable() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
				new ClassPathResource("annotation-config.xml", ObjectFactoryConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(ObjectFactoryConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh();
		assertThat(ctx.getBean(Colour.class)).isSameAs(ctx.getBean(ObjectFactoryConstructorConfig.class).colour);
		ctx.close();
	}

	@Test
	void testAutowiredAnnotatedConstructorSupported() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
				new ClassPathResource("annotation-config.xml", MultipleConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(MultipleConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh();
		assertThat(ctx.getBean(Colour.class)).isSameAs(ctx.getBean(MultipleConstructorConfig.class).colour);
		ctx.close();
	}

	@Test
	void testValueInjection() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ValueInjectionTests.xml", AutowiredConfigurationTests.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void testValueInjectionWithMetaAnnotation() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithMetaAnnotation.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void testValueInjectionWithAliasedMetaAnnotation() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithAliasedMetaAnnotation.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void testValueInjectionWithProviderFields() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderFields.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void testValueInjectionWithProviderConstructorArguments() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderConstructorArguments.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void testValueInjectionWithProviderMethodArguments() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderMethodArguments.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void testValueInjectionWithAccidentalAutowiredAnnotations() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithAccidentalAutowiredAnnotations.class);
		doTestValueInjection(context);
		context.close();
	}

	private void doTestValueInjection(BeanFactory context) {
		System.clearProperty("myProp");

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName()).isNull();

		testBean = context.getBean("testBean2", TestBean.class);
		assertThat(testBean.getName()).isNull();

		System.setProperty("myProp", "foo");

		testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName()).isEqualTo("foo");

		testBean = context.getBean("testBean2", TestBean.class);
		assertThat(testBean.getName()).isEqualTo("foo");

		System.clearProperty("myProp");

		testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName()).isNull();

		testBean = context.getBean("testBean2", TestBean.class);
		assertThat(testBean.getName()).isNull();
	}

	@Test
	void testCustomPropertiesWithClassPathContext() throws IOException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class);

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName()).isEqualTo("localhost");
		assertThat(testBean.getAge()).isEqualTo(contentLength());
		context.close();
	}

	@Test
	void testCustomPropertiesWithGenericContext() throws IOException {
		GenericApplicationContext context = new GenericApplicationContext();
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(
				new ClassPathResource("AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class));
		context.refresh();

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName()).isEqualTo("localhost");
		assertThat(testBean.getAge()).isEqualTo(contentLength());
		context.close();
	}

	private int contentLength() throws IOException {
		return (int) new ClassPathResource("do_not_delete_me.txt").contentLength();
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
				return new TestBean(colour.get() + "-" + colours.get().get(0).toString());
			}
		}
	}


	@Configuration
	static class AutowiredConstructorConfig {

		Colour colour;

		// @Autowired
		AutowiredConstructorConfig(Colour colour) {
			this.colour = colour;
		}
	}


	@Configuration
	static class ObjectFactoryConstructorConfig {

		Colour colour;

		// @Autowired
		ObjectFactoryConstructorConfig(ObjectFactory<Colour> colourFactory) {
			this.colour = colourFactory.getObject();
		}
	}


	@Configuration
	static class MultipleConstructorConfig {

		Colour colour;

		@Autowired
		MultipleConstructorConfig(Colour colour) {
			this.colour = colour;
		}

		MultipleConstructorConfig(String test) {
			this.colour = new Colour(test);
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


	@Value("#{systemProperties[myProp]}")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyProp {
	}


	@Configuration
	@Scope("prototype")
	static class ValueConfigWithMetaAnnotation {

		@MyProp
		private String name;

		private String name2;

		@MyProp
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


	@Value("")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AliasedProp {

		@AliasFor(annotation = Value.class)
		String value();
	}


	@Configuration
	@Scope("prototype")
	static class ValueConfigWithAliasedMetaAnnotation {

		@AliasedProp("#{systemProperties[myProp]}")
		private String name;

		private String name2;

		@AliasedProp("#{systemProperties[myProp]}")
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
	static class ValueConfigWithAccidentalAutowiredAnnotations implements InitializingBean {

		boolean invoked;

		@Override
		public void afterPropertiesSet() {
			Assert.state(!invoked, "Factory method must not get invoked on startup");
		}

		@Bean @Scope("prototype")
		@Autowired
		public TestBean testBean(@Value("#{systemProperties[myProp]}") Provider<String> name) {
			invoked = true;
			return new TestBean(name.get());
		}

		@Bean @Scope("prototype")
		@Autowired
		public TestBean testBean2(@Value("#{systemProperties[myProp]}") Provider<String> name2) {
			invoked = true;
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

		@Value("do_not_delete_me.txt")
		public void setResource(Resource resource) {
			this.resource = resource;
		}

		@Bean
		public TestBean testBean() throws IOException {
			return new TestBean(hostname, (int) resource.contentLength());
		}
	}

}
