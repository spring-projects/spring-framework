package org.springframework.context.annotation.configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.support.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import test.beans.TestBean;


public class AspectTests {
	private void assertAdviceWasApplied(Class<?> configClass) {
		GenericApplicationContext ctx = new GenericApplicationContext(
					new XmlBeanFactory(new ClassPathResource("aspectj-autoproxy-config.xml", AspectTests.class)));
		ctx.addBeanFactoryPostProcessor(new ConfigurationClassPostProcessor());
		ctx.registerBeanDefinition("config", new RootBeanDefinition(configClass));
		ctx.refresh();

		TestBean testBean = ctx.getBean("testBean", TestBean.class);
		assertThat(testBean.getName(), equalTo("name"));
		testBean.absquatulate();
		assertThat(testBean.getName(), equalTo("advisedName"));
	}

	@Test
	public void aspectAnnotatedConfiguration() {
		assertAdviceWasApplied(AspectConfig.class);
	}

	@Test
	public void configurationIncludesAspect() {
		assertAdviceWasApplied(ConfigurationWithAspect.class);
	}


	@Aspect
	@Configuration
	static class AspectConfig {
		@Bean
		public TestBean testBean() {
			return new TestBean("name");
		}

		@Before("execution(* test.beans.TestBean.absquatulate(..)) && target(testBean)")
		public void touchBean(TestBean testBean) {
			testBean.setName("advisedName");
		}
	}

	@Configuration
	static class ConfigurationWithAspect {
		@Bean
		public TestBean testBean() {
			return new TestBean("name");
		}

		@Bean
		public NameChangingAspect nameChangingAspect() {
			return new NameChangingAspect();
		}
	}

	@Aspect
	static class NameChangingAspect {
		@Before("execution(* test.beans.TestBean.absquatulate(..)) && target(testBean)")
		public void touchBean(TestBean testBean) {
			testBean.setName("advisedName");
		}
	}
}
