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

package org.springframework.context.annotation.configuration;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.Color;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
	void autowiredConfigurationDependencies() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				AutowiredConfigurationTests.class.getSimpleName() + ".xml", AutowiredConfigurationTests.class);

		assertThat(context.getBean("color", Color.class)).isEqualTo(Color.RED);
		assertThat(context.getBean("testBean", TestBean.class).getName()).isEqualTo(Color.RED.toString());
		context.close();
	}

	@Test
	void autowiredConfigurationMethodDependencies() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				AutowiredMethodConfig.class, ColorConfig.class);

		assertThat(context.getBean(Color.class)).isEqualTo(Color.RED);
		assertThat(context.getBean(TestBean.class).getName()).isEqualTo("RED-RED");
		context.close();
	}

	@Test
	void autowiredConfigurationMethodDependenciesWithOptionalAndAvailable() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				OptionalAutowiredMethodConfig.class, ColorConfig.class);

		assertThat(context.getBean(Color.class)).isEqualTo(Color.RED);
		assertThat(context.getBean(TestBean.class).getName()).isEqualTo("RED-RED");
		context.close();
	}

	@Test
	void autowiredConfigurationMethodDependenciesWithOptionalAndNotAvailable() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				OptionalAutowiredMethodConfig.class);

		assertThat(context.getBeansOfType(Color.class)).isEmpty();
		assertThat(context.getBean(TestBean.class).getName()).isEmpty();
		context.close();
	}

	@Test
	void autowiredConfigurationMethodDependenciesWithQualifier() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				QualifiedAutowiredMethodConfig.class);

		assertThat(context.getBeansOfType(Color.class)).isEmpty();
		assertThat(context.getBean(TestBean.class).getName()).isEmpty();
		context.close();
	}

	@Test
	void autowiredSingleConstructorSupported() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
				new ClassPathResource("annotation-config.xml", AutowiredConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(AutowiredConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh();
		assertThat(ctx.getBean(Color.class)).isSameAs(ctx.getBean(AutowiredConstructorConfig.class).color);
		ctx.close();
	}

	@Test
	void objectFactoryConstructorWithTypeVariable() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
				new ClassPathResource("annotation-config.xml", ObjectFactoryConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(ObjectFactoryConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh();
		assertThat(ctx.getBean(Color.class)).isSameAs(ctx.getBean(ObjectFactoryConstructorConfig.class).color);
		ctx.close();
	}

	@Test
	void autowiredAnnotatedConstructorSupported() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(
				new ClassPathResource("annotation-config.xml", MultipleConstructorConfig.class));
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.registerBeanDefinition("config1", new RootBeanDefinition(MultipleConstructorConfig.class));
		ctx.registerBeanDefinition("config2", new RootBeanDefinition(ColorConfig.class));
		ctx.refresh();
		assertThat(ctx.getBean(Color.class)).isSameAs(ctx.getBean(MultipleConstructorConfig.class).color);
		ctx.close();
	}

	@Test
	void valueInjection() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ValueInjectionTests.xml", AutowiredConfigurationTests.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void valueInjectionWithMetaAnnotation() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithMetaAnnotation.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void valueInjectionWithAliasedMetaAnnotation() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithAliasedMetaAnnotation.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void valueInjectionWithProviderFields() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderFields.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void valueInjectionWithProviderConstructorArguments() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderConstructorArguments.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void valueInjectionWithProviderMethodArguments() {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ValueConfigWithProviderMethodArguments.class);
		doTestValueInjection(context);
		context.close();
	}

	@Test
	void valueInjectionWithAccidentalAutowiredAnnotations() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
				new AnnotationConfigApplicationContext(ValueConfigWithAccidentalAutowiredAnnotations.class));
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
	void customPropertiesWithClassPathContext() throws IOException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class);

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName()).isEqualTo("localhost");
		assertThat(testBean.getAge()).isEqualTo(contentLength());
		context.close();
	}

	@Test
	void customPropertiesWithGenericContext() throws IOException {
		GenericApplicationContext context = new GenericApplicationContext();
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(
				new ClassPathResource("AutowiredConfigurationTests-custom.xml", AutowiredConfigurationTests.class));
		context.refresh();

		TestBean testBean = context.getBean("testBean", TestBean.class);
		assertThat(testBean.getName()).isEqualTo("localhost");
		assertThat(testBean.getAge()).isEqualTo(contentLength());
		context.close();
	}

	@Test
	void valueInjectionWithRecord() {
		System.setProperty("recordBeanName", "enigma");
		try (GenericApplicationContext context = new AnnotationConfigApplicationContext(RecordBean.class)) {
			assertThat(context.getBean(RecordBean.class).name()).isEqualTo("enigma");
		}
		finally {
			System.clearProperty("recordBeanName");
		}
	}

	private int contentLength() throws IOException {
		return (int) new ClassPathResource("do_not_delete_me.txt").contentLength();
	}


	@Configuration
	static class AutowiredConfig {

		@Autowired
		private Color color;

		@Bean
		public TestBean testBean() {
			return new TestBean(color.toString());
		}
	}


	@Configuration
	static class AutowiredMethodConfig {

		@Bean
		public TestBean testBean(Color color, List<Color> colors) {
			return new TestBean(color + "-" + colors.get(0));
		}
	}


	@Configuration
	static class OptionalAutowiredMethodConfig {

		@Bean
		public TestBean testBean(Optional<Color> color, Optional<List<Color>> colors) {
			if (color.isEmpty() && colors.isEmpty()) {
				return new TestBean("");
			}
			else {
				return new TestBean(color.get() + "-" + colors.get().get(0));
			}
		}
	}


	@Configuration
	static class QualifiedAutowiredMethodConfig {

		@Bean
		@Qualifier("testBean")
		public TestBean testBean(Optional<Color> color, Optional<List<Color>> colors) {
			if (!color.isEmpty() || !colors.isEmpty()) {
				throw new IllegalStateException("Unexpected match: " + color + " " + colors);
			}
			return new TestBean("");
		}

		@Bean
		public List<?> someList() {
			return Collections.singletonList(new TestBean("shouldNotMatch"));
		}
	}


	@Configuration
	static class AutowiredConstructorConfig {

		Color color;

		// @Autowired
		AutowiredConstructorConfig(Color color) {
			this.color = color;
		}
	}


	@Configuration
	static class ObjectFactoryConstructorConfig {

		Color color;

		// @Autowired
		ObjectFactoryConstructorConfig(ObjectFactory<Color> colorFactory) {
			this.color = colorFactory.getObject();
		}
	}


	@Configuration
	static class MultipleConstructorConfig {

		Color color;

		@Autowired
		MultipleConstructorConfig(Color color) {
			this.color = color;
		}

		MultipleConstructorConfig(String test) {
			this.color = Color.BLUE;
		}
	}


	@Configuration
	static class ColorConfig {

		@Bean
		public Color color() {
			return Color.RED;
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


	record RecordBean(@Value("${recordBeanName}") String name) {
	}

}
