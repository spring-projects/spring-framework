package test.basic;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.MalformedConfigurationException;
import org.springframework.config.java.StandardScopes;
import org.springframework.config.java.ext.Bean;
import org.springframework.config.java.support.ConfigurationClassPostProcessor;

import test.beans.ITestBean;
import test.beans.TestBean;


public class BasicTests {

	/**
	 * Creates a new {@link BeanFactory}, populates it with a {@link BeanDefinition} for
	 * each of the given {@link Configuration} <var>configClasses</var>, and then
	 * post-processes the factory using JavaConfig's {@link ConfigurationClassPostProcessor}.
	 * When complete, the factory is ready to service requests for any {@link Bean} methods
	 * declared by <var>configClasses</var>.
	 * 
	 * @param configClasses the {@link Configuration} classes under test. may be an empty
	 *        list.
	 * 
	 * @return fully initialized and post-processed {@link BeanFactory}
	 */
	private static BeanFactory initBeanFactory(Class<?>... configClasses) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		for (Class<?> configClass : configClasses) {
			String configBeanName = configClass.getName();
			factory.registerBeanDefinition(configBeanName, rootBeanDefinition(configClass)
			        .getBeanDefinition());
		}

		new ConfigurationClassPostProcessor().postProcessBeanFactory(factory);

		factory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());

		return factory;
	}


	@Test
	public void simplestPossibleConfiguration() {
		BeanFactory factory = initBeanFactory(SimplestPossibleConfig.class);

		String stringBean = factory.getBean("stringBean", String.class);

		assertThat(stringBean, equalTo("foo"));
	}

	@Configuration
	static class SimplestPossibleConfig {
		public @Bean
		String stringBean() {
			return "foo";
		}
	}


	@Test
	public void configurationWithPrototypeScopedBeans() {
		BeanFactory factory = initBeanFactory(ConfigWithPrototypeBean.class);

		TestBean foo = factory.getBean("foo", TestBean.class);
		ITestBean bar = factory.getBean("bar", ITestBean.class);
		ITestBean baz = factory.getBean("baz", ITestBean.class);

		assertThat(foo.getSpouse(), sameInstance(bar));
		assertThat(bar.getSpouse(), not(sameInstance(baz)));
	}

	@Configuration
	static class ConfigWithPrototypeBean {
		public @Bean
		TestBean foo() {
			TestBean foo = new TestBean("foo");
			foo.setSpouse(bar());
			return foo;
		}

		public @Bean
		TestBean bar() {
			TestBean bar = new TestBean("bar");
			bar.setSpouse(baz());
			return bar;
		}

		@Bean(scope = StandardScopes.PROTOTYPE)
		public TestBean baz() {
			return new TestBean("bar");
		}
	}


	@Test
	public void legalBeanOverriding() {
		{
			BeanFactory factory = initBeanFactory(ConfigWithBeanThatAllowsOverriding.class, ConfigWithBeanOverride.class);

			TestBean testBean = factory.getBean("testBean", TestBean.class);

			assertThat(testBean.getName(), equalTo("overridden"));
		}

		// now try it the other way around - order matters!

		{
			BeanFactory factory = initBeanFactory(ConfigWithBeanOverride.class, ConfigWithBeanThatAllowsOverriding.class);

			TestBean testBean = factory.getBean("testBean", TestBean.class);

			assertThat(testBean.getName(), equalTo("original"));
		}

	}
	
	@Test(expected=MalformedConfigurationException.class)
	public void illegalBeanOverriding() {
		initBeanFactory(ConfigWithBeanThatDisallowsOverriding.class, ConfigWithBeanOverride.class);
	}
	
	@Test
	public void illegalBeanOverriding2() {
		// should be okay when the class that disallows overriding is the one doing the overriding
		initBeanFactory(ConfigWithBeanOverride.class, ConfigWithBeanThatDisallowsOverriding.class);
	}

	@Configuration
	static class ConfigWithBeanThatAllowsOverriding {
		@Bean
		public TestBean testBean() {
			return new TestBean("original");
		}
	}
	
	@Configuration
	static class ConfigWithBeanThatDisallowsOverriding {
		@Bean(allowOverriding = false)
		public TestBean testBean() {
			return new TestBean("original");
		}
	}

	@Configuration
	static class ConfigWithBeanOverride {
		@Bean
		public TestBean testBean() {
			return new TestBean("overridden");
		}
	}
	
}
