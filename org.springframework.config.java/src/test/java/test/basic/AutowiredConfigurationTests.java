package test.basic;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.config.java.Bean;
import org.springframework.config.java.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import test.beans.Colour;
import test.beans.TestBean;


public class AutowiredConfigurationTests {
	public @Test
	void test() {
		ClassPathXmlApplicationContext factory = new ClassPathXmlApplicationContext(
		        AutowiredConfigurationTests.class.getSimpleName() + ".xml", AutowiredConfigurationTests.class);

		assertThat(factory.getBean("colour", Colour.class), equalTo(Colour.RED));
		assertThat(factory.getBean("testBean", TestBean.class).getName(), equalTo(Colour.RED.toString()));
	}

	@Configuration
	static class AutowiredConfig {
		private @Autowired
		Colour colour;

		public @Bean
		TestBean testBean() {
			return new TestBean(colour.toString());
		}
	}

	@Configuration
	static class ColorConfig {
		public @Bean
		Colour colour() {
			return Colour.RED;
		}
	}


	public @Test
	void testValueInjection() {
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

		public @Bean
		TestBean testBean() {
			return new TestBean(name);
		}
	}
}
