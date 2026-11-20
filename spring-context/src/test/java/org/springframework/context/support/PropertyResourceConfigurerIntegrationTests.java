/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link PropertyResourceConfigurer} implementations requiring
 * interaction with an {@link ApplicationContext}.  For example, a
 * {@link org.springframework.beans.factory.config.PropertyPlaceholderConfigurer}
 * that contains ${..} tokens in its 'location' property requires being tested through an ApplicationContext
 * as opposed to using only a BeanFactory during testing.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @see org.springframework.beans.factory.config.PropertyResourceConfigurerTests
 */
@SuppressWarnings({"deprecation", "removal"})
class PropertyResourceConfigurerIntegrationTests {

	@Test
	void propertyPlaceholderConfigurerWithSystemPropertyInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("location", "${user.dir}/test");
		ac.registerSingleton("configurer", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs);
		String userDir = getUserDir();
		assertThatExceptionOfType(BeanInitializationException.class)
			.isThrownBy(ac::refresh)
			.withCauseInstanceOf(FileNotFoundException.class)
			.withMessageContaining(userDir);
		ac.close();
	}

	@Test
	void propertyPlaceholderConfigurerWithSystemPropertiesInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("location", "${user.dir}/test/${user.dir}");
		ac.registerSingleton("configurer", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs);
		String userDir = getUserDir();
		assertThatExceptionOfType(BeanInitializationException.class)
			.isThrownBy(ac::refresh)
			.withCauseInstanceOf(FileNotFoundException.class)
			.matches(ex -> ex.getMessage().contains(userDir + "/test/" + userDir) ||
					ex.getMessage().contains(userDir + "/test//" + userDir));
		ac.close();
	}

	private String getUserDir() {
		// slight hack for Linux/Unix systems
		String userDir = StringUtils.cleanPath(System.getProperty("user.dir"));
		if (userDir.startsWith("/")) {
			userDir = userDir.substring(1);
		}
		return userDir;
	}

	@Test
	void propertyPlaceholderConfigurerWithUnresolvableSystemPropertiesInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("location", "${myprop}/test/${myprop}");
		ac.registerSingleton("configurer", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs);
		assertThatExceptionOfType(BeanInitializationException.class)
			.isThrownBy(ac::refresh)
			.withMessageContaining("myprop");
		ac.close();
	}

	@Test
	void propertyPlaceholderConfigurerWithMultiLevelCircularReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${var}");
		ac.registerSingleton("configurer1", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(ac::refresh);
		ac.close();
	}

	@Test
	void propertyPlaceholderConfigurerWithNestedCircularReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${m}");
		ac.registerSingleton("configurer1", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(ac::refresh);
		ac.close();
	}

	@Test
	void propertyPlaceholderConfigurerWithNestedUnresolvableReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${m2}");
		ac.registerSingleton("configurer1", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class, pvs);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(ac::refresh);
		ac.close();
	}

	@Test
	void propertyPlaceholderConfigurerWithValueFromSystemProperty() {
		final String propertyName = getClass().getName() + ".test";

		try {
			System.setProperty(propertyName, "mytest");

			StaticApplicationContext context = new StaticApplicationContext();

			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.addPropertyValue("touchy", "${" + propertyName + "}");
			context.registerSingleton("tb", TestBean.class, pvs);

			pvs = new MutablePropertyValues();
			pvs.addPropertyValue("target", new RuntimeBeanReference("tb"));
			context.registerSingleton("tbProxy", org.springframework.aop.framework.ProxyFactoryBean.class, pvs);

			context.registerSingleton("configurer", org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class);
			context.refresh();

			TestBean testBean = context.getBean("tb", TestBean.class);
			assertThat(testBean.getTouchy()).isEqualTo("mytest");
			context.close();
		}
		finally {
			System.clearProperty(propertyName);
		}
	}

}
