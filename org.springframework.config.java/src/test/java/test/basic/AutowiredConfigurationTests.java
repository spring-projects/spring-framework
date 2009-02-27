package test.basic;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.config.java.annotation.Configuration;
import org.springframework.config.java.ext.Bean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import test.beans.Colour;
import test.beans.TestBean;

public class AutowiredConfigurationTests {
	public @Test void test() {
		ClassPathXmlApplicationContext factory = new ClassPathXmlApplicationContext(
				AutowiredConfigurationTests.class.getSimpleName() + ".xml",
				AutowiredConfigurationTests.class);
		
		assertThat(factory.getBean("colour", Colour.class), equalTo(Colour.RED));
		assertThat(factory.getBean("testBean", TestBean.class).getName(), equalTo(Colour.RED.toString()));
	}
	
	@Configuration
	static class AutowiredConfig {
		private @Autowired Colour colour;
		
		public @Bean TestBean testBean() {
			return new TestBean(colour.toString());
		}
	}
	
	@Configuration
	static class ColorConfig {
		public @Bean Colour colour() { return Colour.RED; }
	}
	
}
