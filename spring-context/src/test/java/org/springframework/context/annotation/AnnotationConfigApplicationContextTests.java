/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation6.ComponentForScanning;
import org.springframework.context.annotation6.ConfigForScanning;
import org.springframework.context.annotation6.Jsr330NamedForScanning;

import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.util.StringUtils.*;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class AnnotationConfigApplicationContextTests {

	@Test(expected=IllegalArgumentException.class)
	public void nullGetBeanParameterIsDisallowed() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		context.getBean((Class<?>)null);
	}

	@Test
	public void scanAndRefresh() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.scan("org.springframework.context.annotation6");
		context.refresh();
		context.getBean(uncapitalize(ConfigForScanning.class.getSimpleName()));
		context.getBean("testBean"); // contributed by ConfigForScanning
		context.getBean(uncapitalize(ComponentForScanning.class.getSimpleName()));
		context.getBean(uncapitalize(Jsr330NamedForScanning.class.getSimpleName()));
		Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
		assertEquals(1, beans.size());
	}

	@Test
	public void registerAndRefresh() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class, NameConfig.class);
		context.refresh();
		context.getBean("testBean");
		context.getBean("name");
		Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
		assertEquals(2, beans.size());
	}

	@Test
	public void getBeansWithAnnotation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class, NameConfig.class, UntypedFactoryBean.class);
		context.refresh();
		context.getBean("testBean");
		context.getBean("name");
		Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
		assertEquals(2, beans.size());
	}

	@Test
	public void getBeanByType() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		TestBean testBean = context.getBean(TestBean.class);
		assertNotNull("getBean() should not return null", testBean);
		assertThat(testBean.name, equalTo("foo"));
	}

	/**
	 * Tests that Configuration classes are registered according to convention
	 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator#generateBeanName
	 */
	@Test
	public void defaultConfigClassBeanNameIsGeneratedProperly() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		// attempt to retrieve the instance by its generated bean name
		Config configObject = (Config) context.getBean("annotationConfigApplicationContextTests.Config");
		assertNotNull(configObject);
	}

	/**
	 * Tests that specifying @Configuration(value="foo") results in registering
	 * the configuration class with bean name 'foo'.
	 */
	@Test
	public void explicitConfigClassBeanNameIsRespected() {
		AnnotationConfigApplicationContext context =
			new AnnotationConfigApplicationContext(ConfigWithCustomName.class);

		// attempt to retrieve the instance by its specified name
		ConfigWithCustomName configObject =
			(ConfigWithCustomName) context.getBean("customConfigBeanName");
		assertNotNull(configObject);
	}

	@Test
	public void getBeanByTypeRaisesNoSuchBeanDefinitionException() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		// attempt to retrieve a bean that does not exist
		Class<?> targetType = Pattern.class;
		try {
			Object bean = context.getBean(targetType);
			fail("should have thrown NoSuchBeanDefinitionException, instead got: " + bean);
		}
		catch (NoSuchBeanDefinitionException ex) {
			assertThat(ex.getMessage(), containsString(
					format("No qualifying bean of type [%s] is defined", targetType.getName())));
		}
	}

	@Test
	public void getBeanByTypeAmbiguityRaisesException() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TwoTestBeanConfig.class);

		try {
			context.getBean(TestBean.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			assertThat(ex.getMessage(),
					allOf(
						containsString("No qualifying bean of type [" + TestBean.class.getName() + "] is defined"),
						containsString("tb1"),
						containsString("tb2")
					)
				);
		}
	}

	@Test
	public void autowiringIsEnabledByDefault() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AutowiredConfig.class);
		assertThat(context.getBean(TestBean.class).name, equalTo("foo"));
	}

	@Test
	public void nullReturningBeanPostProcessor() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(AutowiredConfig.class);
		context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return (bean instanceof TestBean ? null : bean);
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				return bean;
			}
		});
		context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				bean.getClass().getName();
				return bean;
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				bean.getClass().getName();
				return bean;
			}
		});
		context.refresh();
	}


	@Configuration
	static class Config {
		@Bean
		public TestBean testBean() {
			TestBean testBean = new TestBean();
			testBean.name = "foo";
			return testBean;
		}
	}

	@Configuration("customConfigBeanName")
	static class ConfigWithCustomName {
		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}

	static class ConfigMissingAnnotation {
		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}

	@Configuration
	static class TwoTestBeanConfig {
		@Bean TestBean tb1() { return new TestBean(); }
		@Bean TestBean tb2() { return new TestBean(); }
	}

	@Configuration
	static class NameConfig {
		@Bean String name() { return "foo"; }
	}

	@Configuration
	@Import(NameConfig.class)
	static class AutowiredConfig {
		@Autowired String autowiredName;

		@Bean TestBean testBean() {
			TestBean testBean = new TestBean();
			testBean.name = autowiredName;
			return testBean;
		}
	}

	static class UntypedFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return null;
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}
	}
}

class TestBean {
	String name;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestBean other = (TestBean) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
