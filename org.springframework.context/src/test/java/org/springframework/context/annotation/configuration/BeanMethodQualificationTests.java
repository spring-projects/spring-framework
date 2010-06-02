package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import test.beans.TestBean;

/**
 * Tests proving that @Qualifier annotations work when used 
 * with @Configuration classes on @Bean methods.
 * 
 * @author Chris Beams
 */
public class BeanMethodQualificationTests {

	@Test
	public void test() {
		ApplicationContext ctx =
			new AnnotationConfigApplicationContext(Config.class, Pojo.class);
		Pojo pojo = ctx.getBean(Pojo.class);
		assertThat(pojo.testBean.getName(), equalTo("interesting"));
	}
	
	@Configuration
	static class Config {
		@Bean
		@Qualifier("interesting")
		public TestBean testBean1() {
			return new TestBean("interesting");
		}
		
		@Bean
		@Qualifier("boring")
		public TestBean testBean2() {
			return new TestBean("boring");
		}
	}
	
	@Component
	static class Pojo {
		@Autowired @Qualifier("interesting") TestBean testBean;
	}
}
