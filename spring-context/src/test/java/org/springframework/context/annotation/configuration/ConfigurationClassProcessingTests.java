/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import test.beans.ITestBean;
import test.beans.TestBean;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.Assert.*;

/**
 * Miscellaneous system tests covering {@link Bean} naming, aliases, scoping and error
 * handling within {@link Configuration} class definitions.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ConfigurationClassProcessingTests {

	/**
	 * Creates a new {@link BeanFactory}, populates it with a {@link BeanDefinition} for
	 * each of the given {@link Configuration} <var>configClasses</var>, and then
	 * post-processes the factory using JavaConfig's {@link ConfigurationClassPostProcessor}.
	 * When complete, the factory is ready to service requests for any {@link Bean} methods
	 * declared by <var>configClasses</var>.
	 */
	private ListableBeanFactory initBeanFactory(Class<?>... configClasses) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		for (Class<?> configClass : configClasses) {
			String configBeanName = configClass.getName();
			factory.registerBeanDefinition(configBeanName, new RootBeanDefinition(configClass));
		}
		ConfigurationClassPostProcessor ccpp = new ConfigurationClassPostProcessor();
		ccpp.postProcessBeanDefinitionRegistry(factory);
		ccpp.postProcessBeanFactory(factory);
		RequiredAnnotationBeanPostProcessor rapp = new RequiredAnnotationBeanPostProcessor();
		rapp.setBeanFactory(factory);
		factory.addBeanPostProcessor(rapp);
		return factory;
	}


	@Test
	public void customBeanNameIsRespected() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
		ac.registerBeanDefinition("config", new RootBeanDefinition(ConfigWithBeanWithCustomName.class));
		ac.refresh();
		assertSame(ac.getBean("customName"), ConfigWithBeanWithCustomName.testBean);

		// method name should not be registered
		try {
			ac.getBean("methodName");
			fail("bean should not have been registered with 'methodName'");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
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

	@Test(expected=BeanDefinitionParsingException.class)
	public void testFinalBeanMethod() {
		initBeanFactory(ConfigWithFinalBean.class);
	}

	@Test
	public void simplestPossibleConfiguration() {
		BeanFactory factory = initBeanFactory(SimplestPossibleConfig.class);
		String stringBean = factory.getBean("stringBean", String.class);
		assertEquals(stringBean, "foo");
	}

	@Test
	public void configWithObjectReturnType() {
		BeanFactory factory = initBeanFactory(ConfigWithNonSpecificReturnTypes.class);
		assertEquals(Object.class, factory.getType("stringBean"));
		assertFalse(factory.isTypeMatch("stringBean", String.class));
		String stringBean = factory.getBean("stringBean", String.class);
		assertEquals(stringBean, "foo");
	}

	@Test
	public void configWithFactoryBeanReturnType() {
		ListableBeanFactory factory = initBeanFactory(ConfigWithNonSpecificReturnTypes.class);
		assertEquals(List.class, factory.getType("factoryBean"));
		assertTrue(factory.isTypeMatch("factoryBean", List.class));
		assertEquals(FactoryBean.class, factory.getType("&factoryBean"));
		assertTrue(factory.isTypeMatch("&factoryBean", FactoryBean.class));
		assertFalse(factory.isTypeMatch("&factoryBean", BeanClassLoaderAware.class));
		assertFalse(factory.isTypeMatch("&factoryBean", ListFactoryBean.class));
		assertTrue(factory.getBean("factoryBean") instanceof List);

		String[] beanNames = factory.getBeanNamesForType(FactoryBean.class);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = factory.getBeanNamesForType(BeanClassLoaderAware.class);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = factory.getBeanNamesForType(ListFactoryBean.class);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = factory.getBeanNamesForType(List.class);
		assertEquals("factoryBean", beanNames[0]);
	}

	@Test
	public void configurationWithPrototypeScopedBeans() {
		BeanFactory factory = initBeanFactory(ConfigWithPrototypeBean.class);

		TestBean foo = factory.getBean("foo", TestBean.class);
		ITestBean bar = factory.getBean("bar", ITestBean.class);
		ITestBean baz = factory.getBean("baz", ITestBean.class);

		assertSame(foo.getSpouse(), bar);
		assertNotSame(bar.getSpouse(), baz);
	}

	@Test
	public void configurationWithPostProcessor() {
		AnnotationConfigApplicationContext factory = new AnnotationConfigApplicationContext();
		factory.register(ConfigWithPostProcessor.class);
		RootBeanDefinition placeholderConfigurer = new RootBeanDefinition(PropertyPlaceholderConfigurer.class);
		placeholderConfigurer.getPropertyValues().add("properties", "myProp=myValue");
		factory.registerBeanDefinition("placeholderConfigurer", placeholderConfigurer);
		factory.refresh();

		TestBean foo = factory.getBean("foo", TestBean.class);
		ITestBean bar = factory.getBean("bar", ITestBean.class);
		ITestBean baz = factory.getBean("baz", ITestBean.class);

		assertEquals("foo-processed-myValue", foo.getName());
		assertEquals("bar-processed-myValue", bar.getName());
		assertEquals("baz-processed-myValue", baz.getName());

		SpousyTestBean listener = factory.getBean("listenerTestBean", SpousyTestBean.class);
		assertTrue(listener.refreshed);
	}


	@Configuration
	static class ConfigWithBeanWithCustomName {
		static TestBean testBean = new TestBean();
		@Bean(name="customName")
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithFinalBean {
		public final @Bean TestBean testBean() {
			return new TestBean();
		}
	}


	@Configuration
	static class SimplestPossibleConfig {
		public @Bean String stringBean() {
			return "foo";
		}
	}


	@Configuration
	static class ConfigWithNonSpecificReturnTypes {
		public @Bean Object stringBean() {
			return "foo";
		}
		public @Bean FactoryBean factoryBean() {
			ListFactoryBean fb = new ListFactoryBean();
			fb.setSourceList(Arrays.asList("element1", "element2"));
			return fb;
		}
	}


	@Configuration
	static class ConfigWithBeanWithAliases {

		static TestBean testBean = new TestBean();

		@Bean(name={"name1", "alias1", "alias2", "alias3"})
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithPrototypeBean {

		public @Bean TestBean foo() {
			TestBean foo = new SpousyTestBean("foo");
			foo.setSpouse(bar());
			return foo;
		}

		public @Bean TestBean bar() {
			TestBean bar = new SpousyTestBean("bar");
			bar.setSpouse(baz());
			return bar;
		}

		@Bean @Scope("prototype")
		public TestBean baz() {
			return new TestBean("baz");
		}
	}


	@SuppressWarnings("unused")
	static class ConfigWithPostProcessor extends ConfigWithPrototypeBean {

		@Value("${myProp}")
		private String myProp;

		@Bean
		public POBPP beanPostProcessor() {
			return new POBPP() {
				String nameSuffix = "-processed-" + myProp;

				public void setNameSuffix(String nameSuffix) {
					this.nameSuffix = nameSuffix;
				}
				public Object postProcessBeforeInitialization(Object bean, String beanName) {
					if (bean instanceof ITestBean) {
						((ITestBean) bean).setName(((ITestBean) bean).getName() + nameSuffix);
					}
					return bean;
				}
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					return bean;
				}

				public int getOrder() {
					return 0;
				}
			};
		}

		//@Bean
		public BeanFactoryPostProcessor beanFactoryPostProcessor() {
			return new BeanFactoryPostProcessor() {
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
					BeanDefinition bd = beanFactory.getBeanDefinition("beanPostProcessor");
					bd.getPropertyValues().addPropertyValue("nameSuffix", "-processed-" + myProp);
				}
			};
		}

		@Bean
		public ITestBean listenerTestBean() {
			return new SpousyTestBean("listener");
		}
	}


	public interface POBPP extends BeanPostProcessor {
	}


	private static class SpousyTestBean extends TestBean implements ApplicationListener<ContextRefreshedEvent> {

		public boolean refreshed = false;

		public SpousyTestBean(String name) {
			super(name);
		}

		@Override
		@Required
		public void setSpouse(ITestBean spouse) {
			super.setSpouse(spouse);
		}

		public void onApplicationEvent(ContextRefreshedEvent event) {
			this.refreshed = true;
		}
	}

}
