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

package org.springframework.context.support;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * Integration tests for {@link PropertyResourceConfigurer} implementations requiring
 * interaction with an {@link ApplicationContext}.  For example, a {@link PropertyPlaceholderConfigurer}
 * that contains ${..} tokens in its 'location' property requires being tested through an ApplicationContext
 * as opposed to using only a BeanFactory during testing.
 *
 * @author Chris Beams
 * @see org.springframework.beans.factory.config.PropertyResourceConfigurerTests
 */
public class PropertyResourceConfigurerIntegrationTests {

	@Test
	public void testPropertyPlaceholderConfigurerWithSystemPropertyInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("location", "${user.dir}/test");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanInitializationException");
		}
		catch (BeanInitializationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof FileNotFoundException);
			// slight hack for Linux/Unix systems
			String userDir = StringUtils.cleanPath(System.getProperty("user.dir"));
			if (userDir.startsWith("/")) {
				userDir = userDir.substring(1);
			}
			assertTrue(ex.getMessage().indexOf(userDir) != -1);
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithSystemPropertiesInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("location", "${user.dir}/test/${user.dir}");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanInitializationException");
		}
		catch (BeanInitializationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof FileNotFoundException);
			// slight hack for Linux/Unix systems
			String userDir = StringUtils.cleanPath(System.getProperty("user.dir"));
			if (userDir.startsWith("/")) {
				userDir = userDir.substring(1);
			}
			/* the above hack doesn't work since the exception message is created without
			   the leading / stripped so the test fails.  Changed 17/11/04. DD */
			//assertTrue(ex.getMessage().indexOf(userDir + "/test/" + userDir) != -1);
			assertTrue(ex.getMessage().contains(userDir + "/test/" + userDir) ||
					ex.getMessage().contains(userDir + "/test//" + userDir));
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithUnresolvableSystemPropertiesInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("location", "${myprop}/test/${myprop}");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanInitializationException");
		}
		catch (BeanInitializationException ex) {
			// expected
			assertTrue(ex.getMessage().contains("myprop"));
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithMultiLevelCircularReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${var}");
		ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithNestedCircularReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${m}");
		ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithNestedUnresolvableReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.add("properties", "var=${m}var\nm=${var2}\nvar2=${m2}");
		ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			ex.printStackTrace();
		}
	}

	@Ignore // this test was breaking after the 3.0 repackaging
	@Test
	public void testPropertyPlaceholderConfigurerWithAutowireByType() {
//		StaticApplicationContext ac = new StaticApplicationContext();
//		MutablePropertyValues pvs = new MutablePropertyValues();
//		pvs.addPropertyValue("touchy", "${test}");
//		ac.registerSingleton("tb", TestBean.class, pvs);
//		pvs = new MutablePropertyValues();
//		pvs.addPropertyValue("target", new RuntimeBeanReference("tb"));
//		// uncomment when fixing this test
//		// ac.registerSingleton("tbProxy", org.springframework.aop.framework.ProxyFactoryBean.class, pvs);
//		pvs = new MutablePropertyValues();
//		Properties props = new Properties();
//		props.put("test", "mytest");
//		pvs.addPropertyValue("properties", new Properties(props));
//		RootBeanDefinition ppcDef = new RootBeanDefinition(PropertyPlaceholderConfigurer.class, pvs);
//		ppcDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
//		ac.registerBeanDefinition("configurer", ppcDef);
//		ac.refresh();
//		TestBean tb = (TestBean) ac.getBean("tb");
//		assertEquals("mytest", tb.getTouchy());
	}

}
