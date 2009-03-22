package test.basic;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.Bean;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.StandardScopes;
import org.springframework.config.java.support.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Scope;

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
			factory.registerBeanDefinition(configBeanName, rootBeanDefinition(configClass).getBeanDefinition());
		}

		new ConfigurationClassPostProcessor().postProcessBeanFactory(factory);

		factory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());

		return factory;
	}

	@Test
	public void customBeanNameIsRespected() {
		BeanFactory factory = initBeanFactory(ConfigWithBeanWithCustomName.class);
		assertSame(factory.getBean("customName"), ConfigWithBeanWithCustomName.testBean);

		// method name should not be registered
		try {
			factory.getBean("methodName");
			fail("bean should not have been registered with 'methodName'");
		} catch (NoSuchBeanDefinitionException ex) { /* expected */ }
	}

	@Configuration
	static class ConfigWithBeanWithCustomName {
		static TestBean testBean = new TestBean();
		@Bean(name="customName")
		public TestBean methodName() {
			return testBean;
		}
	}

	@Test
	public void aliasesAreRespected() {
		BeanFactory factory = initBeanFactory(ConfigWithBeanWithAliases.class);
		assertSame(factory.getBean("name1"), ConfigWithBeanWithAliases.testBean);
		String[] aliases = factory.getAliases("name1");
		for(String alias : aliases)
			assertSame(factory.getBean(alias), ConfigWithBeanWithAliases.testBean);

		// method name should not be registered
		try {
			factory.getBean("methodName");
			fail("bean should not have been registered with 'methodName'");
		} catch (NoSuchBeanDefinitionException ex) { /* expected */ }
	}

	@Configuration
	static class ConfigWithBeanWithAliases {
		static TestBean testBean = new TestBean();
		@Bean(name={"name1", "alias1", "alias2", "alias3"})
		public TestBean methodName() {
			return testBean;
		}
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testFinalBeanMethod() {
		initBeanFactory(ConfigWithFinalBean.class);
	}

	@Configuration
	static class ConfigWithFinalBean {
		public final @Bean TestBean testBean() {
			return new TestBean();
		}
	}

	@Test
	public void simplestPossibleConfiguration() {
		BeanFactory factory = initBeanFactory(SimplestPossibleConfig.class);

		String stringBean = factory.getBean("stringBean", String.class);

		assertThat(stringBean, equalTo("foo"));
	}

	@Configuration
	static class SimplestPossibleConfig {
		public @Bean String stringBean() {
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
		public @Bean TestBean foo() {
			TestBean foo = new TestBean("foo");
			foo.setSpouse(bar());
			return foo;
		}

		public @Bean TestBean bar() {
			TestBean bar = new TestBean("bar");
			bar.setSpouse(baz());
			return bar;
		}

		@Bean @Scope(StandardScopes.PROTOTYPE)
		public TestBean baz() {
			return new TestBean("bar");
		}
	}

}
