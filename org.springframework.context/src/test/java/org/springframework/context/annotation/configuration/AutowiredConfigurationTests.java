package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import test.beans.Colour;
import test.beans.TestBean;


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
		System.setProperty("myProp", "foo");

		ClassPathXmlApplicationContext factory = new ClassPathXmlApplicationContext(
		        "ValueInjectionTests.xml", AutowiredConfigurationTests.class);

		TestBean testBean = factory.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("foo"));
	}

	@Configuration
	static class ValueConfig {

		@Value("#{systemProperties.myProp}")
		private String name = "default";

		@Bean
		public TestBean testBean() {
			return new TestBean(name);
		}
	}
}
